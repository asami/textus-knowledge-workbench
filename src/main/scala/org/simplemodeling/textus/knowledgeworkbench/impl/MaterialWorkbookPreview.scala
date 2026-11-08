package org.simplemodeling.textus.knowledgeworkbench.impl

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.Locale
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
      collected.foreach { entry =>
        if (!entry.isDirectory) {
          val stream = zip.getInputStream(entry)
          val data = try _read_bounded(stream, _max_entry) finally stream.close()
          val name = entry.getName.toLowerCase(Locale.ROOT)
          if (name.endsWith("vbaproject.bin") || name.startsWith("xl/externallinks/"))
            throw new IllegalArgumentException("macros and external workbook links are unsupported")
          if (name.endsWith(".xml") || name.endsWith(".rels")) _check_xml(data, budget)
          else if (!_inert_image(name, data))
            throw new IllegalArgumentException(s"unsupported workbook package part: ${entry.getName}")
        }
      }
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

  private final class XmlBudget {
    var elements = 0
    var attributes = 0
    var text = 0L
    var cells = 0
    var sheets = 0
    var strings = 0
    var styles = 0
  }

  private def _check_xml(bytes: Array[Byte], budget: XmlBudget): Unit = {
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
