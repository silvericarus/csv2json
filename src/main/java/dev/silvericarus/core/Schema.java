package dev.silvericarus.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Schema {
	private final List<String> columns;
	private final Map<String, ColumnType> typesByName;

	private Schema(List<String> columns, Map<String, ColumnType> typesByName) {
		this.columns = (columns == null) ? null : List.copyOf(columns);
		this.typesByName = (typesByName == null) ? Map.of() : Map.copyOf(typesByName);
	}

	public static Schema load(Path path) throws IOException {
		ObjectMapper om = new ObjectMapper();
		JsonNode root = om.readTree(Files.newBufferedReader(path, Charset.forName("UTF-8")));

		List<String> cols = null;
		if (root.has("columns") && root.get("columns").isArray()) {
			cols = new ArrayList<>();
			for (JsonNode n : root.get("columns")) cols.add(n.asText());
		}

		Map<String, ColumnType> map = new LinkedHashMap<>();
		JsonNode typesNode = root.path("types");
		if (!typesNode.isMissingNode()) {
			if (!typesNode.isObject()) {
				throw new IOException("Schema inválido: 'types' debe ser un objeto {col: tipo}");
			}
			Iterator<Map.Entry<String, JsonNode>> it = typesNode.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> e = it.next();
				String col = e.getKey();
				String typeStr = e.getValue().asText(null);
				if (typeStr == null || typeStr.isBlank()) {
					throw new IOException("Schema inválido: tipo vacío para columna '" + col + "'");
				}
				map.put(col, ColumnType.from(typeStr));
			}
		}

		return new Schema(cols, map);
	}

	public List<String> getColumns() { return columns; }
	public Map<String, ColumnType> getTypesByName() { return typesByName; }

	public ColumnType typeOf(String colName) { return (colName == null) ? null : typesByName.get(colName); }

	public boolean hasColumns() { return columns != null && !columns.isEmpty(); }
	public boolean hasTypes() { return !typesByName.isEmpty(); }
}
