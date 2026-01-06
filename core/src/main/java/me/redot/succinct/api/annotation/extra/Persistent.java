package me.redot.succinct.api.annotation.extra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// When this annotation is applied to an author class,
/// said class is not deleted before final compilation.
/// <p>
/// If you plan on using this, make sure to include Succinct during runtime,
/// and make sure the author class can compile on its own.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persistent {

}
