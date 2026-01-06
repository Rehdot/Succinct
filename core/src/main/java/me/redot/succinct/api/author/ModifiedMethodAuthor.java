package me.redot.succinct.api.author;

public class ModifiedMethodAuthor<C> extends GenericAuthor<C> {

    /// A call to this method inside a method we're modifying
    /// will generate the original body of said method,
    /// alongside whatever modifier code we're adding.
    protected static void generateOriginalBody() {

    }

}
