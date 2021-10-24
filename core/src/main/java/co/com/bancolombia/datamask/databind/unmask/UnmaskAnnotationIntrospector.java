package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.Mask;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnmaskAnnotationIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 2345739318451076683L;

    private final DataDecipher dataDecipher;

    @Override
    public Object findDeserializer(Annotated annotated) {
        Mask annotation = annotated.getAnnotation(Mask.class);
        if (annotation != null && !annotation.queryOnly()) {
            return new StringDeserializer(dataDecipher);
        }
        return null;
    }
}
