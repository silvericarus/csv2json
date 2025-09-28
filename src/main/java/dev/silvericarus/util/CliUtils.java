package dev.silvericarus.util;

public final class CliUtils {
	private CliUtils() {}

	public static char toDelimeterChar (String s) {
		if (s == null || s.isBlank())
			throw new IllegalArgumentException("Delimitador vacío");

		return switch (s) {
			case "," -> ',';
			case ";" -> ';';
			case "\\t", "tab" -> '\t';
			case "|" -> '|';
			default -> throw new IllegalArgumentException("Delimitador no soportado: " + s);
		};
	}
}
