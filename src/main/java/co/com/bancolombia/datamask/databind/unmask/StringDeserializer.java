package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

public class StringDeserializer extends StdDeserializer<String> {
    private static final long serialVersionUID = 5586926178824899685L;

    private final DataDecipher dataDecipher;

    public StringDeserializer(DataDecipher dataDecipher) {
        super(String.class);
        this.dataDecipher = dataDecipher;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (isEncryptedString(node)) {
            String[] maskedValuesInfo = split(node.asText());
            return dataDecipher.decipher(maskedValuesInfo[1]);
        } else if (isEncryptedObject(node)) {
            return dataDecipher.decipher(node.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asText());
        } else {
            return node.asText();
        }
    }

    private boolean isEncryptedString(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.STRING))
                .map(JsonNode::asText)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.startsWith(DataMaskingConstants.MASKING_PREFIX))
                .orElse(false);
    }

    private boolean isEncryptedObject(JsonNode node) {
        return Optional.of(node)
                .filter(n -> n.getNodeType().equals(JsonNodeType.OBJECT))
                .map(n -> n.findValue(DataMaskingConstants.MASKING_ATTR) != null
                        && n.findValue(DataMaskingConstants.ENCRYPTED_ATTR) !=null)
                .orElse(false);
    }

    private String[] split(String input) {
        return input.replace(DataMaskingConstants.MASKING_PREFIX,"")
                .split("\\|");
    }
}
