package me.redot.succinct.internal.replacer;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import java.util.Map;

import static me.redot.succinct.internal.SuccinctProcessor.context;

/// Replaces parameter names in method bodies.
public class ParamNameReplacer extends TreeTranslator {

    private final Map<String, String> replacements;

    public ParamNameReplacer(Map<String, String> replacements) {
        this.replacements = replacements;
    }

    @Override
    public void visitIdent(JCTree.JCIdent ident) {
        String identName = ident.name.toString();

        if (this.replacements.containsKey(identName)) {
            String newName = this.replacements.get(identName);
            this.result = this.makeIdent(newName);
            return;
        }

        super.visitIdent(ident);
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess select) {
        String fieldName = select.name.toString();

        if (this.replacements.containsKey(fieldName)) {
            String newName = this.replacements.get(fieldName);
            JCTree.JCExpression translated = this.translate(select.selected);
            this.result = this.makeSelect(translated, newName);
            return;
        }

        super.visitSelect(select);
    }

    private JCTree.JCIdent makeIdent(String name) {
        return context.maker().Ident(context.names().fromString(name));
    }

    private JCTree.JCFieldAccess makeSelect(JCTree.JCExpression receiver, String field) {
        return context.maker().Select(receiver, context.names().fromString(field));
    }

    public JCTree getResult() {
        return this.result;
    }

}