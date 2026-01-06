package me.redot.succinct.internal.maker;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import me.redot.succinct.api.annotation.Modified;
import me.redot.succinct.api.annotation.extra.Attr;
import me.redot.succinct.api.annotation.extra.UseAnnotated;
import me.redot.succinct.internal.element.MethodElement;
import me.redot.succinct.internal.replacer.ParamTransformResult;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

import static me.redot.succinct.internal.SuccinctProcessor.context;
import static me.redot.succinct.internal.SuccinctProcessor.helper;

/// Builds a fully-functioning method from an annotated element and author method pattern.
/// <p>
/// Replaces contexts within method body, remaps parameter names and return type.
public class MethodMaker implements ElementMaker<MethodElement> {

    private final Element annotated;
    private final ExecutableElement authorMethod;
    private final TypeElement annotation;
    private final Set<Attr> attrs = new HashSet<>();

    private String name;
    private long modifiers;
    private JCTree.JCBlock body;
    private TypeReference returnType;
    private List<JCTree.JCVariableDecl> params;
    private List<JCTree.JCAnnotation> annotations;
    private List<JCTree.JCExpression> exceptions;
    private List<JCTree.JCTypeParameter> typeParams;

    public MethodMaker(Element annotated, ExecutableElement authorMethod, TypeElement annotation) {
        this.annotated = annotated;
        this.authorMethod = authorMethod;
        this.annotation = annotation;

        JCTree.JCMethodDecl authorDecl = context.javacTrees().getTree(authorMethod);

        this.name = authorMethod.getSimpleName().toString();
        this.modifiers = authorDecl.mods.flags;
        this.body = authorDecl.body;
        this.returnType = helper.extractReturnType(authorMethod);
        this.params = authorDecl.params;
        this.annotations = authorDecl.mods.annotations;
        this.exceptions = authorDecl.thrown;
        this.typeParams = authorDecl.typarams;

        this.handleUsedAttributes();
        this.transformVariables();
    }

    private void handleUsedAttributes() {
        AnnotationMirror mirror = helper.getAnnotation(this.authorMethod, UseAnnotated.class);
        if (mirror == null) return;
        this.attrs.addAll(helper.getAttributes(mirror));
        this.assignUsedAttributes();
    }

    private void assignUsedAttributes() {
        TypeReference annotatedType = helper.extractTypeReference(this.annotated);
        boolean all = this.attrs.contains(Attr.ALL);

        if (this.annotated instanceof ExecutableElement method) {
            JCTree.JCMethodDecl methodDecl = context.javacTrees().getTree(method);
            if (all || this.attrs.contains(Attr.PARAMS)) {
                this.params = methodDecl.params;
            }
            if (all || this.attrs.contains(Attr.BODY)) {
                this.body = methodDecl.body;
            }
            if (all || this.attrs.contains(Attr.EXCEPTIONS)) {
                this.exceptions = methodDecl.thrown;
            }
            if (all || this.attrs.contains(Attr.TYPE_PARAMS)) {
                this.typeParams = methodDecl.typarams;
            }
        }
        if (all || this.attrs.contains(Attr.TYPE)) {
            this.returnType = annotatedType;
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
        TypeReference annotatedType = helper.extractTypeReference(this.annotated);
        String annotatedName = this.annotated.getSimpleName().toString();
        this.name = helper.parseNameTemplate(this.name, annotatedName);

        if (helper.isAnnotated(this.authorMethod, Modified.class)) {
            ExecutableElement annotatedMethod = (ExecutableElement) this.annotated;
            this.body = helper.extractMethodBody(annotatedMethod);
            JCTree.JCBlock authorBody = helper.extractMethodBody(this.authorMethod);
            this.body = helper.generateOriginalMethodCalls(authorBody, this.body);
            this.params = helper.extractMethodParams(annotatedMethod);
        }

        AnnotationMirror annotationMirror = helper.getAnnotation(this.annotated, this.annotation);
        this.body = helper.inlineAnnotationFields(this.body, annotationMirror);
        this.body = helper.replaceContext(this.body, annotatedName);
        this.params = this.transformAnnotatedParams(annotatedType);
        this.annotations = helper.remove(this.annotations, helper.getJCAnnotation(this.annotated, this.annotation));

        ParamTransformResult paramResult = helper.transformParameterNames(this.params, this.body, annotatedName);
        this.params = paramResult.parameters;
        this.body = paramResult.body;
        this.annotations = helper.removeSuccinctApiAnnotations(this.annotations);
    }

    @Override
    public MethodElement make() {
        context.messager().printMessage(Diagnostic.Kind.NOTE, "Succinct: Generating method named '" + this.name + "'");

        JCTree.JCMethodDecl method = context.maker().MethodDef(
                context.maker().Modifiers(this.modifiers, this.annotations),
                context.names().fromString(this.name),
                this.returnType.toJCExpression(),
                this.typeParams,
                this.params,
                this.exceptions,
                this.body,
                null
        );

        return new MethodElement(method);
    }

    // this is a bad check, but I don't really have a better one.
    // with the objective of applying whichever attributes the user wants to the parameters,
    // we can (or should) only really apply the annotated type, so it isn't a huge issue.
    // still, I'd like this to be more specific and use the attribute array.
    public List<JCTree.JCVariableDecl> transformAnnotatedParams(TypeReference annotatedType) {
        if (this.params.isEmpty()) {
            return this.params;
        }

        ListBuffer<JCTree.JCVariableDecl> transformed = new ListBuffer<>();

        for (JCTree.JCVariableDecl param : this.params) {
            // TODO: actually check for Attr.TYPE here
            if (!helper.isAnnotated(param.mods, UseAnnotated.class)) {
                transformed.add(param);
                continue;
            }

            List<JCTree.JCAnnotation> filteredAnnotations = helper.removeAnnotation(param.mods.annotations, UseAnnotated.class);
            JCTree.JCModifiers mods = context.maker().Modifiers(param.mods.flags, filteredAnnotations);
            JCTree.JCExpression type = annotatedType.toJCExpression();
            JCTree.JCVariableDecl newParam = context.maker().VarDef(
                    mods,
                    param.name,
                    type,
                    param.init
            );

            transformed.add(newParam);
        }

        return transformed.toList();
    }

}