package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

public class JsonSerializer extends StdSerializer<DataMask> {

    private final DataCipher dataCipher;

    public JsonSerializer(Class<DataMask> dataMaskClass, DataCipher dataCipher) {
        super(dataMaskClass);
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(DataMask value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        var objectNode = convertValue(value.getData() , generator.getCodec());
        Map<String, MaskingFormat>  values = value.getFields();
        findFields(objectNode, values, null, objectNode);
        generator.writeObject(objectNode);
    }

    private void findFields(JsonNode node, Map<String, MaskingFormat> maskField, String fieldName, JsonNode parent){
        if(node.isArray()){
            node.elements().forEachRemaining(element -> findFields(element, maskField,null, node));
        }else if(node.isObject()){
            node.fields().forEachRemaining(field -> findFields(field.getValue(), maskField, field.getKey(), node));
        }else if(node.isTextual() && fieldName != null && maskField.containsKey(fieldName)){
            applyMask(node.textValue(), maskField.get(fieldName), fieldName, (ObjectNode)parent);
        }
    }

    private JsonNode convertValue(Object node, ObjectCodec objectCodec) throws JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper)objectCodec;
        if(node instanceof String){
            return mapper.readTree(node.toString());
        }
        return mapper.convertValue(node,JsonNode.class);
    }

    private void applyMask(String value, MaskingFormat format, String field, ObjectNode node)  {
        try {
            Object resultMask = MaskSerializerCommons.of(format, dataCipher).applyMask(value);
            if(resultMask instanceof String){
                node.put(field,(String)resultMask);
            }else {
                node.putPOJO(field,resultMask);
            }
        }catch (IOException exception){
            throw new RuntimeException(exception);
        }
    }
}