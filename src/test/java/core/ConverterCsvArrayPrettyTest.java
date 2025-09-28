package core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.silvericarus.core.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterCsvArrayPrettyTest {
	@TempDir Path tmp;

	@Test
	void csv2jsonArrayPrettyOk() throws Exception {
		String csv = String.join("\n",
				"id,name,age,active,quote",
				"1,Ana,30,true,\"Dijo \"\"hola\"\"\"",
				"2,Luis,,false,\"Multilínea\naquí\""
		);
		Path in = tmp.resolve("data.csv");
		Files.writeString(in, csv, StandardCharsets.UTF_8);
		Path out = tmp.resolve("out.json");

		Converter.csvToJson(
				in, out, ',',
				true, false,
				true,
				false,
				false,
				true,
				StandardCharsets.UTF_8,
				null,
				null
		);

		String json = Files.readString(out, StandardCharsets.UTF_8);

		assertThat(json.trim()).startsWith("[");
		assertThat(json).contains("\n");

		ObjectMapper om = new ObjectMapper();
		JsonNode root = om.readTree(json);
		assertThat(root.isArray()).isTrue();
		assertThat(root).hasSize(2);

		JsonNode a = root.get(0);
		JsonNode b = root.get(1);

		assertThat(a.get("id").asInt()).isEqualTo(1);
		assertThat(a.get("name").asText()).isEqualTo("Ana");
		assertThat(a.get("age").asInt()).isEqualTo(30);
		assertThat(a.get("active").asBoolean()).isTrue();
		assertThat(a.get("quote").asText()).isEqualTo("Dijo \"hola\"");

		assertThat(b.get("id").asInt()).isEqualTo(2);
		assertThat(b.get("age").isNull()).isTrue();
		assertThat(b.get("active").asBoolean()).isFalse();
		assertThat(b.get("quote").asText()).isEqualTo("Multilínea\naquí");
	}
}
