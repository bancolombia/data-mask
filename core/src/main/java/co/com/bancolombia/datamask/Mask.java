package co.com.bancolombia.datamask;

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
    boolean queryOnly() default true;
    String format() default DataMaskingConstants.ENCRYPTION_INLINE;
}
