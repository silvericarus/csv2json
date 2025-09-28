package core;

import dev.silvericarus.core.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterCsvToJsonE2ETest {
	@TempDir Path tmp;

	@Test
	void csvjsonHeaderNdJsonOk() throws Exception {
		String csv = String.join("\n",
				"id,name,age,active,quote",
				"1,Ana,30,true,\"Dijo \"\"hola\"\"\"",
				"2,Luis,,false,\"Multilínea",
				"aquí\""
		);

		Path in = tmp.resolve("data.csv");
		Files.writeString(in, csv, StandardCharsets.UTF_8);

		Path out = tmp.resolve("out.ndjson");

		Converter.csvToJson(in, out, ',', true, false,
				true, false,
				true, false,
				StandardCharsets.UTF_8, null, null);

		List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(2);

		ObjectMapper om = new ObjectMapper();
		JsonNode a = om.readTree(lines.get(0));
		JsonNode b = om.readTree(lines.get(1));

		assertThat(a.get("id").isInt()).isTrue();
		assertThat(a.get("id").asInt()).isEqualTo(1);
		assertThat(a.get("name").asText()).isEqualTo("Ana");
		assertThat(a.get("age").asInt()).isEqualTo(30);
		assertThat(a.get("active").asBoolean()).isTrue();
		assertThat(a.get("quote").asText()).isEqualTo("Dijo \"hola\"");

		assertThat(b.get("id").asInt()).isEqualTo(2);
		assertThat(b.get("age").isNull()).isTrue();
		assertThat(b.get("active").asBoolean()).isFalse();
		assertThat(b.get("quote").asText()).isEqualTo("Multilínea\naquí");	}
}
