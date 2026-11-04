package co.com.bancolombia.datamask.databind;

import co.com.bancolombia.datamask.Mask;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.io.Serial;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rejects {@link Mask} on a method that Jackson does not use as a getter or setter. Such an annotation would
 * otherwise be ignored without any error, and the value it was meant to protect would be written as is.
 */
class MaskAccessorModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = -4417095846214322011L;

    MaskAccessorModule() {
        super("data-mask-accessor-check");
        setSerializerModifier(new SerializerCheck());
        setDeserializerModifier(new DeserializerCheck());
    }

    static void checkAccessors(BeanDescription beanDesc) {
        Set<Method> accessors = new HashSet<>();
        for (BeanPropertyDefinition property : beanDesc.findProperties()) {
            if (property.hasGetter()) {
                accessors.add(property.getGetter().getAnnotated());
            }
            if (property.hasSetter()) {
                accessors.add(property.getSetter().getAnnotated());
            }
        }
        for (AnnotatedMethod method : beanDesc.getClassInfo().memberMethods()) {
            if (method.hasAnnotation(Mask.class) && !accessors.contains(method.getAnnotated())) {
                throw new IllegalArgumentException("@Mask on " + method.getFullName()
                        + " has no effect: Jackson does not use this method as a getter or setter");
            }
        }
    }

    private static class SerializerCheck extends ValueSerializerModifier {

        @Serial
        private static final long serialVersionUID = 6170281437560388527L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription.Supplier beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            checkAccessors(beanDesc.get());
            return beanProperties;
        }
    }

    private static class DeserializerCheck extends ValueDeserializerModifier {

        @Serial
        private static final long serialVersionUID = -2902476650938420344L;

        @Override
        public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config,
                                                             BeanDescription.Supplier beanDesc,
                                                             List<BeanPropertyDefinition> propDefs) {
            checkAccessors(beanDesc.get());
            return propDefs;
        }
    }
}
