package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

public class JsonDeserializer extends StdSerializer<DataUnmasked> {

    private final DataDecipher dataDecipher;

    public JsonDeserializer(Class<DataUnmasked> clazz, DataDecipher dataCipher) {
        super(clazz);
        this.dataDecipher = dataCipher;
    }

    @Override
    public void serialize(DataUnmasked value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        var objectNode = convertValue(value.getData() , generator.getCodec());
        value.getFields().forEach(field ->
                objectNode.findParents(field)
                    .forEach(jsonNode -> {
                        if(jsonNode.get(field).isTextual() && isEncryptedString(jsonNode.get(field))) {
                            decipherValue(jsonNode, field);
                        }else if(isEncryptedObject(jsonNode.get(field))){
                            decipherValueObject(jsonNode, field);
                        }
                }));
        generator.writeObject(objectNode);
    }

    private void decipherValueObject(JsonNode jsonNode, String field){
        ((ObjectNode)jsonNode).put(field, dataDecipher.decipher(jsonNode.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asText()));
    }

    private void decipherValue(JsonNode jsonNode, String field){
        String[] maskedValuesInfo = MaskUtils.split(jsonNode.get(field).textValue());
        ((ObjectNode)jsonNode).put(field, dataDecipher.decipher(maskedValuesInfo[1]));
    }

    private JsonNode convertValue(Object node, ObjectCodec objectCodec) throws JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper)objectCodec;
        if(node instanceof String){
            return mapper.readTree(node.toString());
        }
        return mapper.convertValue(node,JsonNode.class);
    }

    private boolean isEncryptedString(TreeNode node) {
        return Optional.of(node)
                .filter(n -> n instanceof TextNode)
                .map(nodes -> (TextNode)nodes)
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