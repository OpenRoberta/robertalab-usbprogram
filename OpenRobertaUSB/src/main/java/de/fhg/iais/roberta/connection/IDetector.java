package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.usb.Robot;

public interface IDetector {
    /**
     * Checks whether a robot targeted by this detector is available.
     * @return whether a robot is available
     */
    boolean detectRobot();

    /**
     * Returns the kind of robot this detector is looking for.
     * @return the target robot of this detector
     */
    Robot getRobot();
}
