package core;

import dev.silvericarus.csv.DelimeterDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.silvericarus.csv.DelimeterDetector.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DelimeterDetectorTest {

	@TempDir
	Path tmp;

	@Test
	void detectCommaSimple() throws Exception {
		String content = String.join("\n",
				"id,name,age",
				"1,Ana,30",
				"2,Luis,31");
		Path f = tmp.resolve("comma.csv");
		Files.writeString(f, content, StandardCharsets.UTF_8);

		char d = detect(f, StandardCharsets.UTF_8, 200);
		assertThat(d).isEqualTo(',');
	}

	@Test
	void detectSemicolonWithQuotedDelimsInsideField() throws Exception {
		String content = String.join("\n",
				"nombre;Genero;edad",
				"Ana;F;30",
				"Luis;M;31");
		Path f = tmp.resolve("semi.csv");
		Files.writeString(f, content, StandardCharsets.UTF_8);

		char d = detect(f, StandardCharsets.UTF_8, 200);
		assertThat(d).isEqualTo(';');
	}

	@Test
	void detectTabAgainstCommasInText() throws Exception {
		String content = String.join("\n",
				"id\tname\tcomment",
				"1\tAna\t\"line with, comma\"",
				"2\tLuis\t\"another, thing\"");
		Path f = tmp.resolve("tab.tsv");
		Files.writeString(f, content, StandardCharsets.UTF_8);

		char d = DelimeterDetector.detect(f, StandardCharsets.UTF_8, 200);
		assertThat(d).isEqualTo('\t');
	}

	@Test
	void detectPipeBasic() throws Exception {
		String content = String.join("\n",
				"a|b|c",
				"1|2|3",
				"4|5|6");
		Path f = tmp.resolve("pipe.csv");
		Files.writeString(f, content, StandardCharsets.UTF_8);

		char d = DelimeterDetector.detect(f, StandardCharsets.UTF_8, 200);
		assertThat(d).isEqualTo('|');
	}

	@Test
	void ambiguousPrefersComma() throws Exception {
		String content = String.join("\n", "alpha", "beta", "gamma");
		Path f = tmp.resolve("amb.csv");
		Files.writeString(f, content, StandardCharsets.UTF_8);

		char d = DelimeterDetector.detect(f, StandardCharsets.UTF_8, 200);
		assertThat(d).isEqualTo(',');
	}
}
