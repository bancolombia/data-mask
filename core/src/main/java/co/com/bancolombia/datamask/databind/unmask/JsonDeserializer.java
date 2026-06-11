package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.mask.IdentifyField;
import co.com.bancolombia.datamask.databind.mask.MaskingFormat;
import co.com.bancolombia.datamask.databind.util.JsonNodePathUtils;
import co.com.bancolombia.datamask.databind.util.QueryType;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Map;
import java.util.Optional;

public class JsonDeserializer extends StdSerializer<DataUnmasked> {

    public static final String INVALID_CALLER_EXCEPTION = "This method haven't enable for TransformationType.ONLY_MASK";

    private final DataDecipher dataDecipher;

    public JsonDeserializer(Class<DataUnmasked> clazz, DataDecipher dataCipher) {
        super(clazz);
        this.dataDecipher = dataCipher;
    }

    @Override
    public void serialize(DataUnmasked value, JsonGenerator generator, SerializationContext provider) throws JacksonException {
        JsonMapper mapper = new JsonMapper();
        var objectNode = JsonNodePathUtils.convertValue(value.getData(), mapper);
        Map<IdentifyField, MaskingFormat> values = value.getFields();
        findFields(objectNode, values);
        mapper.writeValue(generator, objectNode);
    }

    private void findFields(JsonNode node, Map<IdentifyField, MaskingFormat> maskField) {

        boolean existFieldsWithQueryTypeName = maskField.keySet().stream().anyMatch(identifyField ->
                identifyField.getQueryType().equals(QueryType.NAME));
        if (existFieldsWithQueryTypeName) {
            this.findFieldsForName(node, maskField, null, node);
        }
        maskField.keySet().stream().filter(identifyField ->
                identifyField.getQueryType().equals(QueryType.PATH)).forEach(identifyField -> {
            if (maskField.get(identifyField).getTransformationType().equals(TransformationType.ONLY_MASK)) {
                throw new IllegalCallerException(INVALID_CALLER_EXCEPTION);
            }
            String[] querySplit = identifyField.getQuery().split(JsonNodePathUtils.SEPARATOR);
            this.findFieldForPath(node, querySplit);
        });
    }

    private void findFieldsForName(JsonNode node, Map<IdentifyField, MaskingFormat> maskField,
                                   IdentifyField identifyField, JsonNode parent) {

        if (identifyField != null && maskField.containsKey(identifyField)
                && maskField.get(identifyField).getTransformationType().equals(TransformationType.ONLY_MASK)) {
            throw new IllegalCallerException(INVALID_CALLER_EXCEPTION);
        }
        if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                this.findFieldsForName(array.get(i), maskField, null, node);
            }
        } else if (node instanceof ObjectNode) {
            if (isEncryptedObject(node)) {
                decipherValueObject(node, identifyField.getQuery(), parent);
            } else {
                for (String key : node.propertyNames()) {
                    this.findFieldsForName(node.get(key), maskField,
                            new IdentifyField(key, QueryType.NAME), node);
                }
            }
        } else if (node.isString() && identifyField != null && maskField.containsKey(identifyField)) {
            if (isEncryptedString(node)) {
                String[] maskedValuesInfo = MaskUtils.split(node.asString());
                var value = maskedValuesInfo[1];
                decipherValue(value, identifyField.getQuery(), parent);
                return;
            }
            decipherValue(node.stringValue(), identifyField.getQuery(), parent);
        }

    }

    private void findFieldForPath(JsonNode node, String[] querySplit) {
        var pathContext = JsonNodePathUtils.navigatePath(node, querySplit);
        var previousContext = pathContext.getContext();
        var nodeParent = pathContext.getParent();
        var pathPart = pathContext.getPathPart();
        if (previousContext.isValueNode()) {
            if (previousContext.isNumber()) {
                throw new IllegalArgumentException(JsonNodePathUtils.FORMAT_EXCEPTION);
            } else {
                if (isEncryptedObject(previousContext)) {
                    decipherValueObject(node, pathPart, nodeParent);
                    return;
                } else if (isEncryptedString(previousContext)) {
                    String[] maskedValuesInfo = MaskUtils.split(previousContext.asString());
                    var value = maskedValuesInfo[1];
                    decipherValue(value, pathPart, nodeParent);
                    return;
                }
                this.decipherValue(previousContext.stringValue(), pathPart, nodeParent);
            }
        }
    }

    private void decipherValueObject(JsonNode node, String fieldName, JsonNode parent) {
        ((ObjectNode) parent).put(fieldName,
                dataDecipher.decipher(node.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asString()));
    }

    private void decipherValue(String value, String fieldName, JsonNode parent) {
        ((ObjectNode) parent).put(fieldName, dataDecipher.decipher(value));
    }

    private boolean isEncryptedString(TreeNode node) {
        return Optional.of(node)
                .filter(StringNode.class::isInstance)
                .map(StringNode.class::cast)
                .map(JsonNode::asString)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.startsWith(DataMaskingConstants.MASKING_PREFIX))
                .orElse(false);
    }

    public static boolean isEncryptedObject(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.OBJECT))
                .map(n -> n.has(DataMaskingConstants.MASKING_ATTR)
                        && n.has(DataMaskingConstants.ENCRYPTED_ATTR))
                .orElse(false);
    }
}
