package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.JsonNodeType;

import java.util.Optional;

public class StringDeserializer extends StdDeserializer<String> {
    private static final long serialVersionUID = 5586926178824899685L;

    private final DataDecipher dataDecipher;

    public StringDeserializer(DataDecipher dataDecipher) {
        super(String.class);
        this.dataDecipher = dataDecipher;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = p.readValueAsTree();
        if (isEncryptedString(node)) {
            String[] maskedValuesInfo = MaskUtils.split(node.asString());
            return dataDecipher.decipher(maskedValuesInfo[1]);
        } else if (MaskUtils.isEncryptedObject(node)) {
            return dataDecipher.decipher(node.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asString());
        } else {
            return node.asString();
        }
    }

    private boolean isEncryptedString(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.STRING))
                .map(JsonNode::asString)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.startsWith(DataMaskingConstants.MASKING_PREFIX))
                .orElse(false);
    }

}
