package co.com.bancolombia.datamask;

import co.com.bancolombia.datamask.databind.util.TransformationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Mask {
    int leftVisible() default 0;
    int rightVisible() default 0;
    boolean isEmail() default false;
    TransformationType queryOnly() default TransformationType.ONLY_MASK;
    boolean isMultiMask() default false;
    String separator() default " ";
    String format() default DataMaskingConstants.ENCRYPTION_INLINE;
}
