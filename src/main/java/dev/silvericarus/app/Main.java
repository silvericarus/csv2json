package dev.silvericarus.app;

import java.util.*;

public class Main{
    public static void main(String[] args) {
		if (args.length == 0 || isHelp(args[0])){
			printGlobalHelp();
			System.exit(0);
		}
		String sub = args[0];
		String[] rest = Arrays.copyOfRange(args, 1, args.length);
		Map<String, String> opts = parseOptions(rest);

		try{
			switch (sub){
				case "csv2json" -> System.exit(CsvToJsonCmd.run(opts));
				case "json2csv" -> System.exit(JsonToCsvCmd.run(opts));
				default -> {
					System.err.println("Subcomando desconocido: " + sub);
					printGlobalHelp();
					System.exit(4);
				}
			}
		} catch (IllegalArgumentException iae){
			System.err.println("Argumentos inválidos: " + iae.getMessage());
			System.exit(4);
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
			System.exit(5);
		}
    }

	private static boolean isHelp(String s){
		return "-h".equals(s) || "--help".equals(s);
	}

	public static Map<String, String> parseOptions(String[] args){
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < args.length; i++){
			String a = args[i];

			if ("--help".equals(a) || "-h".equals(a)) {
				m.put("help", "true");
				continue;
			}

			if (!a.startsWith("--")){
				throw new IllegalArgumentException("Token no reconocido (esperaba --flag): " + a);
			}

			String keyVal = a.substring(2);
			String key, val = "true";
			int eq = keyVal.indexOf("=");
			if (eq >= 0){
				key = keyVal.substring(0, eq);
				val = keyVal.substring(eq + 1);
			} else {
				key = keyVal;
				if ((i + 1) < args.length && !args[i + 1].startsWith("--") && !args[i + 1].startsWith("-")) {
					val = args[++i];
				}
			}
			m.put(key, val);
		}
		return m;
	}

	private static void printGlobalHelp() {
		System.out.println("""
        converter <csv2json|json2csv> [opciones]

        Subcomandos:
          csv2json   Convierte CSV -> JSON (array o NDJSON)
          json2csv   Convierte JSON -> CSV

        Ejemplos:
          converter csv2json --help
          converter json2csv --help
        """);
	}
}
