package de.fhg.iais.roberta.ui;

import java.util.Observable;
import java.util.Observer;

class SerialLoggingObserver implements Observer {
    private final SerialMonitorController serialMonitorController;

    SerialLoggingObserver(SerialMonitorController serialMonitorController) {
        this.serialMonitorController = serialMonitorController;
    }

    @Override
    public void update(Observable observable, Object o) {
        this.serialMonitorController.appendSerial((byte[]) o);
    }
}
