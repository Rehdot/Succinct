package me.redot.succinct.internal.element;

import com.sun.tools.javac.tree.JCTree;

import javax.tools.Diagnostic;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public class FieldElement implements SuccinctElement<JCTree.JCVariableDecl>, Appendable {

    private final JCTree.JCVariableDecl variable;

    public FieldElement(JCTree.JCVariableDecl variable) {
        this.variable = variable;
    }

    @Override
    public JCTree.JCVariableDecl getElement() {
        return this.variable;
    }

    @Override
    public void appendTo(JCTree.JCClassDecl clazz) {
        String fieldName = this.variable.name.toString();

        for (JCTree def : clazz.defs) {
            if (def instanceof JCTree.JCVariableDecl field && fieldName.equals(field.name.toString())) {
                context.messager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "Skipped generated field '" + fieldName
                                + "', a field with this name already exists."
                );
                return;
            }
        }

        clazz.defs = clazz.defs.append(this.variable);
    }

}