package me.redot.succinct.internal.element;

import com.sun.tools.javac.tree.JCTree;

public interface SuccinctElement<T extends JCTree> {

    T getElement();

}
