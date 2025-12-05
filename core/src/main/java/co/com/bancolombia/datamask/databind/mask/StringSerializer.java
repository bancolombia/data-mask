package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class StringSerializer extends StdSerializer<String> {
    private static final JsonMapper MAPPER = new JsonMapper();
    private MaskingFormat maskingFormat;
    private DataCipher dataCipher;

    public StringSerializer(int leftVisible,
                            int rightVisible,
                            TransformationType transformationType,
                            String format,
                            boolean isEmail,
                            boolean isMultiMask,
                            String separator,
                            DataCipher dataCipher) {
        super(String.class);
        maskingFormat = new MaskingFormat(leftVisible, rightVisible, isEmail, isMultiMask, separator,
                transformationType, format);
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializationContext provider) throws JacksonException {
        try {
            Object resultMask = MaskSerializerCommons.of(maskingFormat, dataCipher).applyMask(value);
            if (resultMask instanceof String) {
                generator.writeString((String) resultMask);
            } else {
                MAPPER.writeValue(generator, resultMask);
            }
        } catch (IOException e) {
            throw new JacksonException("Error during serialization", e) {
            };
        }
    }
}
