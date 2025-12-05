package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import co.com.bancolombia.datamask.databind.mask.MaskAnnotationIntrospector;
import co.com.bancolombia.datamask.databind.unmask.UnmaskAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serial;

import static tools.jackson.databind.AnnotationIntrospector.pair;

public class MaskingObjectMapper extends JsonMapper {

    @Serial
    private static final long serialVersionUID = -1620079742091329228L;

    public MaskingObjectMapper() {
        this(new NoOpCipher(), new NoOpDecipher());
    }

    public MaskingObjectMapper(DataCipher cipher, DataDecipher decipher) {
        super(builder(cipher, decipher));
    }

    public static JsonMapper.Builder builder(DataCipher cipher, DataDecipher decipher) {
        var introspector = pair(
                new MaskAnnotationIntrospector(cipher),
                new UnmaskAnnotationIntrospector(decipher)
        );
        return JsonMapper.builder().annotationIntrospector(introspector);
    }

}
