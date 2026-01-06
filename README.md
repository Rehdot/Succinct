## Succinct

**Succinct** is a compile-time metaprogramming tool.
It operates similarly to [Lombok](https://github.com/projectlombok/lombok)
in how it modifies Java's ASTs, but it allows the _user_ 
to define what Java source they'd like to generate.

---

### Templates

Templates are user-written pieces of code which tell Succinct
how the user wants their code to compile. The important
patterns are Authors, Modifiers, and Attributes.

**Authors** (`@Authored`) generate brand-new elements,
while **Modifiers** (`@Modifier`) alter existing elements.
**Attributes** (`Attr`) control what is generated or changed.

---

### Examples
Below are some examples of boilerplate code that a user could generate with Succinct.

<details>
<summary> Recreating Getter </summary>

This is a very simple example of a `@Getter` annotation, similar to Lombok's, but not as robust.
Take note of the usage of `$Name$` replacement, `this.fieldValue`, and `@UseAnnotated(Attr.TYPE)`.
These symbols are all resolved at compile time, and generated into their classes.

```java
package any.pckg.test;

import me.redot.succinct.api.annotation.Authored;
import me.redot.succinct.api.annotation.extra.Attr;
import me.redot.succinct.api.annotation.extra.UseAnnotated;
import me.redot.succinct.api.author.FieldAuthor;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Getter {

    class GetterAuthor extends FieldAuthor<Object, Object> {
        
        @Authored
        @UseAnnotated(Attr.TYPE)
        public Object get$Name$() {
            return this.fieldValue;
        }
    }
}
```

If we apply this annotation onto a field, as it was intended:
```java
package any.pckg.test;

public class Example {

    @Getter
    private final StringBuilder stringBuilder = new StringBuilder("Hello, world!");

}
```

...and then compile, we see this file as the output:
```java
package any.pckg.test;

public class Example {
    private final StringBuilder stringBuilder = new StringBuilder("Hello, world!");

    public StringBuilder getStringBuilder() {
        return this.stringBuilder;
    }
}
```

</details>

<details>
<summary> Thread Safety </summary>

Looking at an example where we're generating multiple elements per-annotated-member,
we could create an author class such as the following:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadLocked {

    class ReadLockAuthor extends ModifiedMethodAuthor<Object> {

        @Authored
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        @Modified(Attr.BODY)
        public void read() {
            this.readWriteLock.readLock().lock();
            try {
                generateOriginalBody();
            } finally {
                this.readWriteLock.readLock().unlock();
            }
        }
    }
}
```

Note its usage of `@Modified(Attr.BODY)` here - the user's intention is to change _only_ the body
of the method. The most important detail here, however, is `generateOriginalBody()`. This is a static method inherited from
ModifiedMethodAuthor, but importantly, Succinct replaces its pattern with the entire body of the annotated method.

Alongside the target method being modified, a `ReadWriteLock` field is authored, which is plain and simple.

If we take an equivalent author annotation for write operations (expand to see):
<details>
<summary> WriteLocked Author </summary>

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WriteLocked {

    class WriteLockAuthor extends ModifiedMethodAuthor<Object> {

        @Authored
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        @Modified(Attr.BODY)
        public void writeLock() {
            this.readWriteLock.writeLock().lock();
            try {
                generateOriginalBody();
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }
}
```
</details>

...and apply these annotations to two ordinary methods in our Example class:
```java
package any.pckg.test;

public class Example {

    @ReadLocked
    public Object readOperation() {
        System.out.println("Pretend this is a read operation!");
        return new Object();
    }

    @WriteLocked
    public void writeOperation(Object object) {
        System.out.println("Pretend this is a write operation!");
    }

}
```
...we see that this class compiles to:
```java
package any.pckg.test;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Example {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public Object readOperation() {
        this.readWriteLock.readLock().lock();

        Object var1;
        try {
            System.out.println("Pretend this is a read operation!");
            var1 = new Object();
        } finally {
            this.readWriteLock.readLock().unlock();
        }

        return var1;
    }

    public void writeOperation(Object object) {
        this.readWriteLock.writeLock().lock();

        try {
            System.out.println("Pretend this is a write operation!");
        } finally {
            this.readWriteLock.writeLock().unlock();
        }

    }
}
```

Note that only one `ReadWriteLock` field was generated. Succinct warns for these patterns:

`[WARNING] Skipped generated field 'readWriteLock', a field with this name already exists.`

...but they're completely fine if intentional.

</details>

<details>
<summary> Logger </summary>

Succinct also has the ability to inline whatever values the annotation instance is holding.

Here's an example:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    String value() default "Hello, world!";

    class LogAuthor extends ModifiedMethodAuthor<Object> {

        @Modified(Attr.BODY)
        public void anyMethod() {
            String inlinedValue = getAnnotationField(0);
            System.out.println(inlinedValue);
            generateOriginalBody();
        }
    }
}
```

The author's usage of `getAnnotationField(0)` here is key. Succinct takes whatever value was supplied in
the annotation's instance, and reconstructs it into an inline expression. The integer supplied to
this method will be which field value (in order) to get. This 'collection' always uses zero-based indexing.

Now, if we annotated this method:

```java
package any.pckg.test;

public class Example {

    @Log("Entered some method...")
    public static String someMethod() {
        String other = "We're in some method...";
        System.out.println(other);
        return other;
    }
}
```

...this would be generated:
```java
package any.pckg.test;

public class Example {
    public static String someMethod() {
        String inlinedValue = "Entered some method...";
        System.out.println(inlinedValue);
        String other = "We're in some method...";
        System.out.println(other);
        return other;
    }
}
```

</details>

---

### Extra Info

- Unless annotated with `@Persistent`, author classes interestingly do not need to compile,
so long as their authored elements can compile in their target classes. This is because
author classes are, by default, removed after Succinct's processing finishes.


- Succinct is plug-and-play, just like Lombok.
  Include the dependency, and immediately generate code.
```xml
<dependency>
    <groupId>me.redot.succinct</groupId>
    <artifactId>processor</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

### Note

Succinct is not a replacement for Lombok. Not to worry though,
Lombok and Succinct are compatible, so you can use both!

Succinct is still in its early stages and needs lots of work.
There are many behaviors and patterns that are very unsupported, as of now.
Some patterns will never compile though, so be careful to always think about 
what contexts you are generating code into.

Have fun!

---

Sincerely,  
**Redot ❤️**