package me.redot.succinct.internal.element;

import com.sun.tools.javac.tree.JCTree;

public class BodyElement implements SuccinctElement<JCTree.JCBlock> {

    private final JCTree.JCBlock block;

    public BodyElement(JCTree.JCBlock block) {
        this.block = block;
    }

    @Override
    public JCTree.JCBlock getElement() {
        return this.block;
    }

}
