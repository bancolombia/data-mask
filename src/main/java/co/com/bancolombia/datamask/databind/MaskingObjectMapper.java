package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import co.com.bancolombia.datamask.databind.mask.MaskAnnotationIntrospector;
import co.com.bancolombia.datamask.databind.unmask.UnmaskAnnotationIntrospector;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair.pair;

public class MaskingObjectMapper extends ObjectMapper {

    private static final long serialVersionUID = -1620079742091329228L;

    public MaskingObjectMapper(DataCipher cipher, DataDecipher decipher) {
        super();

        MaskAnnotationIntrospector maskAnnotationIntrospector = new MaskAnnotationIntrospector(cipher);
        AnnotationIntrospector pair = pair(this.getSerializationConfig().getAnnotationIntrospector(), maskAnnotationIntrospector);

        UnmaskAnnotationIntrospector unmaskAnnotationIntrospector = new UnmaskAnnotationIntrospector(decipher);
        AnnotationIntrospector pair2 = pair(this.getDeserializationConfig().getAnnotationIntrospector(), unmaskAnnotationIntrospector);

        this.setAnnotationIntrospectors(pair, pair2);
    }

    public MaskingObjectMapper() {
        this(new NoOpCipher(), new NoOpDecipher());
    }

}
