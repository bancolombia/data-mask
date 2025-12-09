package co.com.bancolombia.datamask.databind.unmask;

import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.databind.util.TransformationType;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;

import java.io.Serial;

@RequiredArgsConstructor
public class UnmaskAnnotationIntrospector extends NopAnnotationIntrospector {

    @Serial
    private static final long serialVersionUID = 2345739318451076683L;

    private final DataDecipher dataDecipher;

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated annotated) {
        Mask annotation = annotated.getAnnotation(Mask.class);
        if (annotation != null && !annotation.queryOnly().equals(TransformationType.ONLY_MASK)) {
            return new StringDeserializer(dataDecipher);
        }
        return null;
    }
}
