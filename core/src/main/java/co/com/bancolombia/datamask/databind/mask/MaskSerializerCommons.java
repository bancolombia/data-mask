package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.MaskUtils;
import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.databind.MaskedProperty;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static java.lang.Boolean.TRUE;

@RequiredArgsConstructor(staticName = "of")
public class MaskSerializerCommons {

    private final MaskingFormat maskingFormat;
    private final DataCipher dataCipher;

    public Object applyMask(String value) throws IOException {
        if (!isSuitableForMasking(value)) {
            return value;
        }
        var maskedValue = generateMaskedValue(value);
        if (maskingFormat.isQueryOnly()) {
            return maskedValue;
        } else {
            return writeWithCipher(value, maskedValue);
        }
    }

    private Object writeWithCipher(String plainValue, String maskedValue) throws IOException {
        var cipherValue = dataCipher.cipher(plainValue);
        if (DataMaskingConstants.ENCRYPTION_INLINE.equalsIgnoreCase(maskingFormat.getFormat())) {
            return DataMaskingConstants.MASKING_PREFIX + maskedValue + "|" + cipherValue;
        } else {
            return new MaskedProperty(maskedValue, cipherValue);
        }
    }

    private String generateMaskedValue(String value) {
        if (TRUE.equals(maskingFormat.getIsEmail())) {
            return MaskUtils.maskAsEmail(value,maskingFormat.getLeftVisible(), maskingFormat.getRightVisible());
        } else {
            return MaskUtils.mask(value, maskingFormat.getLeftVisible(), maskingFormat.getRightVisible());
        }
    }

    private boolean isSuitableForMasking(String value) {
        if (StringUtils.isNotBlank(value)) {
            int visibleCharacterCount = maskingFormat.getLeftVisible() + maskingFormat.getRightVisible();
            return value.length() > visibleCharacterCount;
        }
        return false;
    }
}