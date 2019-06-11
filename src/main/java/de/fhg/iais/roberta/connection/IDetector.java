package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.usb.Robot;

import java.util.List;

@FunctionalInterface
public interface IDetector {
    /**
     * Checks whether robots targeted by this detector is available.
     * @return a list of the available robots
     */
    List<Robot> detectRobots();
}
