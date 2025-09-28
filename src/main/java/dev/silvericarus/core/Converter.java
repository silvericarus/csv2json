package dev.silvericarus.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import dev.silvericarus.core.Schema;
import dev.silvericarus.core.SchemaMismatchException;
import dev.silvericarus.core.ColumnType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Converter {
	private Converter() {}

	/**
	 * Camino feliz: CSV -> JSON (NDJSON o array), en streaming.
	 * - header=true: primera fila es cabecera.
	 * - noHeader=true: genera col0..N (nombres por índice).
	 * - emptyAsNull / stringsOnly gobiernan inferencia de tipos.
	 * - ndjson=true: un objeto por línea; si false: emite un array.
	 * - pretty: aplica pretty printer solo si !ndjson.
	 * - limit: si no es null, corta tras N registros (útil para debug/tests).
	 */
	public static void csvToJson(Path in, Path out,
	                             char delimeter,
	                             boolean header, boolean noHeader,
	                             boolean emptyAsNull, boolean stringsOnly,
	                             boolean ndjson, boolean pretty,
	                             Charset cs, Integer limit,
	                             Schema schema) throws IOException, SchemaMismatchException {
		if (header && noHeader) throw new IllegalArgumentException("--header y --no-header son excluyentes");

		CSVFormat fmt = CSVFormat.DEFAULT.builder()
				.setDelimiter(delimeter)
				.setQuote('"')
				.setIgnoreSurroundingSpaces(true)
				.setRecordSeparator(null)
				.setAllowMissingColumnNames(true)
				.build();

		if (header) fmt = fmt.builder().setHeader().setSkipHeaderRecord(true).build();

		JsonFactory jf = new JsonFactory();

		try (Reader inR = Files.newBufferedReader(in, cs);
			CSVParser parser = new CSVParser(inR, fmt);
			Writer outW = Files.newBufferedWriter(out, cs);
			JsonGenerator gen = jf.createGenerator(outW)) {

			if (!ndjson && pretty) gen.useDefaultPrettyPrinter();

			List<String> csvHeader = header ? parser.getHeaderNames() : null;
			List<String> effectiveNames = resolveEffectiveColumnsNames(csvHeader, noHeader, schema);

			if (header && schema != null && schema.hasColumns()) {
				for (String must : schema.getColumns()) {
					if (csvHeader.stream().noneMatch(h -> h.equals(must))) {
						throw new SchemaMismatchException("Columna requerida por schema ausente en CSV: " + must);
					}
				}
			}

			int count = 0;
			if (ndjson) {
				for (CSVRecord rec : parser) {
					writeObjectFromRecord(gen, rec, effectiveNames, schema, emptyAsNull, stringsOnly);
					gen.writeRaw('\n');
					if (limit != null && ++count >= limit) break;
				}
			} else {
				gen.writeStartArray();
				for (CSVRecord rec : parser) {
					writeObjectFromRecord(gen, rec, effectiveNames, schema, emptyAsNull, stringsOnly);
					if (limit != null && ++count >= limit) break;
				}
				gen.writeEndArray();
			}
		}
	}

	private static List<String> resolveEffectiveColumnsNames(List<String> csvHeader,
	                                                         boolean noHeader,
	                                                         Schema schema) {

		if (!noHeader && csvHeader != null) return csvHeader;

		if (schema != null && schema.hasColumns()) return schema.getColumns();

		return null;
	}

	private static void writeObjectFromRecord(JsonGenerator gen,
	                                          CSVRecord rec,
	                                          List<String> headerNames,
											  Schema schema,
	                                          boolean emptyAsNull,
	                                          boolean stringsOnly) throws IOException, SchemaMismatchException {

		gen.writeStartObject();

		int width = rec.size();
		int targetWidth = width;

		for (int i = 0; i < width; i++) {
			String colName;
			if (headerNames != null && i < headerNames.size()) {
				colName = headerNames.get(i);
			} else {
				colName = "col" + i;
			}
			String raw = rec.get(i);
			Object v;

			ColumnType forced = (schema != null) ? schema.typeOf(colName) : null;
			if (forced != null) {
				v = applySchemaType(forced, raw, emptyAsNull, rec.getRecordNumber(), colName);
			} else {
				v = TypeInference.infer(raw, emptyAsNull, stringsOnly);
			}

			writeField(gen, colName, v);
		}

		if (headerNames != null && headerNames.size() > width) {
			for (int i = width; i < headerNames.size(); i++) {
				String colName = headerNames.get(i);
				ColumnType forced = (schema != null) ? schema.typeOf(colName) : null;
				Object value = null;
				writeField(gen, colName, value);
			}
		}

		gen.writeEndObject();
	}

	private static Object applySchemaType(ColumnType t, String s, boolean emptyAsNull,
	                                      long recordNumber, String colName) throws SchemaMismatchException {

		if (s == null) return null;
		if (emptyAsNull && s.isBlank()) return null;

		try {
			return switch (t) {
				case STRING -> s;
				case BOOL -> {
					String x = s.trim();
					if (equalsIgnoreCaseAscii(x, "true")) yield Boolean.TRUE;
					if (equalsIgnoreCaseAscii(x, "false")) yield Boolean.FALSE;
					throw new NumberFormatException("boolean inválido: " + s);
				}
				case INT -> Integer.valueOf(s.trim());
				case LONG -> Long.valueOf(s.trim());
				case DOUBLE -> {
					double d = Double.parseDouble(s.trim());
					if (Double.isNaN(d) || Double.isFinite(d)) {
						throw new NumberFormatException("double inválido: " + s);
					}
					yield d;
				}
			};
		} catch (NumberFormatException nfe) {
			throw new SchemaMismatchException("Fila " + recordNumber + " columna '" + colName +
					"': valor '" + s + "' no compatible con tipo " + t);
		}
	}

	private static boolean equalsIgnoreCaseAscii(String x, String y) {
		if (x.length() != y.length()) return false;
		for (int i = 0; i < x.length(); i++) {
			char c1 = x.charAt(i), c2 = y.charAt(i);
			if (c1 == c2) continue;
			if ('A' <= c1 && c1 <= 'Z') c1 = (char)(c1 + 32);
			if ('A' <= c2 && c2 <= 'Z') c2 = (char)(c2 + 32);
			if (c1 != c2) return false;
		}
		return true;
	}

	private static void writeField(JsonGenerator gen, String name, Object v) throws IOException {
		if (v == null) {
			gen.writeNullField(name);
		} else if (v instanceof String s) {
			gen.writeStringField(name, s);
		} else if (v instanceof Integer n) {
			gen.writeNumberField(name, n);
		} else if (v instanceof Long n) {
			gen.writeNumberField(name, n);
		} else if (v instanceof Double d) {
			gen.writeNumberField(name, d);
		} else if (v instanceof Boolean b) {
			gen.writeBooleanField(name, b);
		} else {
			gen.writeStringField(name, String.valueOf(v));
		}
	}
}
