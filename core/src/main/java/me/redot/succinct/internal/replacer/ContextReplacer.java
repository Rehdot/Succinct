package me.redot.succinct.internal.replacer;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import static me.redot.succinct.internal.SuccinctProcessor.context;

/// Replaces context variables in AST:
/// - fieldValue -> this.<actualFieldName>
/// - thiz -> this
public class ContextReplacer extends TreeTranslator {

    private final String fieldName;

    public ContextReplacer(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public void visitIdent(JCTree.JCIdent ident) {
        String identName = ident.name.toString();

        if ("fieldValue".equals(identName)) {
            this.result = this.makeFieldAccess(this.fieldName);
            return;
        }
        if ("thiz".equals(identName)) {
            this.result = this.makeThis();
            return;
        }

        super.visitIdent(ident);
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess select) {
        String fieldName = select.name.toString();

        if ("fieldValue".equals(fieldName) && isThisAccess(select.selected)) {
            this.result = this.makeFieldAccess(this.fieldName);
            return;
        }
        if ("thiz".equals(fieldName) && isThisAccess(select.selected)) {
            this.result = this.makeThis();
            return;
        }

        super.visitSelect(select);
    }

    private boolean isThisAccess(JCTree.JCExpression expr) {
        return expr instanceof JCTree.JCIdent ident && "this".equals(ident.name.toString());
    }

    private JCTree.JCFieldAccess makeFieldAccess(String field) {
        return context.maker().Select(this.makeThis(), context.names().fromString(field));
    }

    private JCTree.JCIdent makeThis() {
        return context.maker().Ident(context.names()._this);
    }

    public JCTree getResult() {
        return this.result;
    }

}
