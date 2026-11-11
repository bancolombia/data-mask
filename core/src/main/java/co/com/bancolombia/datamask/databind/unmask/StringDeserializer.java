package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.JsonNodeType;

import java.util.Optional;

public class StringDeserializer extends StdDeserializer<String> {

    private final DataDecipher dataDecipher;
    private final TransformationType transformationType;

    public StringDeserializer(DataDecipher dataDecipher) {
        this(dataDecipher, TransformationType.ALL);
    }

    public StringDeserializer(DataDecipher dataDecipher, TransformationType transformationType) {
        super(String.class);
        this.dataDecipher = dataDecipher;
        this.transformationType = transformationType;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = p.readValueAsTree();
        if (isEncryptedString(node)) {
            Optional<String> encryptedValue = MaskUtils.findEncryptedValue(node.asString());
            if (encryptedValue.isEmpty()) {
                return ctxt.reportInputMismatch(this, MaskUtils.INLINE_FORMAT_EXCEPTION);
            }
            return dataDecipher.decipher(encryptedValue.get());
        } else if (MaskUtils.isEncryptedObject(node)) {
            return dataDecipher.decipher(node.findValue(DataMaskingConstants.ENCRYPTED_ATTR).asString());
        } else if (isCipherOnly(node)) {
            return dataDecipher.decipher(node.asString());
        } else {
            return node.asString();
        }
    }

    // ONLY_CIPHER writes the encrypted value alone, with no prefix and no masked value.
    private boolean isCipherOnly(JsonNode node) {
        return transformationType == TransformationType.ONLY_CIPHER
                && node.getNodeType().equals(JsonNodeType.STRING)
                && StringUtils.isNotBlank(node.asString());
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
