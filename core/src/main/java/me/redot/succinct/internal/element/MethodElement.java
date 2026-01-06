package me.redot.succinct.internal.element;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import javax.tools.Diagnostic;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public class MethodElement implements SuccinctElement<JCTree.JCMethodDecl>, Appendable {

    private final JCTree.JCMethodDecl method;

    public MethodElement(JCTree.JCMethodDecl method) {
        this.method = method;
    }

    @Override
    public JCTree.JCMethodDecl getElement() {
        return this.method;
    }

    @Override
    public void appendTo(JCTree.JCClassDecl clazz) {
        String name = this.method.name.toString();
        List<JCTree.JCVariableDecl> params = this.method.params;

        for (JCTree def : clazz.defs) {
            if (def instanceof JCTree.JCMethodDecl methodDecl
                    && methodDecl.name.toString().equals(name)
                    && methodDecl.params.equals(params)) {
                context.messager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "Skipped generated method '" + name
                                + "', a method with this name and params already exists."
                );
                return;
            }
        }

        clazz.defs = clazz.defs.append(this.method);
    }

}
