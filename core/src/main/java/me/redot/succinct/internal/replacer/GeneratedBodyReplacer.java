package me.redot.succinct.internal.replacer;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public class GeneratedBodyReplacer extends TreeTranslator {

    private final List<JCTree.JCStatement> statements;

    public GeneratedBodyReplacer(JCTree.JCBlock originalBody) {
        this.statements = this.cleanedStatements(originalBody);
    }

    // in case the original body has any calls to generateOriginalBody
    private List<JCTree.JCStatement> cleanedStatements(JCTree.JCBlock body) {
        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<>();

        // only add all which are explicitly NOT the call
        for (JCTree.JCStatement statement : body.stats) {
            if (!(statement instanceof JCTree.JCExpressionStatement exprStmt)
                    || !(exprStmt.expr instanceof JCTree.JCMethodInvocation call)
                    || !this.isGenerateOriginalBodyCall(call)) {
                newStatements.add(statement);
            }
        }

        return newStatements.toList();
    }

    @Override
    public void visitBlock(JCTree.JCBlock block) {
        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<>();

        for (JCTree.JCStatement statement : block.stats) {
            if (statement instanceof JCTree.JCExpressionStatement exprStmt
                    && exprStmt.expr instanceof JCTree.JCMethodInvocation call
                    && this.isGenerateOriginalBodyCall(call)) {
                newStatements.addAll(this.statements);
            } else {
                newStatements.add(this.translate(statement));
            }
        }

        this.result = context.maker().Block(block.flags, newStatements.toList());
    }

    private boolean isGenerateOriginalBodyCall(JCTree.JCMethodInvocation call) {
        if (call.meth instanceof JCTree.JCIdent ident) {
            return "generateOriginalBody".equals(ident.name.toString());
        }
        if (call.meth instanceof JCTree.JCFieldAccess select) {
            return "generateOriginalBody".equals(select.name.toString());
        }
        return false;
    }

    public JCTree getResult() {
        return this.result;
    }

}