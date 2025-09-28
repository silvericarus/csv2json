package dev.silvericarus.app;

import java.nio.file.Path;
import java.util.*;

public class JsonToCsvCmd {
	public static int run(Map<String, String> o) throws Exception {
		if (o.containsKey("help")) {
			printHelp();
			return 0;
		}

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
		String  encoding    = o.getOrDefault("encoding", "UTF-8");
		Integer limit       = o.containsKey("limit") ? Integer.parseInt(o.get("limit")) : null;
		Path schema         = o.containsKey("schema") ? Path.of(o.get("schema")) : null;

		System.out.printf(Locale.ROOT,
				"[json2csv] in=%s out=%s delim=%s header=%s noHeader=%s ndjson=%s pretty=%s encoding=%s emptyAsNull=%s stringsOnly=%s limit=%s schema=%s%n",
				in, out, delim, header, noHdr, ndjson, pretty, encoding, emptyAsNull, stringsOnly, limit, schema
		);

		return 0;
	}

	private static Path requirePath(Map<String,String> o, String key) {
		String v = o.get(key);
		if (v == null || v.isBlank()) throw new IllegalArgumentException("Falta --" + key + " <ruta>");
		return Path.of(v);
	}

	private static void printHelp() {
		System.out.println("""
        Uso:
          converter json2csv --in <input.json|.ndjson> --out <output.csv> [opciones]

        Opciones:
          --delim ,|;|\\t||          (por defecto ,)
          --columns a,b,c            (orden de columnas)
          --encoding UTF-8

        Ejemplos:
          converter json2csv --in data.ndjson --out data.csv --columns id,name,age
          converter json2csv --help
        """);
	}
}
