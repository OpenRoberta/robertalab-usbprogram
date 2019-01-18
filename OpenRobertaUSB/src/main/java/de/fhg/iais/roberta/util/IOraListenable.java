package de.fhg.iais.roberta.util;

public interface IOraListenable<T> {
    void registerListener(IOraListener<T> listener);
    void unregisterListener(IOraListener<T> listener);
    void fire(T object);
}
