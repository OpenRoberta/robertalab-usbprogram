package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;

public interface IController {
    /**
     * Sets the connector that provides access to the data necessary for this controller.
     * Should also add any connector observers that the controller may have to observe the connector.
     * @param connector the connector that should be
     */
    void setConnector(IConnector connector);

    /**
     *
     * @param state
     */
    void setState(State state);
}
