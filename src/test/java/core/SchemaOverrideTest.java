package core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.silvericarus.core.Converter;
import dev.silvericarus.core.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaOverrideTest {
	@TempDir Path tmp;

	@Test
	void schemaOverridesInferenceStringAndBool() throws Exception {
		String csv = String.join("\n",
				"id,name,age,active",
				"1,Ana,30,TRUE"
		);

		Path in = tmp.resolve("data.csv");
		Files.writeString(in, csv, StandardCharsets.UTF_8);

		String schema = """
        {
          "columns": ["id","name","age","active"],
          "types": {
            "age": "string",
            "active": "bool"
          }
        }""";

		Path schemaPath = tmp.resolve("schema.json");
		Files.writeString(schemaPath, schema, StandardCharsets.UTF_8);
		Schema sch = Schema.load(schemaPath);

		Path out = tmp.resolve("out.json");

		Converter.csvToJson(
				in, out, ',',
				true, false,
				true, false,
				false, true,
				StandardCharsets.UTF_8,
				null, sch
		);

		String json = Files.readString(out, StandardCharsets.UTF_8);
		ObjectMapper om = new ObjectMapper();
		JsonNode arr = om.readTree(json);
		assertThat(arr.isArray()).isTrue();
		assertThat(arr).hasSize(1);

		JsonNode obj = arr.get(0);
		assertThat(obj.get("age").isTextual()).isTrue();
		assertThat(obj.get("age").asText()).isEqualTo("30");
		assertThat(obj.get("active").isBoolean()).isTrue();
		assertThat(obj.get("active").asBoolean()).isTrue();
	}


}
