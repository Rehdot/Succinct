package me.redot.succinct.internal.element;

import com.sun.tools.javac.tree.JCTree;

public interface Appendable {

    void appendTo(JCTree.JCClassDecl clazz);

}
