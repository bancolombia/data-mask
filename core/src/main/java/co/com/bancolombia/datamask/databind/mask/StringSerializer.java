package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class StringSerializer extends StdSerializer<String> {
    private static final long serialVersionUID = 5586926178824899685L;

    private MaskingFormat maskingFormat;
    private DataCipher dataCipher;

    public StringSerializer(int leftVisible, int rightVisible, boolean queryOnly, String format, boolean isEmail, DataCipher dataCipher) {
        super(String.class);
        maskingFormat = new MaskingFormat(leftVisible, rightVisible, isEmail, queryOnly, format);
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        Object resultMask = MaskSerializerCommons.of(maskingFormat,dataCipher).applyMask(value);
        if(resultMask instanceof String){
            generator.writeString((String)resultMask);
        }else {
            generator.writeObject(resultMask);
        }
    }
}
