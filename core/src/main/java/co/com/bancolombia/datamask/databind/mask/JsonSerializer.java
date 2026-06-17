package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.util.JsonNodePathUtils;
import co.com.bancolombia.datamask.databind.util.QueryType;
import co.com.bancolombia.datamask.exceptions.DataMaskException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Map;

public class JsonSerializer extends StdSerializer<DataMask<?>> {

    private final DataCipher dataCipher;

    public JsonSerializer(Class<DataMask> dataMaskClass, DataCipher dataCipher) { // NOSONAR
        super(dataMaskClass);
        this.dataCipher = dataCipher;
    }


    @Override
    public void serialize(DataMask value, JsonGenerator generator, SerializationContext provider) throws JacksonException {
        JsonMapper mapper = new JsonMapper();
        JsonNode objectNode = JsonNodePathUtils.convertValue(value.data(), mapper);
        Map<IdentifyField, MaskingFormat> values = value.fields();
        findFields(objectNode, values);
        generator.writeTree(objectNode);
    }

    private void findFields(JsonNode node, Map<IdentifyField, MaskingFormat> maskField) {
        maskField.keySet().stream().filter(identifyField ->
                identifyField.getQueryType().equals(QueryType.PATH)).forEach(identifyField -> {
            String[] querySplit = identifyField.getQuery().split(JsonNodePathUtils.SEPARATOR);
            this.findFieldForPath(node, identifyField, maskField, querySplit);
        });
        boolean existFieldsWithQueryTypeName = maskField.keySet().stream().anyMatch(identifyField ->
                identifyField.getQueryType().equals(QueryType.NAME));
        if (existFieldsWithQueryTypeName) {
            this.findFieldsForName(node, maskField, null, node);
        }
    }

    private void findFieldForPath(JsonNode node, IdentifyField identifyField,
                                  Map<IdentifyField, MaskingFormat> maskField, String[] querySplit) {
        var pathContext = JsonNodePathUtils.navigatePath(node, querySplit);
        var previousContext = pathContext.getContext();
        if (previousContext.isValueNode()) {
            if (previousContext.isNumber()) {
                throw new IllegalArgumentException(JsonNodePathUtils.FORMAT_EXCEPTION);
            } else {
                this.applyMask(previousContext.stringValue(), maskField.get(identifyField),
                        pathContext.getPathPart(), (ObjectNode) pathContext.getParent());
            }
        }
    }

    private void findFieldsForName(JsonNode node, Map<IdentifyField, MaskingFormat> maskField,
                                   IdentifyField identifyField, JsonNode parent) {
        if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                this.findFieldsForName(array.get(i), maskField, null, node);
            }
        } else if (node instanceof ObjectNode obj) {
            for (String key : obj.propertyNames()) {
                this.findFieldsForName(obj.get(key), maskField,
                        new IdentifyField(key, QueryType.NAME), node);
            }
        } else if (node.isString() && identifyField != null && maskField.containsKey(identifyField)) {
            this.applyMask(node.stringValue(), maskField.get(identifyField),
                    identifyField.getQuery(), (ObjectNode) parent);
        }
    }

    private void applyMask(String value, MaskingFormat format, String field, ObjectNode node) {
        try {
            Object resultMask = MaskSerializerCommons.of(format, dataCipher).applyMask(value);
            if (resultMask instanceof String resultMaskStr) {
                node.put(field, resultMaskStr);
            } else {
                node.putPOJO(field, resultMask);
            }
        } catch (Exception exception) {
            throw new DataMaskException(exception);
        }
    }
}
