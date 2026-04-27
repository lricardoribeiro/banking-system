package com.banking.system.infrastructure.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/** Marca um Adaptador de Mensageria (adaptador secundário/driven para brokers de mensagens). */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Documented @Component
public @interface MessagingAdapter {
    @AliasFor(annotation = Component.class) String value() default "";
}
