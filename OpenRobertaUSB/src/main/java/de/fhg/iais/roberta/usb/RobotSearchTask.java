package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class RobotSearchTask implements Callable<IConnector>, IOraListenable<List<IConnector>> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotSearchTask.class);

    private final Collection<IOraListener<List<IConnector>>> listeners = new ArrayList<>();

    private final List<IConnector> connectorList;
    private IConnector selectedRobot = null;

    public RobotSearchTask(List<IConnector> connectorList, IOraListener<List<IConnector>> listener, IOraListenable<IConnector> listenable) {
        this.connectorList = Collections.unmodifiableList(connectorList);

        this.registerListener(listener);
        listenable.registerListener(object -> this.selectedRobot = object);
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
                    fire(foundRobots);
                }

                if ( this.selectedRobot != null ) {
                    LOG.info(this.selectedRobot.toString());
                    return this.selectedRobot;
                }
            }
        }
    }

    @Override
    public void registerListener(IOraListener<List<IConnector>> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<List<IConnector>> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(List<IConnector> object) {
        for ( IOraListener<List<IConnector>> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
