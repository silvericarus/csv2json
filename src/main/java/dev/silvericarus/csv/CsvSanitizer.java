package dev.silvericarus.csv;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;

public final class CsvSanitizer {
	private CsvSanitizer() {}

	public static Path sanitizeToTemp(Path in, Charset cs, char... delims) throws IOException {
		if (delims == null || delims.length == 0) delims = new char[]{',',';','\t','|'};

		Path tmp = Files.createTempFile("csv-relaxed-", ".csv");
		try (Reader rr = Files.newBufferedReader(in, cs);
		     BufferedReader r = new BufferedReader(rr);
			 Writer w = Files.newBufferedWriter(tmp, cs)) {

			boolean inQuotes = false;
			boolean justClosedQuote = false;
			int ch;
			while ((ch = r.read()) != -1) {
				char c = (char) ch;

				if (c == '"') {
					if (inQuotes) {
						r.mark(1);
						int nxt = r.read();
						if (nxt == '"'){
							w.write('"'); w.write('"');
						} else {
							inQuotes = false;
							justClosedQuote = true;
							w.write('"');
						}
					} else {
						inQuotes = true;
						w.write('"');
					}
					continue;
				}

				if (!inQuotes && justClosedQuote) {
					if (isAnySpace(c)) {
						r.mark(1);
						int nx = r.read();
						if (nx == -1) break;
						char cn = (char) nx;
						if (isDelimiter(cn, delims) || cn == '\n' || cn == '\r') {
							w.write(cn);
						} else {
							w.write(' ');
							w.write(cn);
						}
						justClosedQuote = false;
						continue;
					} else {
						w.write(c);
						justClosedQuote = false;
						continue;
					}
				}

				if (c == '\n' || c == '\r') justClosedQuote = false;
				w.write(c);
			}
		}
		tmp.toFile().deleteOnExit();
		return tmp;
	}

	private static boolean isDelimiter(char c, char[] delims) {
		for (char d : delims) if (c == d) return true;
		return false;
	}

	private static boolean isAnySpace(char c) {
		return c == ' ' || c == '\t' || Character.isWhitespace(c) || Character.isSpaceChar(c);
	}
}
