package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.util.IOraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RobotDetectorHelper implements IOraListener<Robot> {
    private static final Logger LOG = LoggerFactory.getLogger(RobotDetectorHelper.class);

    private Robot selectedRobot = Robot.NONE;

    private final Collection<IDetector> detectors;

    RobotDetectorHelper(Collection<IDetector> detectors) {
        this.detectors = new ArrayList<>(detectors);
    }

    List<Robot> getDetectedRobots() {
        List<Robot> detectedRobots = new ArrayList<>();
        for ( IDetector detector : this.detectors ) {
            LOG.info("Looking for robot: {}", detector.getRobot());
            if (detector.detectRobot()) {
                detectedRobots.add(detector.getRobot());
            }
        }
        return detectedRobots;
    }

    Robot getSelectedRobot() {
        return this.selectedRobot;
    }

    void reset() {
        this.selectedRobot = Robot.NONE;
    }

    @Override
    public void update(Robot object) {
        this.selectedRobot = object;
    }
}
