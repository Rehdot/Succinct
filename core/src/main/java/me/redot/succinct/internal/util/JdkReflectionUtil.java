package me.redot.succinct.internal.util;

import com.sun.tools.javac.tree.JCTree;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/// A utility to make cross-jdk reflection easier.
public final class JdkReflectionUtil {

    /// Credit to Lombok for this solution.
    /// Different versions of the JDK use different structures for this field.
    private static final Field JCIMPORT_QUALID = tryGetField(JCTree.JCImport.class, "qualid");

    public static Field tryGetField(Class<?> clazz, String name) {
        try {
            return getField(clazz, name);
        } catch (Exception e) {
            return null;
        }
    }

    public static Field getField(Class<?> c, String fName) throws NoSuchFieldException {
        Field f = null;
        Class<?> d = c;
        while (d != null) {
            try {
                f = d.getDeclaredField(fName);
                break;
            } catch (NoSuchFieldException ignored) {}
            d = d.getSuperclass();
        }
        if (f == null) throw new NoSuchFieldException(c.getName() + " :: " + fName);
        f.setAccessible(true);
        return f;
    }

    public static Method tryGetMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return getMethod(clazz, name, parameterTypes);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static Method getMethod(Class<?> c, String mName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method m = null;
        Class<?> oc = c;
        while (c != null) {
            try {
                m = c.getDeclaredMethod(mName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {}
            c = c.getSuperclass();
        }

        if (m == null) throw new NoSuchMethodException(oc.getName() + " :: " + mName + "(args)");
        m.setAccessible(true);
        return m;
    }

    public static JCTree getQualid(JCTree.JCImport tree) {
        try {
            return (JCTree) JCIMPORT_QUALID.get(tree);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
