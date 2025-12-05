package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.Mask;
import co.com.bancolombia.datamask.cipher.DataCipher;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;

import java.io.Serial;

@RequiredArgsConstructor
public class MaskAnnotationIntrospector extends NopAnnotationIntrospector {

    @Serial
    private static final long serialVersionUID = 2345739318451076683L;

    private final DataCipher dataCipher;

    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated annotated) {
        Mask annotation = annotated.getAnnotation(Mask.class);
        if (annotation != null) {
            return new StringSerializer(annotation.leftVisible(),
                    annotation.rightVisible(),
                    annotation.queryOnly(),
                    annotation.format(),
                    annotation.isEmail(),
                    annotation.isMultiMask(),
                    annotation.separator(),
                    dataCipher);
        }
        return null;
    }
}
