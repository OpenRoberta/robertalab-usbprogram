package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.util.Pair;

public interface IController {
    /**
     * Sets the connector that provides access to the data necessary for this controller.
     * Should also register the setState method as a listener to the connector.
     *
     * @param connector the connector that should be handled by this controller
     */
    void setConnector(IConnector connector);

    /**
     * Used as a listener for the connector state.
     * The controller should handle the states accordingly.
     *
     * @param statePair a pair of the state of the connector and an additional message
     */
    void setState(Pair<State, String> statePair);
}
