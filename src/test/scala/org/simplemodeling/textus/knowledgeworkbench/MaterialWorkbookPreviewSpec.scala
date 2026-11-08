package org.simplemodeling.textus.knowledgeworkbench

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import scala.jdk.CollectionConverters.*
import scala.util.Using
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}
import org.apache.poi.ss.usermodel.FormulaError
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.bag.BinaryBag
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}

/*
 * @since   Nov. 8, 2026
 * @version Nov. 8, 2026
 */
final class MaterialWorkbookPreviewSpec extends AnyWordSpec with Matchers with GivenWhenThen with TableDrivenPropertyChecks {
  import MaterialWorkFixture.*

  "Authenticated workbook preview" should {
    "preserve real sheet order, coordinates, values and unevaluated formulas" in {
      Given("a real XLSX original with two sheets, sparse coordinates and a formula")
      val bytes = _workbook()
      withSession() { session =>
        val original = _original(bytes)
        session.producer.respondWith(Consequence.success(original))
        val before = bytes.toVector
        val candidates = _record(session.candidates())
        When("the caller previews the exact Material and resource selection")
        val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the output retains the selection and physical cell coordinates without evaluating the formula")
        val record = _record(result)
        record.getAny("materialRef") shouldBe Some(materialRef)
        record.getAny("resourceId") shouldBe Some(resourceId)
        val preview = workbenchvalue.MaterialWorkbookPreviewResult.createC(record).TAKE
        preview.sourceVersion shouldBe None
        val sheets = preview.sheets
        sheets.map(_.name) shouldBe Vector("First", "Second")
        val cells = sheets.head.cells
        cells.map(cell => (cell.row, cell.column)) shouldBe Vector((2, 3), (4, 1))
        cells.head.kind shouldBe "string"
        cells.head.rawValue shouldBe Some(" 東京 ")
        cells.head.displayText shouldBe Some(" 東京 ")
        cells.last.kind shouldBe "formula"
        cells.last.formula shouldBe Some("1+2")
        cells.last.rawValue shouldBe None
        cells.last.cachedKind shouldBe None
        cells.last.diagnostic shouldBe Some("formula-not-evaluated")
        val second = sheets(1).cells
        second.map(_.kind) shouldBe Vector("numeric", "boolean", "error", "blank")
        second.head.rawValue shouldBe Some("42.5")
        second.head.displayText shouldBe Some("42.5")
        second(1).rawValue shouldBe Some("1")
        second(2).diagnostic shouldBe Some("cell-error")
        second(3).rawValue shouldBe None
        session.producer.calls.map(_.request.operation) shouldBe Vector("readOriginal")
        session.producer.calls.head.request.properties.map(p => p.name -> p.value).toMap shouldBe Map("materialRef" -> materialRef, "resourceId" -> resourceId)
        bytes.toVector shouldBe before
        _record(session.candidates()) shouldBe candidates
        val reread = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId)).TAKE
        reread should be theSameInstanceAs original
        val bag = reread match {
          case OperationResponse.Http(response) => response.bag.asInstanceOf[BinaryBag]
          case other => fail(s"Expected binary response: $other")
        }
        Using.resource(bag.openInputStream())(_.readAllBytes().toVector) shouldBe before
      }
    }

    "propagate denial and reject unsupported, malformed and oversized originals" in {
      Given("the public route and several invalid or denied original responses")
      withSession() { session =>
        val denied = Consequence.securityPermissionDenied[OperationResponse]("denied")
        val anonymous = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId), sessionId = None)
        anonymous shouldBe a[Consequence.Failure[?]]
        session.producer.calls shouldBe empty
        session.producer.respondWith(denied)
        When("the producer denies the selected original")
        val refusal = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the exact denial propagates")
        refusal shouldBe denied
        val examples = Table(("bytes", "expected"),
          (Array[Byte](1, 2, 3), "unsupported workbook format"),
          (new Array[Byte](8 * 1024 * 1024 + 1), "byte limit exceeded"))
        forAll(examples) { (bytes, expected) =>
          Given("unsupported, malformed or oversized input")
          session.producer.respondWith(Consequence.success(_original(bytes)))
          When("the same route previews it")
          val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
          Then("it fails rather than returning a truncated preview")
          _message(result) should include (expected)
        }
        session.producer.respondWith(Consequence.success(_original(Array[Byte](80, 75, 3, 4, 1))))
        val malformed = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        _message(malformed) should not include "unsupported workbook format"
      }
    }

    "reject external relationships without fetching them" in {
      Given("a real XLSX with an external hyperlink")
      val bytes = _workbook(external = true)
      withSession() { session =>
        session.producer.respondWith(Consequence.success(_original(bytes)))
        When("the caller previews the original")
        val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the relationship is rejected and no extra producer operation occurs")
        result shouldBe a[Consequence.Failure[?]]
        session.producer.calls.map(_.request.operation) shouldBe Vector("readOriginal")
      }
    }

    "preserve arbitrary exact resource selections through the caller route" in {
      Given("generated Unicode text, coordinates and opaque selector suffixes in real XLSX originals")
      val generator = for {
        suffix <- Gen.listOfN(12, Gen.alphaNumChar).map(_.mkString)
        row <- Gen.choose(0, 2)
        column <- Gen.choose(0, 6)
      } yield (suffix, row, column)
      withSession() { session =>
        val property = Prop.forAll(generator) { case (suffix, row, column) =>
          val reference = "opaque:material/" + suffix
          val resource = "opaque:resource/" + suffix
          val content = s" 日本語 $suffix "
          session.producer.respondWith(Consequence.success(_original(_workbook(text = content, row = row, column = column))))
          When("an authenticated caller selects generated opaque identities")
          val result = session.execute("previewWorkbook", Vector("materialRef" -> reference, "resourceId" -> resource))
          Then("the returned identities and producer request match exactly")
          val record = _record(result)
          val cell = workbenchvalue.MaterialWorkbookPreviewResult.createC(record).TAKE.sheets.head.cells.head
          (cell.row, cell.column, cell.rawValue, cell.displayText) shouldBe ((row + 1, column + 1, Some(content), Some(content)))
          record.getAny("materialRef") shouldBe Some(reference)
          record.getAny("resourceId") shouldBe Some(resource)
          session.producer.calls.last.request.properties.map(p => p.name -> p.value).toMap shouldBe
            Map("materialRef" -> reference, "resourceId" -> resource)
          true
        }
        Test.check(Test.Parameters.default.withMinSuccessfulTests(100), property).passed shouldBe true
      }
    }

    "reject a workbook with too many sheets" in {
      Given("a real workbook containing sixty-five sheets")
      val workbook = new XSSFWorkbook()
      val bytes = try {
        (1 to 65).foreach(index => workbook.createSheet(s"Sheet$index"))
        val out = new ByteArrayOutputStream()
        workbook.write(out)
        out.toByteArray
      } finally workbook.close()
      withSession() { session =>
        session.producer.respondWith(Consequence.success(_original(bytes)))
        When("the caller requests a preview")
        val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the entire preview fails at the sheet bound")
        _message(result) should include ("workbook structure limit exceeded")
      }
    }

    "reject the specific package and XML limits before loading a workbook" in {
      Given("mutations retaining a real XLSX's workbook and content type parts")
      val original = _entries(_workbook())
      val content = original.find(_._1 == "[Content_Types].xml").get._2
      val xml = new String(content, StandardCharsets.UTF_8)
      val insertion = xml.indexOf("?>") + 2
      val dtd = (xml.substring(0, insertion) + "<!DOCTYPE Types [<!ENTITY leak SYSTEM 'file:///never-read'>]>" + xml.substring(insertion)).getBytes(StandardCharsets.UTF_8)
      val deep = (("<x>" * 65) + ("</x>" * 65)).getBytes(StandardCharsets.UTF_8)
      val examples = Vector(
        _zip(original :+ ("oversized.bin" -> new Array[Byte](8 * 1024 * 1024 + 1))) -> "ZIP entry expanded byte limit exceeded",
        _zip(original ++ Vector.tabulate(5)(index => s"large$index.bin" -> new Array[Byte](7 * 1024 * 1024))) -> "ZIP aggregate expanded byte limit exceeded",
        _zip(original :+ original.head) -> "duplicate ZIP entry",
        _zip(original ++ Vector.tabulate(257)(index => s"part$index.bin" -> Array[Byte](1))) -> "ZIP entry count limit exceeded",
        _zip(original.map { case (name, data) => name -> (if (name == "[Content_Types].xml") deep else data) }) -> "XML structure limit exceeded",
        _zip(original.map { case (name, data) => name -> (if (name == "[Content_Types].xml") dtd else data) }) -> "DOCTYPE",
        _workbook(text = "x" * 10001) -> "cell XML text limit exceeded"
      )
      withSession() { session =>
        for ((bytes, expected) <- examples) {
          session.producer.respondWith(Consequence.success(_original(bytes)))
          When("the caller requests a preview")
          val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
          Then("the named guard rejects the complete preview")
          _message(result) should include (expected)
        }
      }
    }

    "reject alternate XML part names before workbook loading" in {
      Given("real XLSX packages whose worksheet, shared strings or styles part is renamed with its OPC references")
      val original = _entries(_workbook())
      val names = Vector("xl/worksheets/sheet1.xml", "xl/sharedStrings.xml", "xl/styles.xml")
      withSession() { session =>
        val candidates = _record(session.candidates())
        names.foreach { name =>
          val alternate = name.stripSuffix(".xml") + ".bin"
          val basename = name.split("/").last
          val renamed = original.map { case (entry, data) =>
            val updated = if (entry.endsWith(".xml") || entry.endsWith(".rels")) {
              new String(data, StandardCharsets.UTF_8).replace(name, alternate)
                .replace(basename, basename.stripSuffix(".xml") + ".bin").getBytes(StandardCharsets.UTF_8)
            } else data
            (if (entry == name) alternate else entry) -> updated
          }
          val bytes = _zip(renamed)
          val before = bytes.toVector
          session.producer.respondWith(Consequence.success(_original(bytes)))
          When("the caller previews the selected original")
          val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
          Then("the named unsupported part is rejected without modifying the original or Candidate work")
          _message(result) should include (s"unsupported workbook package part: $alternate")
          bytes.toVector shouldBe before
          _record(session.candidates()) shouldBe candidates
        }
      }
    }

    "admit a standard inert PNG and reject XML disguised as an image" in {
      Given("a real XLSX with a standard PNG package part")
      val original = _entries(_workbook())
      val png = java.util.Base64.getDecoder.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
      val entries = original.map { case (name, data) =>
        if (name == "[Content_Types].xml") {
          val xml = new String(data, StandardCharsets.UTF_8).replace("</Types>", "<Default Extension=\"png\" ContentType=\"image/png\"/></Types>")
          name -> xml.getBytes(StandardCharsets.UTF_8)
        } else name -> data
      }
      withSession() { session =>
        session.producer.respondWith(Consequence.success(_original(_zip(entries :+ ("xl/media/image1.png" -> png)))))
        When("the caller previews the package")
        val accepted = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the workbook remains readable")
        workbenchvalue.MaterialWorkbookPreviewResult.createC(_record(accepted)).TAKE.sheets.map(_.name) shouldBe Vector("First", "Second")
        Given("XML content masquerading as the image part")
        val disguised = "<worksheet/>".getBytes(StandardCharsets.UTF_8)
        session.producer.respondWith(Consequence.success(_original(_zip(entries :+ ("xl/media/image1.png" -> disguised)))))
        When("the caller previews it")
        val rejected = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the part is rejected before model loading")
        _message(rejected) should include ("unsupported workbook package part: xl/media/image1.png")
      }
    }

    "admit only associated opaque printer settings with a valid header" in {
      Given("a real XLSX package with an internally referenced printer settings part")
      val original = _entries(_workbook())
      val path = "xl/printerSettings/printerSettings1.bin"
      val ctype = "application/vnd.openxmlformats-officedocument.spreadsheetml.printerSettings"
      val reltype = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/printerSettings"
      val payload = new Array[Byte](220)
      payload(68) = 220.toByte
      def _package_(data: Array[Byte], contenttype: String = ctype, relation: String = reltype,
                    source: String = "xl/worksheets/_rels/sheet1.xml.rels"): Array[Byte] = {
        val updated = original.map { case (name, bytes) =>
          if (name == "[Content_Types].xml") name -> new String(bytes, StandardCharsets.UTF_8)
            .replace("</Types>", s"<Default Extension=\"bin\" ContentType=\"$contenttype\"/></Types>").getBytes(StandardCharsets.UTF_8)
          else name -> bytes
        }
        val rel = s"<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$relation\" Target=\"../printerSettings/printerSettings1.bin\"/></Relationships>"
        _zip(updated :+ (path -> data) :+ (source -> rel.getBytes(StandardCharsets.UTF_8)))
      }
      withSession() { session =>
        When("the public route previews the valid package")
        session.producer.respondWith(Consequence.success(_original(_package_(payload))))
        workbenchvalue.MaterialWorkbookPreviewResult.createC(_record(session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId)))).TAKE.sheets should not be empty
        Then("invalid headers, content types and relationships are rejected")
        val short = new Array[Byte](219)
        val mismatch = payload.clone()
        mismatch(70) = 1
        val disguised = payload.clone()
        Array.copy("<worksheet/>".getBytes(StandardCharsets.UTF_8), 0, disguised, 0, 12)
        val utf16 = Vector(StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE).map { encoding =>
          val data = payload.clone()
          val bom = if (encoding == StandardCharsets.UTF_16LE) Array[Byte](0xff.toByte, 0xfe.toByte) else Array[Byte](0xfe.toByte, 0xff.toByte)
          Array.copy(bom, 0, data, 0, bom.length)
          val xml = "<worksheet/>".getBytes(encoding)
          Array.copy(xml, 0, data, bom.length, xml.length)
          _package_(data)
        }
        utf16.foreach { bytes =>
          session.producer.respondWith(Consequence.success(_original(bytes)))
          _message(session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))) should include ("invalid printer settings payload")
        }
        val cases = Vector(_package_(short), _package_(mismatch), _package_(disguised),
          _package_(payload, contenttype = "application/xml"),
          _package_(payload, relation = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"),
          _package_(payload, source = "xl/_rels/workbook.xml.rels"))
        val renamed = Vector("xl/worksheets/sheet1.xml", "xl/sharedStrings.xml", "xl/styles.xml").map { name =>
          val data = original.find(_._1 == name).get._2
          val disguisedpart = java.util.Arrays.copyOf(data, math.max(220, data.length))
          disguisedpart(68) = 220.toByte
          disguisedpart(69) = 0
          val extra = disguisedpart.length - 220
          disguisedpart(70) = (extra & 0xff).toByte
          disguisedpart(71) = ((extra >> 8) & 0xff).toByte
          val changed = original.filterNot(_._1 == name).map { case (entry, bytes) =>
            if (entry == "[Content_Types].xml") {
              val xml = new String(bytes, StandardCharsets.UTF_8)
                .replace("</Types>", s"<Default Extension=\"bin\" ContentType=\"$ctype\"/></Types>")
              entry -> xml.getBytes(StandardCharsets.UTF_8)
            } else if (entry == "xl/_rels/workbook.xml.rels") {
              val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
              factory.setNamespaceAware(true)
              val document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes))
              val relations = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/package/2006/relationships", "Relationship")
              val matching = (0 until relations.getLength).map(index => relations.item(index).asInstanceOf[org.w3c.dom.Element]).filter { relation =>
                new java.net.URI("/xl/workbook.xml").resolve(new java.net.URI(relation.getAttribute("Target"))).normalize().getPath == "/" + name
              }
              matching.size shouldBe 1
              matching.head.setAttribute("Target", "/xl/printerSettings/printerSettings1.bin")
              val out = new ByteArrayOutputStream()
              javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(
                new javax.xml.transform.dom.DOMSource(document), new javax.xml.transform.stream.StreamResult(out))
              entry -> out.toByteArray
            } else entry -> bytes
          }
          val relation = s"<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$reltype\" Target=\"../printerSettings/printerSettings1.bin\"/></Relationships>"
          _zip(changed :+ (path -> disguisedpart) :+ ("xl/worksheets/_rels/sheet2.xml.rels" -> relation.getBytes(StandardCharsets.UTF_8)))
        }
        renamed.foreach { bytes =>
          session.producer.respondWith(Consequence.success(_original(bytes)))
          _message(session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))) should include ("invalid printer settings relationship")
        }
        val valid = _entries(_package_(payload))
        val orphan = _zip(valid.filterNot(_._1 == "xl/worksheets/_rels/sheet1.xml.rels"))
        val outgoing = _zip(valid :+ ("xl/printerSettings/_rels/printerSettings1.bin.rels" ->
          "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>".getBytes(StandardCharsets.UTF_8)))
        val duplicate = _zip(valid.map { case (name, data) =>
          if (name == "[Content_Types].xml") name -> new String(data, StandardCharsets.UTF_8)
            .replace("</Types>", s"<Default Extension=\"bin\" ContentType=\"$ctype\"/></Types>").getBytes(StandardCharsets.UTF_8)
          else name -> data
        })
        (cases ++ Vector(orphan, outgoing, duplicate)).foreach { bytes =>
          session.producer.respondWith(Consequence.success(_original(bytes)))
          session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId)) shouldBe a[Consequence.Failure[?]]
        }
      }
    }

    "accept a value at the documented cell text boundary" in {
      Given("a real workbook containing exactly ten thousand characters in a cell")
      val text = "x" * 10000
      withSession() { session =>
        session.producer.respondWith(Consequence.success(_original(_workbook(text = text))))
        When("the caller requests the workbook preview")
        val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
        Then("the complete value survives")
        workbenchvalue.MaterialWorkbookPreviewResult.createC(_record(result)).TAKE.sheets.head.cells.head.rawValue shouldBe Some(text)
      }
    }
  }

  private def _original(bytes: Array[Byte]): OperationResponse = OperationResponse.Http(HttpResponse.Binary(
    HttpStatus.Ok, ContentType(MimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), None),
    Bag.binary(bytes), Record.empty))

  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected record response: $other")
  }

  private def _message(result: Consequence[OperationResponse]): String = result match {
    case Consequence.Failure(conclusion) => conclusion.displayMessage
    case other => fail(s"Expected structured preview failure: $other")
  }

  private def _workbook(external: Boolean = false, text: String = " 東京 ", row: Int = 1, column: Int = 2): Array[Byte] = {
    val workbook = new XSSFWorkbook()
    try {
      val first = workbook.createSheet("First")
      val cell = first.createRow(row).createCell(column)
      cell.setCellValue(text)
      val formula = first.createRow(3).createCell(0)
      formula.setCellFormula("1+2")
      if (formula.getCTCell.isSetV) formula.getCTCell.unsetV()
      val second = workbook.createSheet("Second").createRow(0)
      second.createCell(1).setCellValue(42.5)
      second.createCell(2).setCellValue(true)
      second.createCell(3).setCellErrorValue(FormulaError.DIV0.getCode)
      second.createCell(4, org.apache.poi.ss.usermodel.CellType.BLANK)
      if (external) {
        val link = workbook.getCreationHelper.createHyperlink(org.apache.poi.common.usermodel.HyperlinkType.URL)
        link.setAddress("https://example.invalid/never-fetch")
        cell.setHyperlink(link)
      }
      val out = new ByteArrayOutputStream()
      workbook.write(out)
      out.toByteArray
    } finally workbook.close()
  }

  private def _zip(entries: Vector[(String, Array[Byte])]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val zip = new ZipArchiveOutputStream(out)
    try entries.foreach { case (name, data) =>
      zip.putArchiveEntry(new ZipArchiveEntry(name))
      zip.write(data)
      zip.closeArchiveEntry()
    } finally zip.close()
    out.toByteArray
  }

  private def _entries(bytes: Array[Byte]): Vector[(String, Array[Byte])] = {
    val zip = new ZipInputStream(new ByteArrayInputStream(bytes))
    val entries = Vector.newBuilder[(String, Array[Byte])]
    try {
      var entry = zip.getNextEntry
      while (entry != null) {
        entries += entry.getName -> zip.readAllBytes()
        zip.closeEntry()
        entry = zip.getNextEntry
      }
      entries.result()
    } finally zip.close()
  }
}

@org.scalatest.tags.Slow
final class MaterialWorkbookOriginalFileSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialWorkFixture.*

  "An explicitly supplied original workbook" should {
    "preview through MaterialWork without changing its bytes" in {
      Given("the private original supplied through the explicit test property")
      val path = java.nio.file.Paths.get(sys.props.getOrElse("textus.test.workbook.original", fail("Missing textus.test.workbook.original property")))
      val bytes = java.nio.file.Files.readAllBytes(path)
      val digest = java.security.MessageDigest.getInstance("SHA-256")
      val before = digest.digest(bytes).toVector
      val original = OperationResponse.Http(HttpResponse.Binary(HttpStatus.Ok,
        ContentType(MimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), None),
        Bag.binary(bytes), Record.empty))
      withSession() { session =>
        session.producer.respondWith(Consequence.success(original))
        When("the authenticated public preview route reads it")
        val result = session.execute("previewWorkbook", Vector("materialRef" -> materialRef, "resourceId" -> resourceId)).TAKE
        Then("a nonempty generic preview is returned and the original is unchanged")
        val record = result match {
          case OperationResponse.RecordResponse(value) => value
          case _ => fail("Expected workbook preview record")
        }
        workbenchvalue.MaterialWorkbookPreviewResult.createC(record).TAKE.sheets should not be empty
        digest.digest(java.nio.file.Files.readAllBytes(path)).toVector shouldBe before
        digest.digest(bytes).toVector shouldBe before
      }
    }
  }
}
