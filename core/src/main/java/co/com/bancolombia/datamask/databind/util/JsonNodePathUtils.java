package co.com.bancolombia.datamask.databind.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNodePathUtils {

    public static final String SEPARATOR = "/";
    public static final String FORMAT_EXCEPTION = "DataMask only support fields in String format";
    public static final String NOT_FOUND_IN_TREE = " not found in tree.";
    public static final String ARRAY_ELEMENT_EXCEPTION = "PATH must end on an object field, not on an array element: ";

    public static JsonNode convertValue(Object node, JsonMapper mapper) {
        if (node instanceof String) {
            return mapper.readTree(node.toString());
        }
        return mapper.valueToTree(node);
    }

    /**
     * Resolves a PATH query. Each field segment is looked up as a direct child first and then in the
     * subtree, so a path may skip intermediate levels. The returned parent is always the node that
     * actually holds the resolved value. A segment that follows an array is read as an index.
     */
    public static PathContext navigatePath(JsonNode node, String[] querySplit) {
        var context = new PathContext(node, node, "");
        var expectIndex = false;
        for (String pathPart : querySplit) {
            if (!pathPart.isEmpty()) {
                context = expectIndex
                        ? findArrayElement(context.context(), pathPart)
                        : findField(context.context(), pathPart);
                expectIndex = context.context().isArray();
            }
        }
        if (!context.pathPart().isEmpty() && context.parent().isArray()) {
            throw new IllegalArgumentException(ARRAY_ELEMENT_EXCEPTION + context.pathPart());
        }
        return context;
    }

    private static PathContext findArrayElement(JsonNode array, String pathPart) {
        int index;
        try {
            index = Integer.parseInt(pathPart);
        } catch (NumberFormatException e) {
            index = -1;
        }
        if (index < 0 || index >= array.size()) {
            throw new IllegalArgumentException("Index \"" + pathPart + "\"" + NOT_FOUND_IN_TREE);
        }
        return new PathContext(array.get(index), array, pathPart);
    }

    private static PathContext findField(JsonNode node, String pathPart) {
        var context = searchField(node, pathPart);
        if (context == null) {
            throw new IllegalArgumentException("\"" + pathPart + "\"" + NOT_FOUND_IN_TREE);
        }
        return context;
    }

    // Same lookup order as JsonNode.findValue, but keeps the parent of the match.
    private static PathContext searchField(JsonNode node, String pathPart) {
        var child = node.get(pathPart);
        if (child != null) {
            return new PathContext(child, node, pathPart);
        }
        for (JsonNode value : node.values()) {
            var context = searchField(value, pathPart);
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    public record PathContext(JsonNode context, JsonNode parent, String pathPart) {
    }
}
