package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.arduino.ArduinoConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoDetector;
import de.fhg.iais.roberta.connection.ev3.Ev3Connector;
import de.fhg.iais.roberta.connection.ev3.Ev3Detector;
import de.fhg.iais.roberta.ui.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

class UsbProgram {
    private static final Logger LOG = LoggerFactory.getLogger(UsbProgram.class);

    private static final String MESSAGES_BUNDLE = "messages";
    private static final long TIMEOUT = 1000L;

    private final MainController controller;

    private final Ev3Detector ev3Detector = new Ev3Detector();
    private final ArduinoDetector arduinoDetector = new ArduinoDetector();
    private final RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(this.ev3Detector, this.arduinoDetector));

    UsbProgram() {
        ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE, Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());

        this.controller = new MainController(messages);
        this.controller.registerListener(this.robotDetectorHelper); // register the detector helper as a listener to selection events of the controller
    }

    void run() {
        // Main loop, repeats until the program is closed
        while ( !Thread.currentThread().isInterrupted() ) {

            // Check which robots are available
            this.robotDetectorHelper.reset();
            Robot selectedRobot = Robot.NONE;
            while ( selectedRobot == Robot.NONE ) {
                List<Robot> detectedRobots = this.robotDetectorHelper.getDetectedRobots();
                // If only one robot is available select that one immediately
                if (detectedRobots.size() == 1) {
                    selectedRobot = detectedRobots.get(0);
                } else if (detectedRobots.size() > 1) {
                    // If there is more than one robot wait until one of the robots was selected
                    this.controller.setRobotList(detectedRobots);
                    selectedRobot = this.robotDetectorHelper.getSelectedRobot();
                }

                // Repeat until a robot is available or one was selected
                try {
                    Thread.sleep(TIMEOUT);
                } catch ( InterruptedException e ) {
                    LOG.error("Thread was interrupted while waiting for a robot selection: {}", e.getMessage());
                }
            }

            // Start the appropriate connector depending on the robot
            switch ( selectedRobot ) {
                case EV3:
                    this.controller.setConnector(new Ev3Connector());
                    break;
                case ARDUINO:
                    Map<Integer, String> errors = this.arduinoDetector.getReadIdFileErrors();
                    if ( !errors.isEmpty() ) {
                        this.controller.showConfigErrorPopup(errors);
                    }
                    this.controller.setConnector(new ArduinoConnector(this.arduinoDetector.getType(), this.arduinoDetector.getPortName()));
                    break;
                default:
                    break;
            }
        }
    }
}
