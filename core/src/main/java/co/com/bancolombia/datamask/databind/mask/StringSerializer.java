package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdSerializer;

public class StringSerializer extends StdSerializer<String> {
    private static final JsonMapper MAPPER = new JsonMapper();
    private final MaskSerializerCommons maskSerializerCommons;

    public StringSerializer(int leftVisible,
                            int rightVisible,
                            TransformationType transformationType,
                            String format,
                            boolean isEmail,
                            boolean isMultiMask,
                            String separator,
                            DataCipher dataCipher) {
        super(String.class);
        var maskingFormat = new MaskingFormat(leftVisible, rightVisible, isEmail, isMultiMask, separator,
                transformationType, format);
        this.maskSerializerCommons = MaskSerializerCommons.of(maskingFormat, dataCipher);
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializationContext provider) throws JacksonException {
        Object resultMask = maskSerializerCommons.applyMask(value);
        if (resultMask instanceof String resultMaskStr) {
            generator.writeString(resultMaskStr);
        } else {
            MAPPER.writeValue(generator, resultMask);
        }
    }
}
