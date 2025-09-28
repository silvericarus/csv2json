package core;

import dev.silvericarus.core.Converter;
import dev.silvericarus.core.Schema;
import dev.silvericarus.core.SchemaMismatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SchemaMissingColumnTest {
	@TempDir Path tmp;

	@Test
	void missingRequiredColumnFails() throws Exception {
		String csv = String.join("\n",
				"id,name",
				"1,Ana"
		);
		Path in = tmp.resolve("data.csv");
		Files.writeString(in, csv, StandardCharsets.UTF_8);

		String schemaJson = """
        {
          "columns": ["id","name","age"],
          "types":   {"age":"int"}
        }""";

		Path schemaPath = tmp.resolve("schema.json");
		Files.writeString(schemaPath, schemaJson, StandardCharsets.UTF_8);
		Schema sch = Schema.load(schemaPath);

		Path out = tmp.resolve("out.json");

		assertThatThrownBy(() ->
				Converter.csvToJson(
						in, out, ',', true, false,
						true, false,
						false, true,
						StandardCharsets.UTF_8,
						null,
						sch
				)
		).isInstanceOf(SchemaMismatchException.class)
				.hasMessageContaining("Columna requerida por schema ausente");
	}
}
