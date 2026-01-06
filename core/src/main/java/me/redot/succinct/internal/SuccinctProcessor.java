package me.redot.succinct.internal;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import me.redot.succinct.api.annotation.Authored;
import me.redot.succinct.api.annotation.Modified;
import me.redot.succinct.api.annotation.extra.Persistent;
import me.redot.succinct.api.author.Author;
import me.redot.succinct.internal.maker.FieldMaker;
import me.redot.succinct.internal.maker.MethodMaker;
import me.redot.succinct.internal.maker.MethodModifier;
import me.redot.succinct.internal.model.ProcessorContext;
import me.redot.succinct.internal.model.ProcessorHelper;
import sun.misc.Unsafe;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class SuccinctProcessor extends AbstractProcessor {

    public static ProcessorContext context;
    public static ProcessorHelper helper;

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        this.messager = env.getMessager();
        this.messager.printMessage(Diagnostic.Kind.NOTE, "Starting Succinct processor...");

        this.addOpens();

        try {
            Context ctx = ((JavacProcessingEnvironment) env).getContext();

            helper = new ProcessorHelper();
            context = new ProcessorContext(
                    this.processingEnv,
                    this.messager,
                    JavacTrees.instance(env),
                    TreeMaker.instance(ctx),
                    Names.instance(ctx)
            );

            context.messager().printMessage(Diagnostic.Kind.NOTE, "Succinct processor initialized!");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (!helper.isSuccinctBased(annotation)) {
                continue;
            }

            context.messager().printMessage(Diagnostic.Kind.NOTE, "Processing Succinct-based annotation @" + annotation.getSimpleName());

            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                switch (element.getKind()) {
                    case FIELD -> this.processField((VariableElement) element, annotation);
                    case METHOD -> this.processMethod((ExecutableElement) element, annotation);
                    case CLASS -> this.processClass((TypeElement) element, annotation);
                    default -> context.messager().printMessage(Diagnostic.Kind.WARNING, "Found no suitable generator for element: " + element.getSimpleName());
                }
            }

            for (Element enclosed : annotation.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.CLASS) continue;
                TypeElement element = (TypeElement) enclosed;

                if (helper.typeExtends(element, Author.class) && !helper.isAnnotated(element, Persistent.class)) {
                    helper.removeInnerElement(element);
                }
            }
        }

        return false;
    }

    private void processClass(TypeElement clazz, TypeElement annotation) {
        JCTree.JCClassDecl classDecl = context.javacTrees().getTree(clazz);
        this.processElement(clazz, annotation, classDecl);
    }

    private void processMethod(ExecutableElement method, TypeElement annotation) {
        TypeElement classElement = (TypeElement) method.getEnclosingElement();
        JCTree.JCClassDecl classDecl = context.javacTrees().getTree(classElement);
        List<TypeElement> authors = helper.getAuthorClasses(annotation);
        boolean changed = false;

        for (TypeElement author : authors) {
            for (Element enclosed : author.getEnclosedElements()) {
                if (helper.isAnnotated(enclosed, Authored.class)) {
                    if (enclosed instanceof VariableElement authorField) {
                        new FieldMaker(method, authorField).make().appendTo(classDecl);
                    } else if (enclosed instanceof ExecutableElement authorMethod) {
                        new MethodMaker(method, authorMethod, annotation).make().appendTo(classDecl);
                    }
                    changed = true;
                } else if (helper.isAnnotated(enclosed, Modified.class)
                        && enclosed instanceof ExecutableElement authorMethod) {
                    new MethodModifier(method, authorMethod, annotation).modify();
                    changed = true;
                }
            }
        }

        if (changed) {
            helper.copyImports(annotation, method);
            helper.removeAnnotation(method, annotation);
        }
    }

    private void processField(VariableElement field, TypeElement annotation) {
        TypeElement classElement = (TypeElement) field.getEnclosingElement();
        JCTree.JCClassDecl classDecl = context.javacTrees().getTree(classElement);
        this.processElement(field, annotation, classDecl);
    }

    private void processElement(Element annotated, TypeElement annotation, JCTree.JCClassDecl classDecl) {
        List<TypeElement> authors = helper.getAuthorClasses(annotation);
        boolean changed = false;

        for (TypeElement authorClass : authors) {
            for (Element enclosed : authorClass.getEnclosedElements()) {
                if (!helper.isAnnotated(enclosed, Authored.class)) continue;

                if (enclosed instanceof VariableElement authorField) {
                    new FieldMaker(annotated, authorField).make().appendTo(classDecl);
                } else if (enclosed instanceof ExecutableElement authorMethod) {
                    new MethodMaker(annotated, authorMethod, annotation).make().appendTo(classDecl);
                }

                changed = true;
            }
        }

        if (changed) {
            helper.copyImports(annotation, annotated);
            helper.removeAnnotation(annotated, annotation);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    // Full credit to the geniuses at Lombok for the following, this
    // hack allows us to use javac within the processor without
    // begging users to put --add-opens in their jvm args.

    private void addOpens() {
        try {
            Class<?> cModule = Class.forName("java.lang.Module");
            Unsafe unsafe = this.getUnsafe();
            Object jdkCompilerModule = this.getJdkCompilerModule();
            Object ownModule = this.getOwnModule();

            String[] allPkgs = {
                    "com.sun.tools.javac.api",
                    "com.sun.tools.javac.code",
                    "com.sun.tools.javac.comp",
                    "com.sun.tools.javac.processing",
                    "com.sun.tools.javac.tree",
                    "com.sun.tools.javac.util",
            };

            Method implAddOpens = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            long firstFieldOffset = this.getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(implAddOpens, firstFieldOffset, true);

            for (String pkg : allPkgs) {
                implAddOpens.invoke(jdkCompilerModule, pkg, ownModule);
            }
        } catch (Exception e) {
            this.messager.printMessage(Diagnostic.Kind.WARNING, "Failed to open javac modules: " + e.getMessage());
        }
    }

    private Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getJdkCompilerModule() {
        try {
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);
            Class<?> cOptional = Class.forName("java.util.Optional");
            Method mFindModule = cModuleLayer.getDeclaredMethod("findModule", String.class);
            Object oCompilerO = mFindModule.invoke(bootLayer, "jdk.compiler");
            return cOptional.getDeclaredMethod("get").invoke(oCompilerO);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getOwnModule() {
        try {
            Method getModule = Class.class.getDeclaredMethod("getModule");
            return getModule.invoke(this.getClass());
        } catch (Exception e) {
            return null;
        }
    }

    static class Parent {
        boolean first;
    }

    private long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}