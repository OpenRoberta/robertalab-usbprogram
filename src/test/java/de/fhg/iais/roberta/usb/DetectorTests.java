package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.connection.arduino.ArduinoDetector;
import de.fhg.iais.roberta.connection.ev3.Ev3Detector;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.testUtils.TestListenable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectorTests {

    @Test
    void getRobot_ShouldReturnRobotType_WhenRun() {
        IDetector arduinoDetector = new ArduinoDetector();
        IDetector ev3Detector = new Ev3Detector();

        assertThat(arduinoDetector.getRobot(), is(Robot.ARDUINO));
        assertThat(ev3Detector.getRobot(), is(Robot.EV3));
    }

    @Test
    void getDetectedRobots_ShouldOnlyReturnDetectedRobots_WhenRun() {
        IDetector arduDDetector = new TestArduinoDetectedDetector();
        IDetector arduNDDetector = new TestArduinoNotDetectedDetector();
        IDetector ev3DDetector = new TestEv3DetectedDetector();
        IDetector ev3NDDetector = new TestEv3NotDetectedDetector();

        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduDDetector, ev3DDetector));
        List<Robot> detectedRobots = robotDetectorHelper.getDetectedRobots();

        assertThat(detectedRobots, contains(Robot.ARDUINO, Robot.EV3));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduDDetector, ev3NDDetector));
        detectedRobots = robotDetectorHelper.getDetectedRobots();

        assertThat(detectedRobots, contains(Robot.ARDUINO));
        assertThat(detectedRobots, not(contains(Robot.EV3)));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduNDDetector, ev3DDetector));
        detectedRobots = robotDetectorHelper.getDetectedRobots();

        assertThat(detectedRobots, not(contains(Robot.ARDUINO)));
        assertThat(detectedRobots, contains(Robot.EV3));

        robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(arduNDDetector, ev3NDDetector));
        detectedRobots = robotDetectorHelper.getDetectedRobots();

        assertThat(detectedRobots, empty());
    }

    @Test
    void getSelectedRobot_ShouldThrowIllegalState_WhenSelectedRobotIsNotRegistered() {
        IOraListener<Robot> robotDetectorHelper = new RobotDetectorHelper(Collections.emptyList());
        IOraListenable<Robot> robotTestListenable = new TestListenable<>();

        robotTestListenable.registerListener(robotDetectorHelper);

        assertThrows(IllegalStateException.class, () -> robotTestListenable.fire(Robot.EV3));
    }

    @Test
    void getSelectedRobot_ShouldReturnSelectedRobot_WhenRobotIsSelected() {
        RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(new TestArduinoDetectedDetector(), new TestEv3DetectedDetector()));
        IOraListenable<Robot> robotTestListenable = new TestListenable<>();

        robotTestListenable.registerListener(robotDetectorHelper);

        robotTestListenable.fire(Robot.EV3);
        Robot selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, is(Robot.EV3));

        robotTestListenable.fire(Robot.ARDUINO);
        selectedRobot = robotDetectorHelper.getSelectedRobot();

        assertThat(selectedRobot, is(Robot.ARDUINO));
    }

    private static class TestEv3DetectedDetector implements IDetector {
        @Override
        public boolean detectRobot() {
            return true;
        }

        @Override
        public Robot getRobot() {
            return Robot.EV3;
        }
    }

    private static class TestEv3NotDetectedDetector implements IDetector {
        @Override
        public boolean detectRobot() {
            return false;
        }

        @Override
        public Robot getRobot() {
            return Robot.EV3;
        }
    }

    private static class TestArduinoDetectedDetector implements IDetector {
        @Override
        public boolean detectRobot() {
            return true;
        }

        @Override
        public Robot getRobot() {
            return Robot.ARDUINO;
        }
    }
    private static class TestArduinoNotDetectedDetector implements IDetector {
        @Override
        public boolean detectRobot() {
            return false;
        }

        @Override
        public Robot getRobot() {
            return Robot.ARDUINO;
        }
    }
}
