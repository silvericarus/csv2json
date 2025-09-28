package dev.silvericarus.app;

import java.nio.file.Path;
import java.util.*;

import dev.silvericarus.csv.CsvSanitizer;
import dev.silvericarus.csv.DelimeterDetector;
import dev.silvericarus.util.CliUtils;
import java.nio.charset.Charset;
import dev.silvericarus.core.Converter;
import dev.silvericarus.core.Schema;
import dev.silvericarus.core.SchemaMismatchException;

public class CsvToJsonCmd {
	public static int run(Map<String, String> o) throws Exception {
		if (o.containsKey("help")){printHelp();return 0;}

		Path in  = requirePath(o, "in");
		Path out = requirePath(o, "out");

		String delim   = o.getOrDefault("delim", "auto");
		boolean header = o.containsKey("header");
		boolean noHdr  = o.containsKey("no-header");
		if (header && noHdr) throw new IllegalArgumentException("--header y --no-header son excluyentes");

		boolean emptyAsNull = o.containsKey("empty-as-null");
		boolean stringsOnly = o.containsKey("strings-only");
		boolean ndjson      = o.containsKey("ndjson");
		boolean pretty      = o.containsKey("pretty");
		boolean relaxed     = o.containsKey("relaxed-quotes");
		String  encoding    = o.getOrDefault("encoding", "UTF-8");
		Integer limit       = o.containsKey("limit") ? Integer.parseInt(o.get("limit")) : null;
		Path schema         = o.containsKey("schema") ? Path.of(o.get("schema")) : null;

		Charset cs = Charset.forName(encoding);

		final char delimeterChar;
		if ("auto".equalsIgnoreCase(delim)) {
			delimeterChar = DelimeterDetector.detect(in, cs, 200);
			System.err.println("[auto-delim] elegido = " + printable(delimeterChar));
		} else {
			delimeterChar = CliUtils.toDelimeterChar(delim);
		}

		Schema schemaObj = null;
		if (o.containsKey("schema")) {
			Path schemaPath = Path.of(o.get("schema"));
			schemaObj = Schema.load(schemaPath);
		}

		Path source = in;
		if (relaxed) {
			source = CsvSanitizer.sanitizeToTemp(in, cs);
		}

		System.out.printf(Locale.ROOT,
				"[csv2json] in=%s out=%s delim=%s header=%s noHeader=%s ndjson=%s pretty=%s encoding=%s emptyAsNull=%s stringsOnly=%s limit=%s schema=%s%n",
				in, out, delim, header, noHdr, ndjson, pretty, encoding, emptyAsNull, stringsOnly, limit, schema
		);

		try {
			Converter.csvToJson(
					source, out, delimeterChar, header, noHdr,
					emptyAsNull, stringsOnly, ndjson, pretty,
					cs, limit, schemaObj
			);
			return 0;
		} catch (SchemaMismatchException e) {
			System.err.println("[schema] " + e.getMessage());
			return 3;
		}
	}

	private static String printable(char c) {
		return switch (c) {
			case '\t' -> "\\t";
			default -> String.valueOf(c);
		};
	}

	private static Path requirePath(Map<String ,String> o, String key){
		String v = o.get(key);
		if (v == null || v.isBlank()) throw new IllegalArgumentException("Falta --" + key + " <ruta>");
		return Path.of(v);
	}

	private static void printHelp() {
		System.out.println("""
				Uso:
				  converter csv2json --in <input.csv> --out <output.json> [opciones]
				
				Opciones:
				  --delim auto|,|;|\\t||    (por defecto auto)
				  --header | --no-header
				  --schema <path.json>
				  --empty-as-null           (CSV→JSON)
				  --strings-only            (desactiva inferencia)
				  --ndjson                  (salida NDJSON)
				  --pretty                  (JSON con indentación)
				  --limit N
				  --encoding UTF-8
				
				Ejemplos:
				  converter csv2json --in data.csv --out out.ndjson --header --ndjson --delim auto
				  converter csv2json --help
				""");
	}
}
