package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector.State;

import java.util.Observable;
import java.util.Observer;

class ConnectorObserver implements Observer {
    private final MainController mainController;

    ConnectorObserver(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void update(Observable observable, Object o) {
        State state = (State) o;
        this.mainController.setState(state);
    }
}
