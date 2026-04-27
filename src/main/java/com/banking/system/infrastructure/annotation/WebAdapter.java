package com.banking.system.infrastructure.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/** Marca um Adaptador Web (adaptador primário/driving). */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Documented @Component
public @interface WebAdapter {
    @AliasFor(annotation = Component.class) String value() default "";
}
