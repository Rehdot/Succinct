package me.redot.succinct.api.annotation.extra;

/// Short for Attribute.
/// <p>
/// Represents a part of a piece of code.
/// For example, the name of a method, or a field's type.
public enum Attr {

    TYPE,
    BODY,
    MODIFIERS,
    NAME,
    PARAMS,
    ANNOTATIONS,
    EXCEPTIONS,
    TYPE_PARAMS,
    ALL;

    public static Attr resolve(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

}
