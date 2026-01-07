package me.redot.succinct.internal.model;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import me.redot.succinct.api.annotation.extra.Attr;
import me.redot.succinct.internal.maker.TypeReference;
import me.redot.succinct.internal.replacer.*;
import me.redot.succinct.internal.util.JdkReflectionUtil;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.*;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public class ProcessorHelper implements SuccinctHelper {

    public void removeInnerElement(Element element) {
        Element outer = element.getEnclosingElement();
        JCTree innerTree = context.javacTrees().getTree(element);
        JCTree.JCClassDecl outerTree = (JCTree.JCClassDecl) context.javacTrees().getTree(outer);
        outerTree.defs = this.remove(outerTree.defs, innerTree);
    }

    public void copyImports(Element from, Element to) {
        JCTree.JCCompilationUnit fromUnit = this.getCompUnit(from);
        JCTree.JCCompilationUnit toUnit = this.getCompUnit(to);

        Map<String, String> nameMap = new HashMap<>();
        ListBuffer<JCTree.JCImport> toAdd = new ListBuffer<>();

        for (JCTree.JCImport imp : toUnit.getImports()) {
            String qualifiedName = JdkReflectionUtil.getQualid(imp).toString();
            String simpleName = this.getSimpleName(qualifiedName);
            nameMap.put(simpleName, qualifiedName);
        }

        for (JCTree.JCImport imp : fromUnit.getImports()) {
            String qualifiedName = JdkReflectionUtil.getQualid(imp).toString();
            String simpleName = this.getSimpleName(qualifiedName);

            if (nameMap.containsKey(simpleName)) {
                String existingQualified = nameMap.get(simpleName);

                if (!existingQualified.equals(qualifiedName)) {
                    context.messager().printMessage(Diagnostic.Kind.WARNING,
                            "Import conflict: " + simpleName + " already imported from "
                                    + existingQualified + ", cannot also import from " + qualifiedName
                                    + ". Skipping this import."
                    );
                }
                continue;
            }

            toAdd.add(imp);
            nameMap.put(simpleName, qualifiedName);
        }

        ListBuffer<JCTree> newDefs = new ListBuffer<>();

        if (toUnit.getPackage() != null) {
            newDefs.add(toUnit.getPackage());
        }

        newDefs.addAll(toUnit.getImports());
        newDefs.addAll(toAdd);

        for (JCTree def : toUnit.defs) {
            if (!(def instanceof JCTree.JCImport) && def != toUnit.getPackage()) {
                newDefs.add(def);
            }
        }

        toUnit.defs = newDefs.toList();
    }

    public JCTree.JCCompilationUnit getCompUnit(Element element) {
        return (JCTree.JCCompilationUnit) context.javacTrees().getPath(element).getCompilationUnit();
    }

    public String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    public JCTree.JCBlock inlineAnnotationFields(JCTree.JCBlock body, AnnotationMirror annotation) {
        if (body == null) return null;
        AnnotationValueReplacer replacer = new AnnotationValueReplacer(annotation);
        this.copy(body).accept(replacer);
        return (JCTree.JCBlock) replacer.getResult();
    }

    public Collection<? extends AnnotationValue> annotationFieldValues(AnnotationMirror mirror) {
        return context.processingEnv().getElementUtils().getElementValuesWithDefaults(mirror).values();
    }

    public JCTree.JCExpression getAnnotationFieldExpression(AnnotationMirror annotationMirror, int targetIndex) {
        Collection<? extends AnnotationValue> values = this.annotationFieldValues(annotationMirror);
        int index = 0;

        for (AnnotationValue value : values) {
            if (index++ != targetIndex) continue;
            return this.convertAnnotationValue(value);
        }

        throw new UnsupportedOperationException("Target index " + targetIndex + " was out of bounds for this annotation's values! (length " + values.size() + ")");
    }

    public JCTree.JCExpression convertAnnotationValue(AnnotationValue value) {
        Object val = value.getValue();

        // as far as I'm aware, there isn't a better way to do this
        if (val instanceof String) {
            return context.maker().Literal(TypeTag.CLASS, val);
        }
        if (val instanceof Integer) {
            return context.maker().Literal(TypeTag.INT, val);
        }
        if (val instanceof Long) {
            return context.maker().Literal(TypeTag.LONG, val);
        }
        if (val instanceof Float) {
            return context.maker().Literal(TypeTag.FLOAT, val);
        }
        if (val instanceof Double) {
            return context.maker().Literal(TypeTag.DOUBLE, val);
        }
        if (val instanceof Boolean) {
            return context.maker().Literal(TypeTag.BOOLEAN, ((Boolean) val) ? 1 : 0);
        }
        if (val instanceof Character) {
            return context.maker().Literal(TypeTag.CHAR, val);
        }
        if (val instanceof Byte) {
            return context.maker().Literal(TypeTag.BYTE, val);
        }
        if (val instanceof Short) {
            return context.maker().Literal(TypeTag.SHORT, val);
        }
        if (val instanceof TypeMirror type) {
            return this.createClassLiteral(type.toString());
        }
        if (val instanceof VariableElement enumConst) {
            return this.createEnumReference(enumConst);
        }
        if (val instanceof java.util.List list) {
            return this.createArrayLiteral(list);
        }
        if (val instanceof AnnotationMirror) {
            throw new UnsupportedOperationException("Nested annotations are not supported for inline expression generation.");
        }

        throw new UnsupportedOperationException("Unknown annotation value type: " + val.getClass());
    }

    /// String.class, int.class, etc.
    public JCTree.JCExpression createClassLiteral(String typeName) {
        JCTree.JCExpression typeExpr = this.createTypeExpression(typeName);
        return context.maker().Select(typeExpr, context.names().fromString("class"));
    }

    public JCTree.JCExpression createTypeExpression(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        JCTree.JCExpression expr = context.maker().Ident(context.names().fromString(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            expr = context.maker().Select(expr, context.names().fromString(parts[i]));
        }

        return expr;
    }

    /// EnumClass.ENUM_VALUE
    public JCTree.JCExpression createEnumReference(VariableElement enumConst) {
        String enumClassName = enumConst.getEnclosingElement().toString();
        String enumConstName = enumConst.getSimpleName().toString();
        JCTree.JCExpression classExpr = createTypeExpression(enumClassName);

        return context.maker().Select(classExpr, context.names().fromString(enumConstName));
    }

    public JCTree.JCExpression createArrayLiteral(java.util.List<? extends AnnotationValue> values) {
        ListBuffer<JCTree.JCExpression> expressions = new ListBuffer<>();

        for (AnnotationValue value : values) {
            expressions.add(this.convertAnnotationValue(value));
        }

        return context.maker().NewArray(null, List.nil(), expressions.toList());
    }

    public ParamTransformResult transformParameterNames(List<JCTree.JCVariableDecl> params, JCTree.JCBlock body, String fieldName) {
        if (params.isEmpty()) {
            return new ParamTransformResult(params, body);
        }

        Map<String, String> nameReplacements = new HashMap<>();
        ListBuffer<JCTree.JCVariableDecl> transformed = new ListBuffer<>();

        for (JCTree.JCVariableDecl param : params) {
            String originalName = param.name.toString();
            String newName = parseNameTemplate(originalName, fieldName);

            if (!originalName.equals(newName)) {
                nameReplacements.put(originalName, newName);
            }

            JCTree.JCVariableDecl newParam = context.maker().VarDef(
                    param.mods,
                    context.names().fromString(newName),
                    param.vartype,
                    param.init
            );

            transformed.add(newParam);
        }

        JCTree.JCBlock transformedBody = body;
        if (!nameReplacements.isEmpty()) {
            transformedBody = this.replaceParameterReferences(body, nameReplacements);
        }

        return new ParamTransformResult(transformed.toList(), transformedBody);
    }

    public Set<Attr> getAttributes(AnnotationMirror mirror) {
        Collection<? extends AnnotationValue> values = this.annotationFieldValues(mirror);
        Set<Attr> attributes = new HashSet<>();

        for (AnnotationValue value : values) {
            if (!(value.getValue() instanceof List<?> list)) continue;
            for (Object object : list) {
                if (object instanceof com.sun.tools.javac.code.Attribute.Enum enumAttribute) {
                    String name = enumAttribute.value.name.toString();
                    Attr attr = Attr.resolve(name);
                    if (attr != null) attributes.add(attr);
                }
            }
        }

        return attributes;
    }

    public JCTree.JCBlock replaceParameterReferences(JCTree.JCBlock body, Map<String, String> replacements) {
        if (body == null) return null;
        ParamNameReplacer replacer = new ParamNameReplacer(replacements);
        JCTree.JCBlock copied = this.copy(body);
        copied.accept(replacer);
        return (JCTree.JCBlock) replacer.getResult();
    }

    public <T extends JCTree> List<T> remove(List<T> annotations, T toRemove) {
        ListBuffer<T> filtered = new ListBuffer<>();

        for (T annotation : annotations) {
            if (!annotation.equals(toRemove)) {
                filtered.add(annotation);
            }
        }

        return filtered.toList();
    }

    public void removeAnnotation(Element annotated, TypeElement annotation) {
        JCTree.JCAnnotation jcAnnotation = this.getJCAnnotation(annotated, annotation);
        JCTree tree = context.javacTrees().getTree(annotated);

        if (tree instanceof JCTree.JCVariableDecl field) {
            field.mods.annotations = this.remove(field.mods.annotations, jcAnnotation);
        } else if (tree instanceof JCTree.JCMethodDecl method) {
            method.mods.annotations = this.remove(method.mods.annotations, jcAnnotation);
        } else if (tree instanceof JCTree.JCClassDecl clazz) {
            clazz.mods.annotations = this.remove(clazz.mods.annotations, jcAnnotation);
        }
    }

    public JCTree.JCAnnotation getJCAnnotation(Element annotated, TypeElement annotation) {
        AnnotationMirror mirror = this.getAnnotation(annotated, annotation);
        JCTree tree = context.javacTrees().getTree(annotated, mirror);

        if (tree instanceof JCTree.JCMethodDecl method) {
            return this.getJCAnnotation(method.mods.annotations, annotation);
        } else if (tree instanceof JCTree.JCVariableDecl field) {
            return this.getJCAnnotation(field.mods.annotations, annotation);
        } else if (tree instanceof JCTree.JCClassDecl clazz) {
            return this.getJCAnnotation(clazz.mods.annotations, annotation);
        } else if (tree instanceof JCTree.JCAnnotation jcAnnotation) {
            return jcAnnotation;
        }

        return null;
    }

    public JCTree.JCAnnotation getJCAnnotation(List<JCTree.JCAnnotation> annotations, Class<? extends Annotation> annotationClass) {
        return this.getJCAnnotation(annotations, this.typeFor(annotationClass));
    }

    public JCTree.JCAnnotation getJCAnnotation(List<JCTree.JCAnnotation> annotations, TypeElement annotationType) {
        String simpleName = annotationType.getSimpleName().toString();

        for (JCTree.JCAnnotation annotation : annotations) {
            if (!annotation.type.toString().endsWith(simpleName)) {
                return annotation;
            }
        }

        return null;
    }

    public List<JCTree.JCAnnotation> removeSuccinctApiAnnotations(List<JCTree.JCAnnotation> annotations) {
        ListBuffer<JCTree.JCAnnotation> filtered = new ListBuffer<>();

        for (JCTree.JCAnnotation annotation : annotations) {
            if (!annotation.type.toString().startsWith("me.redot.succinct.api")) {
                filtered.add(annotation);
            }
        }

        return filtered.toList();
    }

    public List<JCTree.JCAnnotation> removeAnnotation(List<JCTree.JCAnnotation> annotations, Class<? extends Annotation> annotationClass) {
        ListBuffer<JCTree.JCAnnotation> filtered = new ListBuffer<>();
        String simpleName = annotationClass.getSimpleName();

        for (JCTree.JCAnnotation annotation : annotations) {
            if (!annotation.type.toString().endsWith(simpleName)) {
                filtered.add(annotation);
            }
        }

        return filtered.toList();
    }

    public boolean isAnnotated(JCTree.JCModifiers mods, Class<? extends Annotation> annotationClass) {
        String simpleName = annotationClass.getSimpleName();
        return mods.annotations.stream().anyMatch(a -> a.type.toString().endsWith(simpleName));
    }

    public JCTree.JCAnnotation getAnnotation(JCTree.JCModifiers mods, Class<? extends Annotation> annotationClass) {
        String simpleName = annotationClass.getSimpleName();
        return mods.annotations.stream().filter(a -> a.type.toString().endsWith(simpleName)).findFirst().orElse(null);
    }

    public JCTree.JCExpression replaceContext(JCTree.JCExpression expression, String fieldName) {
        if (expression == null) return null;
        ContextReplacer replacer = new ContextReplacer(fieldName);
        this.copy(expression).accept(replacer);
        return (JCTree.JCExpression) replacer.getResult();
    }

    public JCTree.JCBlock generateOriginalMethodCalls(JCTree.JCBlock toModify, JCTree.JCBlock original) {
        if (toModify == null) return null;
        GeneratedBodyReplacer replacer = new GeneratedBodyReplacer(original);
        this.copy(toModify).accept(replacer);
        return (JCTree.JCBlock) replacer.getResult();
    }

    public JCTree.JCBlock replaceContext(JCTree.JCBlock block, String fieldName) {
        if (block == null) return null;
        ContextReplacer replacer = new ContextReplacer(fieldName);
        this.copy(block).accept(replacer);
        return (JCTree.JCBlock) replacer.getResult();
    }

    public JCTree copy(JCTree tree) {
        return new TreeCopier<>(context.maker()).copy(tree);
    }

    public JCTree.JCBlock copy(JCTree.JCBlock statement) {
        return new TreeCopier<>(context.maker()).copy(statement);
    }

    public JCTree.JCStatement copy(JCTree.JCStatement statement) {
        return new TreeCopier<>(context.maker()).copy(statement);
    }

    public JCTree.JCExpression copy(JCTree.JCExpression expression) {
        return new TreeCopier<>(context.maker()).copy(expression);
    }

    public long compileModifiers(Set<Modifier> modifiers) {
        long flags = 0;

        for (Modifier mod : modifiers) {
            flags |= switch (mod) {
                case PUBLIC -> Flags.PUBLIC;
                case PRIVATE -> Flags.PRIVATE;
                case PROTECTED -> Flags.PROTECTED;
                case ABSTRACT -> Flags.ABSTRACT;
                case DEFAULT -> Flags.DEFAULT;
                case STATIC -> Flags.STATIC;
                case SEALED -> Flags.SEALED;
                case NON_SEALED -> Flags.NON_SEALED;
                case FINAL -> Flags.FINAL;
                case TRANSIENT -> Flags.TRANSIENT;
                case VOLATILE -> Flags.VOLATILE;
                case SYNCHRONIZED -> Flags.SYNCHRONIZED;
                case NATIVE -> Flags.NATIVE;
                case STRICTFP -> Flags.STRICTFP;
            };
        }

        return flags;
    }

    public TypeReference extractReturnType(ExecutableElement element) {
        return this.extractTypeFromString(element.getReturnType().toString());
    }

    public TypeReference extractTypeReference(Element element) {
        if (element instanceof ExecutableElement executableElement) {
            return this.extractReturnType(executableElement);
        }
        return this.extractTypeFromString(element.asType().toString());
    }

    public TypeReference extractTypeFromString(String typeString) {
        return switch (typeString) {
            case "boolean" -> TypeReference.primitive(boolean.class);
            case "byte" -> TypeReference.primitive(byte.class);
            case "short" -> TypeReference.primitive(short.class);
            case "int" -> TypeReference.primitive(int.class);
            case "long" -> TypeReference.primitive(long.class);
            case "char" -> TypeReference.primitive(char.class);
            case "float" -> TypeReference.primitive(float.class);
            case "double" -> TypeReference.primitive(double.class);
            case "void" -> TypeReference.primitive(void.class);
            default -> TypeReference.object(typeString);
        };
    }

    public String parseNameTemplate(String template, String annotatedName) {
        String capitalized = Character.toUpperCase(annotatedName.charAt(0)) + annotatedName.substring(1);
        return template
                .replace("$name$", annotatedName)
                .replace("$Name$", capitalized)
                .replace("$NAME$", annotatedName.toUpperCase());
    }

    // I really wish javac used some sort of inheritance system.

    public JCTree.JCBlock extractMethodBody(ExecutableElement method) {
        JCTree.JCMethodDecl methodDecl = context.javacTrees().getTree(method);
        return methodDecl != null ? methodDecl.body : null;
    }

    public List<JCTree.JCExpression> extractThrownExceptions(ExecutableElement method) {
        JCTree.JCMethodDecl methodDecl = context.javacTrees().getTree(method);
        return methodDecl != null ? methodDecl.thrown : null;
    }

    public List<JCTree.JCVariableDecl> extractMethodParams(ExecutableElement method) {
        JCTree.JCMethodDecl methodDecl = context.javacTrees().getTree(method);
        return methodDecl != null ? methodDecl.params : null;
    }

    public List<JCTree.JCAnnotation> extractAnnotations(ExecutableElement method) {
        JCTree.JCMethodDecl methodDecl = context.javacTrees().getTree(method);
        return methodDecl != null ? methodDecl.mods.annotations : null;
    }

    public List<JCTree.JCAnnotation> extractAnnotations(TypeElement clazz) {
        JCTree.JCClassDecl classDecl = context.javacTrees().getTree(clazz);
        return classDecl != null ? classDecl.mods.annotations : null;
    }

    public List<JCTree.JCAnnotation> extractAnnotations(VariableElement field) {
        JCTree.JCVariableDecl varDecl = (JCTree.JCVariableDecl) context.javacTrees().getTree(field);
        return varDecl != null ? varDecl.mods.annotations : null;
    }

    public List<JCTree.JCAnnotation> extractAnnotations(Element annotated) {
        if (annotated instanceof VariableElement field) {
            return this.extractAnnotations(field);
        } else if (annotated instanceof ExecutableElement method) {
            return this.extractAnnotations(method);
        } else if (annotated instanceof TypeElement clazz) {
            return this.extractAnnotations(clazz);
        }
        return List.nil();
    }

    public JCTree.JCExpression extractFieldInitializer(VariableElement field) {
        JCTree.JCVariableDecl fieldTree = (JCTree.JCVariableDecl) context.javacTrees().getTree(field);
        return fieldTree != null ? fieldTree.init : null;
    }

}
