package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoDetector;
import de.fhg.iais.roberta.connection.ev3.Ev3Connector;
import de.fhg.iais.roberta.connection.ev3.Ev3Detector;
import de.fhg.iais.roberta.ui.main.MainController;
import de.fhg.iais.roberta.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

class UsbProgram {
    private static final Logger LOG = LoggerFactory.getLogger(UsbProgram.class);

    private static final long TIMEOUT = 1000L;
    private static final long HELP_THRESHOLD = 20000L;

    private final MainController controller;

    private final Ev3Detector ev3Detector = new Ev3Detector();
    private final ArduinoDetector arduinoDetector = new ArduinoDetector();
    private final RobotDetectorHelper robotDetectorHelper = new RobotDetectorHelper(Arrays.asList(this.arduinoDetector, this.ev3Detector));

    UsbProgram() {
        ResourceBundle messages = ResourceBundle.getBundle(PropertyHelper.getInstance().getProperty("messagesBundle"), Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());

        this.controller = new MainController(messages);
        this.controller.registerListener(this.robotDetectorHelper); // register the detector helper as a listener to selection events of the controller
    }

    void run() {
        long previousTime = System.currentTimeMillis();
        long helpTimer = 0L;
        boolean showHelp = true;

        Map<Integer, String> errors = this.arduinoDetector.getReadIdFileErrors();
        if ( !errors.isEmpty() ) {
            this.controller.showConfigErrorPopup(errors);
        }

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
                    helpTimer += (System.currentTimeMillis() - previousTime);

                    if ( (helpTimer > HELP_THRESHOLD) && showHelp ) {
                        this.controller.showHelp();
                        showHelp = false;
                    }
                } catch ( InterruptedException e ) {
                    LOG.error("Thread was interrupted while waiting for a robot selection: {}", e.getMessage());
                }

                previousTime = System.currentTimeMillis();
            }

            // Create the appropriate connector depending on the robot
            IConnector connector;
            switch ( selectedRobot ) {
                case EV3:
                    connector = new Ev3Connector();
                    break;
                case ARDUINO:
                    connector = new ArduinoConnector(this.arduinoDetector.getType(), this.arduinoDetector.getPortName());
                    break;
                default:
                    throw new UnsupportedOperationException("Selected robot not supported!");
            }
            this.controller.setConnector(connector);
            connector.run(); // Blocking until the connector is finished

            showHelp = false;
        }
    }
}
