package me.redot.succinct.internal.replacer;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public class ParamTransformResult {

    public final List<JCTree.JCVariableDecl> parameters;
    public final JCTree.JCBlock body;

    public ParamTransformResult(List<JCTree.JCVariableDecl> parameters, JCTree.JCBlock body) {
        this.parameters = parameters;
        this.body = body;
    }

}
