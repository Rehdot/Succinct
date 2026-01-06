package me.redot.succinct.internal.maker;

import me.redot.succinct.internal.element.SuccinctElement;

public interface ElementMaker<T extends SuccinctElement<?>> {

    T make();

}
