package me.redot.succinct.internal.maker;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public class TypeReference {

    private final Class<?> primitiveType;
    private final String qualifiedName;
    private final boolean isPrimitive;

    private TypeReference(Class<?> primitiveType) {
        this.primitiveType = primitiveType;
        this.qualifiedName = null;
        this.isPrimitive = true;
    }

    private TypeReference(String qualifiedName) {
        this.primitiveType = null;
        this.qualifiedName = qualifiedName;
        this.isPrimitive = false;
    }

    public static TypeReference primitive(Class<?> type) {
        return new TypeReference(type);
    }

    public static TypeReference object(Class<?> type) {
        return new TypeReference(type.getName());
    }

    public static TypeReference object(String qualifiedName) {
        return new TypeReference(qualifiedName);
    }

    public JCTree.JCExpression toJCExpression() {
        if (this.isPrimitive) {
            return context.maker().TypeIdent(this.getTypeTag(this.primitiveType));
        }

        String[] parts = this.qualifiedName.split("\\.");
        JCTree.JCExpression expr = context.maker().Ident(context.names().fromString(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            expr = context.maker().Select(expr, context.names().fromString(parts[i]));
        }

        return expr;
    }

    private TypeTag getTypeTag(Class<?> type) {
        if (type == boolean.class) return TypeTag.BOOLEAN;
        if (type == byte.class) return TypeTag.BYTE;
        if (type == short.class) return TypeTag.SHORT;
        if (type == int.class) return TypeTag.INT;
        if (type == long.class) return TypeTag.LONG;
        if (type == char.class) return TypeTag.CHAR;
        if (type == float.class) return TypeTag.FLOAT;
        if (type == double.class) return TypeTag.DOUBLE;
        if (type == void.class) return TypeTag.VOID;
        throw new UnsupportedOperationException("Supplied type was non-primitive.");
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public String getQualifiedName() {
        return isPrimitive ? primitiveType.getName() : qualifiedName;
    }
}