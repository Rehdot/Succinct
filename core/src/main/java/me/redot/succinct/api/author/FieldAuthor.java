package me.redot.succinct.api.author;

/// type-parameter 'F': the annotated field's type
/// intended for when the Succinct-based annotation is present on a field.
/// this pattern is specifically useful for generating method bodies.
public class FieldAuthor<C, F> extends GenericAuthor<C> {

    /// references to 'fieldValue' are hot-swapped
    /// with the annotated field we have reference to
    protected F fieldValue = null;

}
