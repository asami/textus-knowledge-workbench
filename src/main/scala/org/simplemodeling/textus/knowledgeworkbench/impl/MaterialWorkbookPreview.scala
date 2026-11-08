package org.simplemodeling.textus.knowledgeworkbench.impl

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.Locale
import java.net.URI
import scala.collection.mutable
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource
import org.apache.poi.ss.usermodel.{CellType, DataFormatter}
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFWorkbook}
import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.http.HttpResponse
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.value
import org.xml.sax.{Attributes, InputSource, SAXException}
import org.xml.sax.helpers.DefaultHandler

/*
 * @since   Nov. 8, 2026
 * @version Nov. 8, 2026
 */
/** Bounded, read-only XLSX interpretation of a caller-authorized original. */
private[knowledgeworkbench] object MaterialWorkbookPreview {
  private val _max_bytes = 8 * 1024 * 1024
  private val _max_entry = 8 * 1024 * 1024
  private val _max_total = 32 * 1024 * 1024
  private val _max_entries = 256
  private val _max_elements = 100000
  private val _max_cells = 20000
  private val _max_text = 1024 * 1024
  private val _max_cell_text = 10000

  def preview(response: OperationResponse, reference: String, resource: String): Consequence[OperationResponse] =
    try {
      val stream = response match {
        case OperationResponse.Http(binary: HttpResponse.Binary) => binary.bag.asInstanceOf[BinaryBag].openInputStream()
        case _ => throw new IllegalArgumentException("Original response is not binary")
      }
      val bytes = try _read_bounded(stream, _max_bytes) finally stream.close()
      val result = _decode(bytes, reference, resource)
      Consequence.success(OperationResponse(result))
    } catch {
      case NonFatal(e) => Consequence.resourceInvalid(s"Workbook preview rejected: ${Option(e.getMessage).getOrElse(e.getClass.getSimpleName)}")
    }

  private def _read_bounded(stream: InputStream, limit: Int): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val buffer = new Array[Byte](8192)
    var total = 0
    var count = stream.read(buffer, 0, math.min(buffer.length, limit - total + 1))
    while (count != -1) {
      total += count
      if (total > limit) throw new IllegalArgumentException("byte limit exceeded")
      out.write(buffer, 0, count)
      count = stream.read(buffer, 0, math.min(buffer.length, limit - total + 1))
    }
    out.toByteArray
  }

  private def _decode(bytes: Array[Byte], reference: String, resource: String): Record = {
    if (bytes.length < 4 || bytes(0) != 'P'.toByte || bytes(1) != 'K'.toByte)
      throw new IllegalArgumentException("unsupported workbook format; only unencrypted XLSX is supported")
    val zip = ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(bytes)).get()
    var owned = true
    try {
      val iterator = zip.getEntries.asScala
      val entries = Vector.newBuilder[org.apache.commons.compress.archivers.zip.ZipArchiveEntry]
      var entrycount = 0
      while (iterator.hasNext) {
        entrycount += 1
        if (entrycount > _max_entries) throw new IllegalArgumentException("ZIP entry count limit exceeded")
        entries += iterator.next()
      }
      val collected = entries.result()
      if (collected.isEmpty) throw new IllegalArgumentException("empty ZIP package")
      if (collected.map(_.getName).distinct.size != collected.size) throw new IllegalArgumentException("duplicate ZIP entry")
      var total = 0L
      collected.foreach { entry =>
        if (!entry.isDirectory) {
          val stream = zip.getInputStream(entry)
          val remaining = (_max_total - total).toInt
          val limit = math.min(_max_entry, remaining)
          val data = try _read_bounded(stream, limit) catch {
            case e: IllegalArgumentException if remaining < _max_entry =>
              throw new IllegalArgumentException("ZIP aggregate expanded byte limit exceeded", e)
            case e: IllegalArgumentException =>
              throw new IllegalArgumentException("ZIP entry expanded byte limit exceeded", e)
          } finally stream.close()
          total += data.length
          if (total > _max_total) throw new IllegalArgumentException("ZIP expanded byte limit exceeded")
        }
      }
      val budget = new XmlBudget
      val metadata = new PackageMetadata
      val parts = mutable.Map.empty[String, Array[Byte]]
      collected.foreach { entry =>
        if (!entry.isDirectory) {
          val stream = zip.getInputStream(entry)
          val data = try _read_bounded(stream, _max_entry) finally stream.close()
          val name = entry.getName.toLowerCase(Locale.ROOT)
          parts += entry.getName -> data
          if (name.endsWith("vbaproject.bin") || name.startsWith("xl/externallinks/"))
            throw new IllegalArgumentException("macros and external workbook links are unsupported")
          if (name.endsWith(".xml") || name.endsWith(".rels")) _check_xml(data, budget, entry.getName, metadata)
          else if (!_inert_image(name, data) && !_printer_path(entry.getName))
            throw new IllegalArgumentException(s"unsupported workbook package part: ${entry.getName}")
        }
      }
      _check_package(parts.toMap, metadata)
      if (!collected.exists(_.getName == "[Content_Types].xml") || !collected.exists(_.getName == "xl/workbook.xml"))
        throw new IllegalArgumentException("unsupported or malformed XLSX package")
      val pkg = OPCPackage.open(new ZipFileZipEntrySource(zip))
      owned = false
      try {
        val workbook = new XSSFWorkbook(pkg)
        try {
        if (workbook.isMacroEnabled) throw new IllegalArgumentException("macro-enabled workbooks are unsupported")
        if (workbook.getNumberOfSheets > 64) throw new IllegalArgumentException("sheet limit exceeded")
        val formatter = new DataFormatter(Locale.ROOT)
        var cellcount = 0
        var textcount = 0L
        val sheets = (0 until workbook.getNumberOfSheets).map { index =>
          val sheet = workbook.getSheetAt(index)
          val cells = sheet.iterator().asScala.toVector.flatMap { row =>
            row.cellIterator().asScala.map { cell =>
              cellcount += 1
              if (cellcount > _max_cells) throw new IllegalArgumentException("cell limit exceeded")
              val xcell = cell.asInstanceOf[XSSFCell]
              val kind = cell.getCellType
              val raw = Option(xcell.getRawValue)
              val formula = if (kind == CellType.FORMULA) Some(cell.getCellFormula) else None
              val cachedkind = if (kind == CellType.FORMULA && raw.isDefined)
                Some(cell.getCachedFormulaResultType.toString.toLowerCase(Locale.ROOT)) else None
              val display = if (kind == CellType.FORMULA || kind == CellType.BLANK) None else Some(formatter.formatCellValue(cell))
              val actualraw = if (kind == CellType.STRING) Some(cell.getStringCellValue) else raw
              val diagnostic = if (kind == CellType.FORMULA) Some("formula-not-evaluated") else if (kind == CellType.ERROR) Some("cell-error") else None
              (actualraw.toVector ++ display.toVector ++ formula.toVector).foreach { value =>
                if (value.length > _max_cell_text) throw new IllegalArgumentException("cell text limit exceeded")
                textcount += value.length
                if (textcount > _max_text) throw new IllegalArgumentException("output text limit exceeded")
              }
              value.MaterialWorkbookCell(cell.getRowIndex + 1, cell.getColumnIndex + 1,
                kind.toString.toLowerCase(Locale.ROOT), actualraw, display, formula, cachedkind, diagnostic)
            }.toVector
          }
          value.MaterialWorkbookSheet(index + 1, sheet.getSheetName, cells)
        }.toVector
        value.MaterialWorkbookPreviewResult(reference, resource, None, sheets, Vector.empty).toRecord()
        } finally workbook.close()
      } finally if (!pkg.isClosed) pkg.revert()
    } finally if (owned) zip.close()
  }

  private def _inert_image(name: String, bytes: Array[Byte]): Boolean = {
    val location = name.matches("xl/media/[^/]+\\.(png|jpg|jpeg)") ||
      name.matches("docprops/thumbnail\\.(png|jpg|jpeg)")
    val png = Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    val jpeg = Array[Byte](0xff.toByte, 0xd8.toByte, 0xff.toByte)
    location && ((name.endsWith(".png") && bytes.startsWith(png)) ||
      ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && bytes.startsWith(jpeg)))
  }

  private val _printer_type = "application/vnd.openxmlformats-officedocument.spreadsheetml.printerSettings"
  private val _printer_rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/printerSettings"

  private def _printer_path(name: String): Boolean =
    name.matches("xl/printerSettings/printerSettings[1-9][0-9]*\\.bin")

  private final class PackageMetadata {
    val defaults = mutable.Map.empty[String, String]
    val overrides = mutable.Map.empty[String, String]
    val relationships = mutable.ArrayBuffer.empty[(String, String, String, String)]
  }

  private def _check_package(parts: Map[String, Array[Byte]], metadata: PackageMetadata): Unit = {
    val printers = parts.keys.filter(_printer_path).toSet
    def _content_type_(name: String): Option[String] = metadata.overrides.get("/" + name).orElse {
      name.lastIndexOf('.') match {
        case -1 => None
        case index => metadata.defaults.get(name.substring(index + 1).toLowerCase(Locale.ROOT))
      }
    }
    parts.keys.foreach { name =>
      if (name.toLowerCase(Locale.ROOT).endsWith(".png") && !_content_type_(name).contains("image/png") ||
          (name.toLowerCase(Locale.ROOT).endsWith(".jpg") || name.toLowerCase(Locale.ROOT).endsWith(".jpeg")) && !_content_type_(name).contains("image/jpeg"))
        throw new IllegalArgumentException("invalid image content type")
      if (_content_type_(name).contains(_printer_type) != printers.contains(name))
        throw new IllegalArgumentException("invalid printer settings content type or path")
    }
    metadata.overrides.foreach { case (name, contenttype) =>
      if (contenttype == _printer_type && !printers.contains(name.stripPrefix("/")))
        throw new IllegalArgumentException("orphan printer settings content type")
    }
    if (metadata.defaults.get("bin").contains(_printer_type) && printers.isEmpty)
      throw new IllegalArgumentException("orphan printer settings content type")
    if (printers.exists(name => parts.contains(name.substring(0, name.lastIndexOf('/') + 1) + "_rels/" + name.split("/").last + ".rels")))
      throw new IllegalArgumentException("printer settings outgoing relationships")
    val incoming = mutable.Set.empty[String]
    metadata.relationships.foreach { case (source, target, kind, mode) =>
      val base = new URI("/" + source)
      val resolved = base.resolve(new URI(target)).normalize()
      if (resolved.isAbsolute || resolved.getRawQuery != null || resolved.getRawFragment != null ||
          resolved.getRawPath == null || !resolved.getRawPath.startsWith("/") || resolved.getRawPath.contains("%"))
        throw new IllegalArgumentException("invalid relationship target")
      val path = resolved.getRawPath.stripPrefix("/")
      if (_printer_path(source)) throw new IllegalArgumentException("printer settings outgoing relationship")
      if (parts.get(path).exists(data => _inert_image(path.toLowerCase(Locale.ROOT), data)) &&
          kind != "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" &&
          kind != "http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail")
        throw new IllegalArgumentException("invalid image relationship")
      if (printers.contains(path) || kind == _printer_rel || _printer_path(path)) {
        if (!printers.contains(path) || kind != _printer_rel || (mode.nonEmpty && mode != "Internal") ||
            !source.matches("xl/worksheets/sheet[1-9][0-9]*\\.xml") || !parts.contains(source))
          throw new IllegalArgumentException("invalid printer settings relationship")
        incoming += path
      }
    }
    if (incoming.toSet != printers) throw new IllegalArgumentException("orphan printer settings part")
    printers.foreach { name =>
      val bytes = parts(name)
      def _unsigned_short_(offset: Int): Int = (bytes(offset) & 0xff) | ((bytes(offset + 1) & 0xff) << 8)
      if (bytes.length < 220) throw new IllegalArgumentException("invalid printer settings payload")
      val size = _unsigned_short_(68)
      val extra = _unsigned_short_(70)
      val prefix = bytes.take(128).filterNot(b => b == 0 || b == 0x20 || b == 0x09 || b == 0x0a || b == 0x0d)
      if (size < 220 || size > bytes.length || size + extra != bytes.length ||
          prefix.headOption.contains('<'.toByte) || prefix.startsWith(Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte, '<'.toByte)) ||
          bytes.startsWith(Array[Byte](0xff.toByte, 0xfe.toByte)) || bytes.startsWith(Array[Byte](0xfe.toByte, 0xff.toByte)))
        throw new IllegalArgumentException("invalid printer settings payload")
    }
  }

  private final class XmlBudget {
    var elements = 0
    var attributes = 0
    var text = 0L
    var cells = 0
    var sheets = 0
    var strings = 0
    var styles = 0
  }

  private def _check_xml(bytes: Array[Byte], budget: XmlBudget, name: String, metadata: PackageMetadata): Unit = {
    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    val parser = factory.newSAXParser()
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    val handler = new DefaultHandler {
      private var _depth = 0
      private var _value_depth = 0
      private var _value_text = 0
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit = {
        _depth += 1
        if (name == "[Content_Types].xml" && uri == "http://schemas.openxmlformats.org/package/2006/content-types") {
          if (localName == "Default") {
            val extension = Option(attributes.getValue("Extension")).getOrElse(throw new SAXException("missing extension")).toLowerCase(Locale.ROOT)
            val contenttype = Option(attributes.getValue("ContentType")).getOrElse(throw new SAXException("missing content type"))
            if (metadata.defaults.contains(extension)) throw new SAXException("duplicate content type default")
            metadata.defaults += extension -> contenttype
          } else if (localName == "Override") {
            val part = Option(attributes.getValue("PartName")).getOrElse(throw new SAXException("missing part name"))
            val contenttype = Option(attributes.getValue("ContentType")).getOrElse(throw new SAXException("missing content type"))
            if (metadata.overrides.contains(part)) throw new SAXException("duplicate content type override")
            metadata.overrides += part -> contenttype
          }
        }
        if (name.endsWith(".rels") && uri == "http://schemas.openxmlformats.org/package/2006/relationships" && localName == "Relationship") {
          val source = if (name == "_rels/.rels") "" else {
            val marker = "/_rels/"
            val index = name.lastIndexOf(marker)
            if (index < 0) throw new SAXException("invalid relationships path")
            name.substring(0, index + 1) + name.substring(index + marker.length).stripSuffix(".rels")
          }
          metadata.relationships += ((source,
            Option(attributes.getValue("Target")).getOrElse(throw new SAXException("missing relationship target")),
            Option(attributes.getValue("Type")).getOrElse(throw new SAXException("missing relationship type")),
            Option(attributes.getValue("TargetMode")).getOrElse("")))
        }
        if (_value_depth == 0 && (localName == "c" || localName == "si")) {
          _value_depth = _depth
          _value_text = 0
        }
        budget.elements += 1
        budget.attributes += attributes.getLength
        if (_depth > 64 || budget.elements > _max_elements || budget.attributes > 500000 || attributes.getLength > 128)
          throw new SAXException("XML structure limit exceeded")
        (0 until attributes.getLength).foreach { index =>
          val value = attributes.getValue(index)
          if (value.length > _max_cell_text) throw new SAXException("XML attribute limit exceeded")
          if (attributes.getLocalName(index) == "TargetMode" && value.equalsIgnoreCase("External"))
            throw new SAXException("external relationships are unsupported")
          if (value.toLowerCase(Locale.ROOT).contains("macroenabled")) throw new SAXException("macro-enabled workbook is unsupported")
        }
        localName match {
          case "c" => budget.cells += 1
          case "sheet" => budget.sheets += 1
          case "si" => budget.strings += 1
          case "xf" => budget.styles += 1
          case _ => ()
        }
        if (budget.cells > _max_cells || budget.sheets > 64 || budget.strings > _max_cells || budget.styles > _max_cells)
          throw new SAXException("workbook structure limit exceeded")
      }
      override def endElement(uri: String, localName: String, qName: String): Unit = {
        if (_depth == _value_depth) _value_depth = 0
        _depth -= 1
      }
      override def characters(chars: Array[Char], start: Int, length: Int): Unit = {
        budget.text += length
        if (budget.text > _max_text) throw new SAXException("XML text limit exceeded")
        if (_value_depth != 0) {
          _value_text += length
          if (_value_text > _max_cell_text) throw new SAXException("cell XML text limit exceeded")
        }
      }
    }
    parser.parse(new InputSource(new ByteArrayInputStream(bytes)), handler)
  }
}
