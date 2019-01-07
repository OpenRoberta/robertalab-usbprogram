package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.ui.UIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class RobotSearchTask implements Callable<IConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotSearchTask.class);

    private final List<IConnector> connectorList;
    private final UIController controller;

    public RobotSearchTask(List<IConnector> connectorList, UIController controller) {
        this.connectorList = Collections.unmodifiableList(connectorList);
        this.controller = controller;
    }

    private boolean updateFoundRobots(Collection<IConnector> foundRobots) {
        boolean updated = false;
        for ( IConnector connector : this.connectorList ) {
            LOG.debug("Looking for {}", connector.getBrickName());
            if (connector.findRobot()) {
                if (!foundRobots.contains(connector)) {
                    foundRobots.add(connector);
                    updated = true;
                }
            } else {
                if (foundRobots.contains(connector)) {
                    foundRobots.remove(connector);
                    updated = true;
                }
            }
        }
        return updated;
    }

    @Override
    public IConnector call() {
        List<IConnector> foundRobots = new ArrayList<>();

        while (true) {
            boolean wasListUpdated = updateFoundRobots(foundRobots);

            if ( foundRobots.isEmpty() ) {
                LOG.info("No robot connected!");
            } else if (foundRobots.size() == 1) {
                LOG.info("Only {} available.", foundRobots.get(0).getBrickName());
                return foundRobots.get(0);
            } else {
                if (wasListUpdated) {
                    LOG.info("list was updated!");
                    for (IConnector robot : foundRobots) {
                        LOG.info("{} available.", robot.getBrickName());
                    }
                    this.controller.setConnectorMap(foundRobots);
                }

                IConnector selectedRobot = this.controller.getSelectedRobot();
                if (selectedRobot != null) {
                    LOG.info(selectedRobot.toString());
                    return selectedRobot;
                }
            }
        }
    }
}
