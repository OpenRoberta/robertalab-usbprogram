package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoUsbConnector;
import de.fhg.iais.roberta.connection.ev3.Ev3UsbConnector;
import de.fhg.iais.roberta.ui.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class UsbProgram {
    private static final Logger LOG = LoggerFactory.getLogger(UsbProgram.class);

    private static final String MESSAGES_BUNDLE = "messages";
    private static final List<IConnector> connectorList = Collections.unmodifiableList(Arrays.asList(new ArduinoUsbConnector(), new Ev3UsbConnector()));

    private final MainController controller;

    public UsbProgram() {
        ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE, Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());
        this.controller = new MainController(messages);
    }

    public void run() {
        LOG.debug("Entering run method!");
        while ( !Thread.currentThread().isInterrupted() ) {
                LOG.debug("Waiting for robot search results!");
                IConnector selectedRobot = new RobotSearchTask(connectorList, this.controller::setConnectorList, this.controller).search();
                LOG.debug("Result {}", selectedRobot);

                this.controller.setConnector(selectedRobot);

                LOG.info("Connector finished!");
        }
    }
}
