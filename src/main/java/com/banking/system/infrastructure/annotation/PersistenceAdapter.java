package com.banking.system.infrastructure.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/** Marca um Adaptador de Persistência (adaptador secundário/driven). */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Documented @Component
public @interface PersistenceAdapter {
    @AliasFor(annotation = Component.class) String value() default "";
}
