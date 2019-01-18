package de.fhg.iais.roberta.util;

@FunctionalInterface
public interface IOraListener<T> {
    void update(T object);
}
