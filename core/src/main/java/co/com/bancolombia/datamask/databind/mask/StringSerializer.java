package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.MaskedProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class StringSerializer extends StdSerializer<String> {
    private static final long serialVersionUID = 5586926178824899685L;

    private int leftVisible;
    private int rightVisible;
    private boolean queryOnly = true;
    private boolean isEmail;
    private String format;
    private DataCipher dataCipher;

    public StringSerializer(int leftVisible, int rightVisible, boolean queryOnly, String format, boolean isEmail, DataCipher dataCipher) {
        super(String.class);
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.queryOnly = queryOnly;
        this.format = format;
        this.isEmail = isEmail;
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (!isSuitableForMasking(value)) {
            generator.writeString(value);
            return;
        }
        var maskedValue = generateMaskedValue(value);
        if (queryOnly) {
            generator.writeString(maskedValue);
        } else {
            writeWithCipher(value, maskedValue, generator);
        }
    }

    private void writeWithCipher(String plainValue, String maskedValue, JsonGenerator generator) throws IOException {
        var cipherValue = dataCipher.cipher(plainValue);
        if (DataMaskingConstants.ENCRYPTION_INLINE.equalsIgnoreCase(this.format)) {
            generator.writeString(DataMaskingConstants.MASKING_PREFIX + maskedValue + "|" + cipherValue);
        } else {
            var data = new MaskedProperty(maskedValue, cipherValue);
            generator.writeObject(data);
        }
    }

    private String generateMaskedValue(String value) {
        if (isEmail) {
            return MaskUtils.maskAsEmail(value);
        } else {
            return MaskUtils.mask(value, leftVisible, rightVisible);
        }
    }

    private boolean isSuitableForMasking(String value) {
        if (StringUtils.isNotBlank(value)) {
            int visibleCharacterCount = leftVisible + rightVisible;
            return value.length() > visibleCharacterCount;
        }
        return false;
    }

}
