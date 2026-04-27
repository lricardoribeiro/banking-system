package com.banking.system.infrastructure.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/** Marca um Serviço de Aplicação / implementação de Caso de Uso. */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Documented @Component
public @interface UseCase {
    @AliasFor(annotation = Component.class) String value() default "";
}
