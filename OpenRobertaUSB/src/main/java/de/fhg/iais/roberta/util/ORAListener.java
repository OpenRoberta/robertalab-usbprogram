package de.fhg.iais.roberta.util;

@FunctionalInterface
public interface ORAListener<T> {
    void update(T object);
}
