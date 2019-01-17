package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

class RobotSearchObserver implements Observer {
    private final MainController mainController;

    RobotSearchObserver(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void update(Observable observable, Object o) {
        this.mainController.setConnectorList((List<IConnector>) o);
    }
}
