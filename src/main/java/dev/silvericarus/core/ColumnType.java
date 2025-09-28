package dev.silvericarus.core;

import java.util.Locale;

public enum ColumnType {
	STRING, INT,
	LONG, DOUBLE,
	BOOL;

	public static ColumnType from(String s) {
		if (s == null) return null;
		return switch (s.toLowerCase(Locale.ROOT)) {
			case "string", "str", "text" -> STRING;
			case "int", "integer" -> INT;
			case "long" -> LONG;
			case "double", "float", "number" -> DOUBLE;
			case "bool", "boolean" -> BOOL;
			default -> throw new IllegalArgumentException("Tipo no soportado en schema: " + s);
		};
	}
}
