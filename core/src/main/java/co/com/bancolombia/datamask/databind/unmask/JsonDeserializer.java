package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.mask.IdentifyField;
import co.com.bancolombia.datamask.databind.mask.MaskingFormat;
import co.com.bancolombia.datamask.databind.util.QueryType;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class JsonDeserializer extends StdSerializer<DataUnmasked> {

    public static final String SEPARATOR = "/";
    public static final String FORMAT_EXCEPTION = "DataMask only support fields in String format";
    public static final String INVALID_CALLER_EXCEPTION = "This method haven't enable for TransformationType.ONLY_MASK";
    public static final String NOT_FOUND_IN_TREE = " not found in tree.";

    private final DataDecipher dataDecipher;

    public JsonDeserializer(Class<DataUnmasked> clazz, DataDecipher dataCipher) {
        super(clazz);
        this.dataDecipher = dataCipher;
    }

    @Override
    public void serialize(DataUnmasked value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        var objectNode = convertValue(value.getData(), generator.getCodec());
        Map<IdentifyField, MaskingFormat> values = value.getFields();
        findFields(objectNode, values);
        generator.writeObject(objectNode);
    }

    private void findFields(JsonNode node, Map<IdentifyField, MaskingFormat> maskField) {

        boolean existFieldsWithQueryTypeName = maskField.keySet().stream().anyMatch((identifyField) ->
                identifyField.getQueryType().equals(QueryType.NAME));
        if (existFieldsWithQueryTypeName) {
            this.findFieldsForName(node, maskField, null, node);
        }
        maskField.keySet().stream().filter((identifyField) ->
                identifyField.getQueryType().equals(QueryType.PATH)).forEach((identifyField) -> {
            if (maskField.get(identifyField).getTransformationType().equals(TransformationType.ONLY_MASK)) {
                throw new IllegalCallerException(INVALID_CALLER_EXCEPTION);
            }
            String[] querySplit = identifyField.getQuery().split(SEPARATOR);
            this.findFieldForPath(node, identifyField, maskField, querySplit);
        });
    }

    private void findFieldsForName(JsonNode node, Map<IdentifyField, MaskingFormat> maskField,
                                   IdentifyField identifyField, JsonNode parent) {

        if (identifyField != null && maskField.containsKey(identifyField)
                && maskField.get(identifyField).getTransformationType().equals(TransformationType.ONLY_MASK)) {
            throw new IllegalCallerException(INVALID_CALLER_EXCEPTION);
        }
        if (node.isArray()) {
            node.elements().forEachRemaining((element) ->
                    this.findFieldsForName(element, maskField, null, node));
        } else if (node.isObject()) {
            if (isEncryptedObject(node)) {
                decipherValueObject(node, identifyField.getQuery(), (ObjectNode) parent);
            } else {
                node.fields().forEachRemaining((field) -> this.findFieldsForName(field.getValue(), maskField,
                        new IdentifyField(field.getKey(), QueryType.NAME), node));
            }

        } else if (node.isTextual() && identifyField != null && maskField.containsKey(identifyField)) {
            if (isEncryptedString(node)) {
                String[] maskedValuesInfo = MaskUtils.split(node.asText());
                var value = maskedValuesInfo[1];
                decipherValue(value, identifyField.getQuery(), parent);
                return;
            }
            decipherValue(node.textValue(), identifyField.getQuery(), (ObjectNode) parent);
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
                if (isEncryptedObject(previousContext)) {
                    decipherValueObject(node, pathPart, nodeParent);
                    return;
                } else if (isEncryptedString(previousContext)) {
                    String[] maskedValuesInfo = MaskUtils.split(previousContext.asText());
                    var value = maskedValuesInfo[1];
                    decipherValue(value, pathPart, nodeParent);
                    return;
                }
                this.decipherValue(previousContext.textValue(), pathPart, (ObjectNode) nodeParent);
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

    private void decipherValueObject(JsonNode node, String fieldName, JsonNode parent) {
        ((ObjectNode) parent).put(fieldName, dataDecipher.decipher(node.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asText()));
    }

    private void decipherValue(String value, String fieldName, JsonNode parent) {
        ((ObjectNode) parent).put(fieldName, dataDecipher.decipher(value));
    }

    private JsonNode convertValue(Object node, ObjectCodec objectCodec) throws JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) objectCodec;
        if (node instanceof String) {
            return mapper.readTree(node.toString());
        }
        return mapper.convertValue(node, JsonNode.class);
    }

    private boolean isEncryptedString(TreeNode node) {
        return Optional.of(node)
                .filter(n -> n instanceof TextNode)
                .map(nodes -> (TextNode) nodes)
                .map(JsonNode::asText)
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