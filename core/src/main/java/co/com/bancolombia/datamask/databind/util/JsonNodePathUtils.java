package co.com.bancolombia.datamask.databind.util;

import lombok.Getter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

public final class JsonNodePathUtils {

    public static final String SEPARATOR = "/";
    public static final String FORMAT_EXCEPTION = "DataMask only support fields in String format";
    public static final String NOT_FOUND_IN_TREE = " not found in tree.";

    private JsonNodePathUtils() {
    }

    public static JsonNode convertValue(Object node, JsonMapper mapper) {
        if (node instanceof String) {
            return mapper.readTree(node.toString());
        }
        return mapper.valueToTree(node);
    }

    public static PathContext navigatePath(JsonNode node, String[] querySplit) {
        var previousContext = node;
        var nodeParent = node;
        var pathPart = "";
        var nodeParentArray = node;
        var arrayNodeFound = false;
        JsonNode newContext;
        for (String element : querySplit) {
            pathPart = element;
            if (!pathPart.isEmpty()) {
                if (arrayNodeFound) {
                    nodeParent = findArrayParent(nodeParentArray, pathPart, nodeParent);
                    previousContext = nodeParent;
                    newContext = nodeParent;
                    arrayNodeFound = false;
                } else {
                    newContext = findContext(previousContext, pathPart);
                    previousContext = newContext;
                }
                if (!newContext.isValueNode()) {
                    nodeParent = newContext;
                }
                if (newContext.isArray()) {
                    nodeParentArray = newContext;
                    arrayNodeFound = true;
                }
            }
        }
        return new PathContext(previousContext, nodeParent, pathPart);
    }

    private static JsonNode findArrayParent(JsonNode arrayNodeParent, String pathPart, JsonNode referenceParent) {
        int index = Integer.parseInt(pathPart);
        var context = (ArrayNode) arrayNodeParent;

        for (int i = 0; i < context.size(); i++) {
            if (i == index && !context.get(i).isValueNode()) {
                referenceParent = context.get(i);
                return referenceParent;
            }
        }
        return referenceParent;
    }

    private static JsonNode findContext(JsonNode node, String pathPart) {
        JsonNode context = node.findValue(pathPart);
        if (context == null) {
            throw new IllegalArgumentException("\"" + pathPart + "\"" + NOT_FOUND_IN_TREE);
        }
        return context;
    }

    @Getter
    public static class PathContext {
        private final JsonNode context;
        private final JsonNode parent;
        private final String pathPart;

        public PathContext(JsonNode context, JsonNode parent, String pathPart) {
            this.context = context;
            this.parent = parent;
            this.pathPart = pathPart;
        }
    }
}
