package me.redot.succinct.api.annotation;

import me.redot.succinct.api.annotation.extra.Attr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modified {

    Attr[] value() default Attr.ALL;

}
