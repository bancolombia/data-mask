package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.MaskedProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StringArraySerializer extends StdSerializer<String[]> {
    private static final long serialVersionUID = 5586926178824899685L;

    private int leftVisible;
    private int rightVisible;
    private boolean queryOnly = true;
    private String format;
    private DataCipher dataCipher;

    public StringArraySerializer(int leftVisible, int rightVisible, boolean queryOnly, String format, DataCipher dataCipher) {
        super(String[].class);
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.queryOnly = queryOnly;
        this.format = format;
        this.dataCipher = dataCipher;
    }

    @Override
    public void serialize(String[] values, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (!isSuitableForMasking(values)) {
            generator.writeArray(new String[]{},0,0);
            return;
        }

        List maskedList;
        if (DataMaskingConstants.ENCRYPTION_INLINE.equalsIgnoreCase(this.format)) {
            maskedList = maskAndCipherInline(values);
        } else {
            maskedList = maskAndCipherAsObject(values);
        }
        generator.writeObject(maskedList);
    }

    private List<String> maskAndCipherInline(String[] values) {
        var maskedList = new ArrayList<String>();
        for (String v : values) {
            var maskedValue = MaskUtils.mask(v, leftVisible, rightVisible);
            if (queryOnly) {
                maskedList.add(maskedValue);
            } else {
                var cipherValue = dataCipher.cipher(v);
                maskedList.add(DataMaskingConstants.MASKING_PREFIX + maskedValue + "|" + cipherValue);
            }
        }
        return maskedList;
    }

    private List<MaskedProperty> maskAndCipherAsObject(String[] values) {
        var maskedList = new ArrayList<MaskedProperty>();
        for (String v : values) {
            var maskedValue = MaskUtils.mask(v, leftVisible, rightVisible);
            if (queryOnly) {
                maskedList.add(new MaskedProperty(maskedValue, ""));
            } else {
                var cipherValue = dataCipher.cipher(v);
                maskedList.add(new MaskedProperty(maskedValue, cipherValue));
            }
        }
        return maskedList;
    }

    private boolean isSuitableForMasking(String[] value) {
        return value != null && value.length > 0;
    }
}
