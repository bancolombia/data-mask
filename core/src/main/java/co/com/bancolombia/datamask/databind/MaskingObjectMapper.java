package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.cipher.DataCipher;
import co.com.bancolombia.datamask.cipher.DataDecipher;
import co.com.bancolombia.datamask.cipher.NoOpCipher;
import co.com.bancolombia.datamask.cipher.NoOpDecipher;
import co.com.bancolombia.datamask.databind.mask.MaskAnnotationIntrospector;
import co.com.bancolombia.datamask.databind.unmask.UnmaskAnnotationIntrospector;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MapperBuilderState;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serial;

import static tools.jackson.databind.AnnotationIntrospector.pair;

public class MaskingObjectMapper extends JsonMapper {

    private static final long serialVersionUID = -1620079742091329228L;

    public MaskingObjectMapper() {
        this(new NoOpCipher(), new NoOpDecipher());
    }

    public MaskingObjectMapper(DataCipher cipher, DataDecipher decipher) {
//        this(new PrivateBuilder(new JsonFactory(), cipher, decipher));
        super(JsonMapper.builder().annotationIntrospector(pair(new MaskAnnotationIntrospector(cipher),
                new UnmaskAnnotationIntrospector(decipher))));
    }

//    protected MaskingObjectMapper(PrivateBuilder builder) {
//        super(builder);
//    }

//    protected static class PrivateBuilder extends MapperBuilder<MaskingObjectMapper, PrivateBuilder> {
//        private final AnnotationIntrospector introspector;
//
//        public PrivateBuilder(TokenStreamFactory tsf, DataCipher cipher, DataDecipher decipher) {
//            super(tsf);
//            MaskAnnotationIntrospector maskAnnotationIntrospector = new MaskAnnotationIntrospector(cipher);
//            UnmaskAnnotationIntrospector unmaskAnnotationIntrospector = new UnmaskAnnotationIntrospector(decipher);
//            introspector = pair(maskAnnotationIntrospector, unmaskAnnotationIntrospector);
//        }
//
//        @Override
//        public MaskingObjectMapper build() {
//            return new MaskingObjectMapper(this.annotationIntrospector(introspector));
//        }
//
//        @Override
//        protected MapperBuilderState _saveState() {
//            return new PrivateBuilder.StateImpl(this, introspector);
//        }
//
//        public PrivateBuilder(MapperBuilderState state, AnnotationIntrospector introspector) {
//            super(state);
//            this.introspector = introspector;
//        }
//
//        static class StateImpl extends MapperBuilderState {
//            private final AnnotationIntrospector introspector;
//
//            public StateImpl(PrivateBuilder b, AnnotationIntrospector introspector) {
//                super(b);
//                this.introspector = introspector;
//            }
//
//            @Serial
//            @Override
//            protected Object readResolve() {
//                return new PrivateBuilder(this, introspector).build();
//            }
//        }
//    }

}
