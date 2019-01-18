package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
import de.fhg.iais.roberta.connection.ev3.EV3USBConnector;
import de.fhg.iais.roberta.ui.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class USBProgram {
    private static final Logger LOG = LoggerFactory.getLogger(USBProgram.class);

    private static final int SLEEP_TIME = 1000;
    private static final String MESSAGES_BUNDLE = "messages";
    private static final List<IConnector> connectorList = Collections.unmodifiableList(Arrays.asList(new ArduinoUSBConnector(), new EV3USBConnector()));

    private static boolean connectorShouldStop = false;
    private final MainController controller;

    public USBProgram() {
        ResourceBundle messages = ResourceBundle.getBundle(MESSAGES_BUNDLE, Locale.getDefault());
        LOG.info("Using locale {}", (messages.getLocale().getLanguage().isEmpty()) ? "default en" : messages.getLocale());
        this.controller = new MainController(messages);
    }

    public void run() {
        LOG.debug("Entering run method!");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        while ( !Thread.currentThread().isInterrupted() ) {
            Future<IConnector>
                robotSearchFuture =
                executorService.submit(new RobotSearchTask(connectorList, this.controller::setConnectorList, this.controller));

            try {
                LOG.debug("Waiting for robot search results!");
                IConnector selectedRobot = robotSearchFuture.get();
                LOG.debug("Result {}", selectedRobot);
                this.controller.setConnector(selectedRobot);

                Future<Boolean> connectorFuture = executorService.submit(selectedRobot);

                while ( !connectorFuture.isDone() ) {
                    if ( connectorShouldStop ) {
                        connectorFuture.cancel(true);
                    }
                    Thread.sleep(SLEEP_TIME);
                }
                connectorShouldStop = false;
                LOG.info("Connector finished!");
            } catch ( InterruptedException | ExecutionException e ) {
                LOG.error("Something went wrong: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        executorService.shutdown();
    }

    public static void stopConnector() {
        connectorShouldStop = true;
    }
}
