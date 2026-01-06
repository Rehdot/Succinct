package me.redot.succinct.internal.model;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

public class ProcessorContext {

    private final ProcessingEnvironment processingEnvironment;
    private final Messager messager;
    private final JavacTrees javacTrees;
    private final TreeMaker maker;
    private final Names names;

    public ProcessorContext(ProcessingEnvironment processingEnvironment,
                            Messager messager, JavacTrees javacTrees,
                            TreeMaker maker, Names names) {
        this.processingEnvironment = processingEnvironment;
        this.messager = messager;
        this.javacTrees = javacTrees;
        this.maker = maker;
        this.names = names;
    }

    public JavacTrees javacTrees() {
        return this.javacTrees;
    }

    public Messager messager() {
        return this.messager;
    }

    public Names names() {
        return this.names;
    }

    public ProcessingEnvironment processingEnv() {
        return this.processingEnvironment;
    }

    public TreeMaker maker() {
        return this.maker;
    }

}
