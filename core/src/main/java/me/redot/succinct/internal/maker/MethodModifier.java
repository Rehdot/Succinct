package me.redot.succinct.internal.maker;

import com.sun.tools.javac.tree.JCTree;
import me.redot.succinct.api.annotation.Modified;
import me.redot.succinct.api.annotation.extra.Attr;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static me.redot.succinct.internal.SuccinctProcessor.context;
import static me.redot.succinct.internal.SuccinctProcessor.helper;

public class MethodModifier {

    private final ExecutableElement annotatedMethod;
    private final ExecutableElement authorMethod;
    private final TypeElement annotation;

    public MethodModifier(ExecutableElement annotatedMethod, ExecutableElement authorMethod, TypeElement annotation) {
        this.annotatedMethod = annotatedMethod;
        this.authorMethod = authorMethod;
        this.annotation = annotation;
    }

    public void modify() {
        JCTree.JCMethodDecl originalMethod = context.javacTrees().getTree(this.annotatedMethod);
        JCTree.JCMethodDecl makerMethod = new MethodMaker(
                this.annotatedMethod,
                this.authorMethod,
                this.annotation
        ).make().getElement();

        originalMethod.mods.annotations = helper.remove(
                originalMethod.mods.annotations,
                helper.getJCAnnotation(this.annotatedMethod, this.annotation)
        );

        AnnotationMirror mirror = helper.getAnnotation(this.authorMethod, Modified.class);
        Set<Attr> attrs = helper.getAttributes(mirror);

        boolean all = attrs.contains(Attr.ALL);

        if (all || attrs.contains(Attr.BODY)) originalMethod.body = makerMethod.body;
        if (all || attrs.contains(Attr.NAME)) originalMethod.name = makerMethod.name;
        if (all || attrs.contains(Attr.TYPE)) originalMethod.restype = makerMethod.restype;
        if (all || attrs.contains(Attr.PARAMS)) originalMethod.params = makerMethod.params;
        if (all || attrs.contains(Attr.MODIFIERS)) originalMethod.mods.flags = makerMethod.mods.flags;
        if (all || attrs.contains(Attr.ANNOTATIONS)) originalMethod.mods.annotations = makerMethod.mods.annotations;
        if (all || attrs.contains(Attr.EXCEPTIONS)) originalMethod.thrown = makerMethod.thrown;
    }

}
