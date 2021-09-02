package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.Mask;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaskAnnotationIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 2345739318451076683L;

    private final DataCipher dataCipher;

    @Override
    public Object findSerializer(Annotated annotated) {
        Mask annotation = annotated.getAnnotation(Mask.class);
        if (annotation != null) {
            if (annotated.getRawType().isArray()) { //TODO: change to query and select serialized based on type
                return new StringArraySerializer(annotation.leftVisible(),
                        annotation.rightVisible(),
                        annotation.queryOnly(),
                        annotation.format(),
                        dataCipher);
            } else {
                return new StringSerializer(annotation.leftVisible(),
                        annotation.rightVisible(),
                        annotation.queryOnly(),
                        annotation.format(),
                        dataCipher);
            }
        }
        return null;
    }
}
