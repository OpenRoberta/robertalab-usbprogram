package de.fhg.iais.roberta.util;

public interface ORAListenable<T> {
    void registerListener(ORAListener<T> listener);
    void unregisterListener(ORAListener<T> listener);
    void fire(T object);
}
