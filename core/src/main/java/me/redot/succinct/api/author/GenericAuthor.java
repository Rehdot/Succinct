package me.redot.succinct.api.author;

/// type-parameter 'C': the class in which this method should appear.
/// <p>
/// as a general rule of thumb, the type parameter should represent the most
/// derivative class in which the user wants to generate code into.
/// however, Succinct will not try to stop the user from deviating from this rule.
public class GenericAuthor<C> implements Author {

    /// references to 'thiz' are hot-swapped with a 'this' call.
    protected final C thiz = null;

    /// per-generated-member, generates an inline expression set to
    /// the explicit value of the field, in the annotation's instance.
    /// since fields for annotation instances are inline and final,
    /// this isn't so difficult to generate cleanly.
    /// <p>
    /// generic so that users can cast it to anything
    /// <p>
    /// uses zero-based indexing
    protected static <T> T getAnnotationField(int num) {
        return null;
    }

}
