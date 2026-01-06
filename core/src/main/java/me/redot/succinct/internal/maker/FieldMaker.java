package me.redot.succinct.internal.maker;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import me.redot.succinct.api.annotation.extra.Attr;
import me.redot.succinct.api.annotation.extra.UseAnnotated;
import me.redot.succinct.internal.element.FieldElement;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

import static me.redot.succinct.internal.SuccinctProcessor.context;
import static me.redot.succinct.internal.SuccinctProcessor.helper;

public class FieldMaker implements ElementMaker<FieldElement> {

    private final Element annotated;
    private final VariableElement authorField;
    private final Set<Attr> attrs = new HashSet<>();

    private String name;
    private long modifiers;
    private TypeReference type;
    private JCTree.JCExpression initializer;
    private List<JCTree.JCAnnotation> annotations;

    public FieldMaker(Element annotated, VariableElement authorField) {
        this.annotated = annotated;
        this.authorField = authorField;

        this.name = authorField.getSimpleName().toString();
        this.modifiers = helper.compileModifiers(authorField.getModifiers());
        this.type = helper.extractTypeReference(authorField);
        this.initializer = helper.extractFieldInitializer(authorField);
        this.annotations = helper.extractAnnotations(authorField);

        this.handleUsedAttributes();
        this.transformVariables();
    }

    private void handleUsedAttributes() {
        AnnotationMirror mirror = helper.getAnnotation(this.authorField, UseAnnotated.class);
        if (mirror == null) return;
        this.attrs.addAll(helper.getAttributes(mirror));
        this.assignUsedAttributes();
    }

    private void assignUsedAttributes() {
        TypeReference annotatedType = helper.extractTypeReference(this.annotated);
        boolean all = this.attrs.contains(Attr.ALL);

        if (all || this.attrs.contains(Attr.TYPE)) {
            this.type = annotatedType;
        }
        if (all || this.attrs.contains(Attr.ANNOTATIONS)) {
            this.annotations = helper.extractAnnotations(this.annotated);
        }
        if (all || this.attrs.contains(Attr.MODIFIERS)) {
            this.modifiers = helper.compileModifiers(this.annotated.getModifiers());
        }
        if (all || this.attrs.contains(Attr.NAME)) {
            this.name = this.annotated.getSimpleName().toString();
        }
    }

    private void transformVariables() {
        String annotatedName = this.annotated.getSimpleName().toString();
        this.name = helper.parseNameTemplate(this.name, annotatedName);
        this.annotations = helper.removeSuccinctApiAnnotations(this.annotations);
        this.initializer = helper.replaceContext(this.initializer, annotatedName);
    }

    @Override
    public FieldElement make() {
        context.messager().printMessage(Diagnostic.Kind.NOTE, "Succinct: Generating field named '" + this.name + "'");

        JCTree.JCVariableDecl field = context.maker().VarDef(
                context.maker().Modifiers(this.modifiers, this.annotations),
                context.names().fromString(this.name),
                this.type.toJCExpression(),
                this.initializer
        );

        return new FieldElement(field);
    }

}
