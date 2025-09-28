package dev.silvericarus.core;

public final class TypeInference {
	private TypeInference(){}

	public static Object infer(String s, boolean emptyAsNull, boolean stringsOnly) {
		if (s == null) return null;
		if (emptyAsNull && s.isBlank()) return null;
		if (stringsOnly) return s;

		//boolean
		if (equalsIgnoreCaseAscii(s, "true")) return Boolean.TRUE;
		if (equalsIgnoreCaseAscii(s, "false")) return Boolean.FALSE;

		//int
		String t = s.trim();

		try {
			int v = Integer.parseInt(t);
			return v;
		} catch (NumberFormatException ignored) {}

		try {
			long v = Long.parseLong(t);
			return v;
		} catch (NumberFormatException ignored) {}

		try {
			double v = Double.parseDouble(t);
			return v;
		} catch (NumberFormatException ignored) {}

		return s;
	}

	private static boolean equalsIgnoreCaseAscii(String a, String b) {
		if (a.length() != b.length()) return false;
		for (int i = 0; i < a.length(); i++) {
			char c1 = a.charAt(i), c2 = b.charAt(i);
			if (c1 == c2) continue;
			if ('A' <= c1 && c1 <= 'Z') c1 = (char)(c1 + 32);
			if ('A' <= c2 && c2 <= 'Z') c2 = (char)(c2 + 32);
			if(c1 != c2) return false;
		}
		return true;
	}
}
