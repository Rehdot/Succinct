package me.redot.succinct.internal.replacer;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import javax.lang.model.element.AnnotationMirror;

import static me.redot.succinct.internal.SuccinctProcessor.helper;

/// Replaces getAnnotationField() calls with actual annotation values
public class AnnotationValueReplacer extends TreeTranslator {

    private final AnnotationMirror annotation;

    public AnnotationValueReplacer(AnnotationMirror annotation) {
        this.annotation = annotation;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation methodInvocation) {
        if (this.isGetAnnotationFieldCall(methodInvocation)) {
            int fieldIndex = this.extractFieldIndex(methodInvocation);
            this.result = helper.getAnnotationFieldExpression(this.annotation, fieldIndex);
            return;
        }

        super.visitApply(methodInvocation);
    }

    private boolean isGetAnnotationFieldCall(JCTree.JCMethodInvocation call) {
        if (call.meth instanceof JCTree.JCIdent ident) {
            return "getAnnotationField".equals(ident.name.toString());
        }
        return false;
    }

    private int extractFieldIndex(JCTree.JCMethodInvocation call) {
        if (call.args.isEmpty()) {
            throw new IllegalArgumentException("getAnnotationField() requires an index argument");
        }

        JCTree.JCExpression arg = call.args.head;
        if (arg instanceof JCTree.JCLiteral literal) {
            return (Integer) literal.value;
        }

        throw new IllegalArgumentException("getAnnotationField() index must be a compile-time constant");
    }

    public JCTree getResult() {
        return this.result;
    }

}
