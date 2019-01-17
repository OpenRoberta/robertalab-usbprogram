package de.fhg.iais.roberta.ui;

import java.util.Observable;
import java.util.Observer;

class SerialLoggingObserver implements Observer {
    private final MainController mainController;

    SerialLoggingObserver(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void update(Observable observable, Object o) {
        this.mainController.appendSerial((byte[]) o);
    }
}
