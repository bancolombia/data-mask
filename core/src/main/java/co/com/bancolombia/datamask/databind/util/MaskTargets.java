package co.com.bancolombia.datamask.databind.util;

import lombok.NoArgsConstructor;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MaskTargets {

    /**
     * The masking serializer and deserializer only handle String values. Rejects any other type with a clear
     * message, instead of a ClassCastException when the first value is processed.
     */
    public static void requireString(Annotated annotated) {
        Class<?> type = valueType(annotated);
        if (!String.class.equals(type)) {
            throw new IllegalArgumentException("@Mask supports String members only, but " + describe(annotated)
                    + " is of type " + type.getName());
        }
    }

    // A setter carries its value in its parameter; fields, getters and constructor parameters in their own type.
    private static Class<?> valueType(Annotated annotated) {
        if (annotated instanceof AnnotatedMethod method && method.getParameterCount() == 1) {
            return method.getRawParameterType(0);
        }
        return annotated.getRawType();
    }

    private static String describe(Annotated annotated) {
        return annotated instanceof AnnotatedMember member ? member.getFullName() : annotated.getName();
    }
}
