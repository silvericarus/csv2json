package dev.silvericarus.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Detecta el delimitador más probable probando candidatos y puntuando
 * la "coherencia" (mismo número de columnas por registro) en un muestreo.
 * Candidatos: ',', ';', '\t', '|'.
 *
 * Heurística de puntuación (menor = mejor):
 *  - penaliza fuertemente si el modo de columnas es 1 (prob. delimitador incorrecto)
 *  - penaliza inconsistencias (filas con nº columnas distinto al modo)
 *  - en empate, prioridad: ',', ';', '\t', '|'
 */
public final class DelimeterDetector {
	private static final char[] CANDIDATES = {',', ';', '\t', '|'};
	private static final int PENALTY_SINGLE_COLUMN = 1_000;
	private static final int PENALTY_INCONSISTENCY = 10;

	private DelimeterDetector() {}

	public static char detect(Path path, Charset charset, int maxRecords) throws IOException {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(charset, "charset");
		if (maxRecords <= 0) maxRecords = 200;

		Detection best = null;

		for (int i = 0; i < CANDIDATES.length; i++){
			char cand = CANDIDATES[i];
			Detection d = scoreCandidateSafe(path, charset, cand, maxRecords, i);
			if (best == null || d.score < best.score) best = d;
		}

		return best != null ? best.delimeter : ',';
	}

	private static Detection scoreCandidateSafe(Path path, Charset cs, char delimeter, int maxRecords, int priorityIndex){
		try {
			return scoreCandidate(path, cs, delimeter, maxRecords, priorityIndex);
		} catch (IOException ex) {
			int badScore = Integer.MAX_VALUE / 2 + priorityIndex;
			return new Detection(delimeter, badScore);
		}
	}

	private static Detection scoreCandidate(Path path, Charset cs, char delimeter, int maxRecords, int priorityIndex) throws IOException {
		CSVFormat fmt = CSVFormat.DEFAULT.builder()
				.setDelimiter(delimeter)
				.setQuote('"')
				.setRecordSeparator(null)
				.setAllowMissingColumnNames(true)
				.build();

		int recCount = 0;
		List<Integer> widths = new ArrayList<>(Math.min(64, maxRecords));

		try(Reader r = Files.newBufferedReader(path, cs);
		    CSVParser p = new CSVParser(r, fmt)){

			for (CSVRecord rec : p) {
				widths.add(rec.size());
				recCount++;
				if (recCount >= maxRecords) break;
			}
		}

		if (recCount == 0){
			return new Detection(delimeter, Integer.MAX_VALUE - priorityIndex);
		}

		int mode = mostFrequent(widths);
		int inconsistencies = 0;
		for (int w : widths) if (w != mode) inconsistencies++;

		int score = inconsistencies * PENALTY_INCONSISTENCY;
		if (mode == 1 && recCount > 1) score += PENALTY_SINGLE_COLUMN;

		score = score * 100 + priorityIndex;

		return new Detection(delimeter, score);
	}

	private static int mostFrequent(List<Integer> xs) {
		Map<Integer, Integer> freq = new HashMap<>();
		for (int x : xs) freq.merge(x, 1, Integer::sum);
		int best = xs.get(0), bestC = 0;
		for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
			int k = e.getKey(), c=e.getValue();
			if (c > bestC || (c == bestC && k > best)) {
				best = k; bestC = c;
			}
		}
		return best;
	}

	private record Detection(char delimeter, int score) {}

}
