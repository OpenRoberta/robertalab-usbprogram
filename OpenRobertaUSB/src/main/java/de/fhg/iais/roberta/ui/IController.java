package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;

public interface IController {
    /**
     * Sets the connector that provides access to the data necessary for this controller.
     * Should also add any connector listeners that the controller may have to listen to the connector.
     * @param connector the connector that should be handled by this controller
     */
    void setConnector(IConnector connector);

    /**
     * Sets the state of the connector
     * @param state the state of the connector
     */
    void setState(State state);
}
