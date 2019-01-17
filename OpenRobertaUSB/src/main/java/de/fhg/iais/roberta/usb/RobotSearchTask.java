package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

public class RobotSearchTask extends Observable implements Callable<IConnector>, Observer {
    private static final Logger LOG = LoggerFactory.getLogger(RobotSearchTask.class);

    private final List<IConnector> connectorList;
    private IConnector selectedRobot = null;

    public RobotSearchTask(List<IConnector> connectorList, Observer observer, Observable observable) {
        this.connectorList = Collections.unmodifiableList(connectorList);

        this.addObserver(observer);
        observable.addObserver(this);
    }

    private boolean updateFoundRobots(Collection<IConnector> foundRobots) {
        boolean updated = false;
        for ( IConnector connector : this.connectorList ) {
            LOG.debug("Looking for {}", connector.getBrickName());
            if ( connector.findRobot() ) {
                if ( !foundRobots.contains(connector) ) {
                    foundRobots.add(connector);
                    updated = true;
                }
            } else {
                if ( foundRobots.contains(connector) ) {
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

        while ( true ) {
            boolean wasListUpdated = updateFoundRobots(foundRobots);

            if ( foundRobots.isEmpty() ) {
                LOG.info("No robot connected!");
            } else if ( foundRobots.size() == 1 ) {
                LOG.info("Only {} available.", foundRobots.get(0).getBrickName());
                return foundRobots.get(0);
            } else {
                if ( wasListUpdated ) {
                    LOG.info("list was updated!");
                    for ( IConnector robot : foundRobots ) {
                        LOG.info("{} available.", robot.getBrickName());
                    }
                    setChanged();
                    notifyObservers(foundRobots);
                }

                if ( this.selectedRobot != null ) {
                    LOG.info(this.selectedRobot.toString());
                    return this.selectedRobot;
                }
            }
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        this.selectedRobot = (IConnector) o;
    }
}
