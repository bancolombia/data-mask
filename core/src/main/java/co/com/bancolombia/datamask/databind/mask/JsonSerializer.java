package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.util.QueryType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

public class JsonSerializer extends StdSerializer<DataMask> {

    public static final String SEPARATOR = "/";
    public static final String FORMAT_EXCEPTION = "DataMask only support fields in String format";
    public static final String NOT_FOUND_IN_TREE = " not found in tree.";
    private final DataCipher dataCipher;

    public JsonSerializer(Class<DataMask> dataMaskClass, DataCipher dataCipher) {
        super(dataMaskClass);
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(DataMask value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        var objectNode = convertValue(value.getData(), generator.getCodec());
        Map<IdentifyField, MaskingFormat> values = value.getFields();
        findFields(objectNode, values);
        generator.writeObject(objectNode);
    }

    private void findFields(JsonNode node, Map<IdentifyField, MaskingFormat> maskField) {
        maskField.keySet().stream().filter((identifyField) ->
                identifyField.getQueryType().equals(QueryType.PATH)).forEach((identifyField) -> {
            String[] querySplit = identifyField.getQuery().split(SEPARATOR);
            this.findFieldForPath(node, identifyField, maskField, querySplit);
        });
        boolean existFieldsWithQueryTypeName = maskField.keySet().stream().anyMatch((identifyField) ->
                identifyField.getQueryType().equals(QueryType.NAME));
        if (existFieldsWithQueryTypeName) {
            this.findFieldsForName(node, maskField, null, node);
        }
    }

    private void findFieldForPath(JsonNode node, IdentifyField identifyField,
                                  Map<IdentifyField, MaskingFormat> maskField, String[] querySplit) {
        var previousContext = node;
        var nodeParent = node;
        var pathPart = "";
        var nodeParentArray = node;
        var arrayNodeFound = false;
        var newContext = node;
        for (String element : querySplit) {
            pathPart = element;
            if (!pathPart.equals("")) {
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
        if (previousContext.isValueNode()) {
            if (previousContext.isNumber()) {
                throw new IllegalArgumentException(FORMAT_EXCEPTION);
            } else {
                this.applyMask(previousContext.textValue(), maskField.get(identifyField),
                        pathPart, (ObjectNode) nodeParent);
            }
        }
    }

    private JsonNode findArrayParent(JsonNode arrayNodeParent, String pathPart, JsonNode referenceParent) {
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

    private JsonNode findContext(JsonNode node, String pathPart) {
        JsonNode context = node.findValue(pathPart);
        if (context == null) {
            throw new IllegalArgumentException("\"" + pathPart + "\"" + NOT_FOUND_IN_TREE);
        }
        return context;
    }

    private void findFieldsForName(JsonNode node, Map<IdentifyField, MaskingFormat> maskField,
                                   IdentifyField identifyField, JsonNode parent) {
        if (node.isArray()) {
            node.elements().forEachRemaining((element) ->
                    this.findFieldsForName(element, maskField, null, node));
        } else if (node.isObject()) {
            node.fields().forEachRemaining((field) -> this.findFieldsForName(field.getValue(), maskField,
                    new IdentifyField(field.getKey(), QueryType.NAME), node));
        } else if (node.isTextual() && identifyField != null && maskField.containsKey(identifyField)) {
            this.applyMask(node.textValue(), maskField.get(identifyField),
                    identifyField.getQuery(), (ObjectNode) parent);
        }

    }

    private JsonNode convertValue(Object node, ObjectCodec objectCodec) throws JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) objectCodec;
        if (node instanceof String) {
            return mapper.readTree(node.toString());
        }
        return mapper.convertValue(node, JsonNode.class);
    }

    private void applyMask(String value, MaskingFormat format, String field, ObjectNode node) {
        try {
            Object resultMask = MaskSerializerCommons.of(format, dataCipher).applyMask(value);
            if (resultMask instanceof String) {
                node.put(field, (String) resultMask);
            } else {
                node.putPOJO(field, resultMask);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}