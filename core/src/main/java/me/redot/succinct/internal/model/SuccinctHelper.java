package me.redot.succinct.internal.model;

import me.redot.succinct.api.author.Author;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static me.redot.succinct.internal.SuccinctProcessor.context;

public interface SuccinctHelper {

    default List<TypeElement> getAuthorClasses(TypeElement annotation) {
        List<TypeElement> classes = new ArrayList<>();

        for (Element clazz : annotation.getEnclosedElements()) {
            if (clazz.getKind() != ElementKind.CLASS) continue;

            TypeElement typeClass = (TypeElement) clazz;
            if (this.typeExtends(typeClass, Author.class)) {
                classes.add(typeClass);
            }
        }

        return classes;
    }

    default boolean isSuccinctBased(TypeElement annotation) {
        for (Element element : annotation.getEnclosedElements()) {
            if (element.getKind() != ElementKind.CLASS) continue;

            TypeElement nested = (TypeElement) element;
            if (this.typeExtends(nested, Author.class)) {
                return true;
            }
        }

        return false;
    }

    default boolean isAnnotated(Element element, Class<? extends Annotation> annotation) {
        return this.isAnnotated(element, this.typeFor(annotation));
    }

    default AnnotationMirror getAnnotation(Element element, Class<? extends Annotation> annotation) {
        return this.getAnnotation(element, this.typeFor(annotation));
    }

    default AnnotationMirror getAnnotation(Element element, TypeElement annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (this.typeEquals(mirror.getAnnotationType(), annotation.asType())) {
                return mirror;
            }
        }
        return null;
    }

    default boolean isAnnotated(Element element, TypeElement annotation) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (this.typeEquals(mirror.getAnnotationType(), annotation.asType())) {
                return true;
            }
        }
        return false;
    }

    default TypeElement typeFor(Class<?> clazz) {
        return this.typeFor(clazz.getName());
    }

    default TypeElement typeFor(String fqcn) {
        return context.processingEnv().getElementUtils().getTypeElement(fqcn);
    }

    default boolean typeEquals(TypeElement t1, TypeElement t2) {
        return this.typeEquals(t1.asType(), t2.asType());
    }

    default boolean typeEquals(TypeMirror t1, TypeMirror t2) {
        return context.processingEnv().getTypeUtils().isSameType(t1, t2);
    }

    default boolean typeExtends(TypeElement type, Class<?> clazz) {
        return this.typeExtends(type, clazz.getName());
    }

    default boolean typeExtends(TypeElement type, String fqcn) {
        TypeMirror superclass = type.getSuperclass();
        TypeElement target = this.typeFor(fqcn);
        if (target == null) return false;
        return this.typeExtends(superclass, target.asType());
    }

    default boolean typeExtends(TypeMirror type1, TypeMirror type2) {
        return context.processingEnv().getTypeUtils().isAssignable(type1, type2);
    }

}
