package de.fhg.iais.roberta.usb;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
import de.fhg.iais.roberta.connection.ev3.EV3USBConnector;
import de.fhg.iais.roberta.ui.ConnectionView;
import de.fhg.iais.roberta.ui.UIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class USBProgram {
    private static final Logger LOG = LoggerFactory.getLogger(USBProgram.class);

    static {
        ResourceBundle serverProps = ResourceBundle.getBundle("OpenRobertaUSB");
        connectorList = Arrays.asList(
            new ArduinoUSBConnector(serverProps),
            new EV3USBConnector(serverProps));
    }

    private static final List<IConnector> connectorList;
    private static boolean connectorShouldStop = false;
    private ConnectionView view = null;
    private UIController controller = null;

    public USBProgram() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                ResourceBundle messages = getLocale();
                this.view = new ConnectionView(messages);
                this.controller = new UIController(this.view, messages);
            });
        } catch ( InterruptedException | InvocationTargetException e ) {
            LOG.error("UI could not be set up");
            closeProgram();
        }
    }

    public void run() {
        LOG.debug("Entering run method!");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        while(!Thread.currentThread().isInterrupted()) {
            Future<IConnector> robotSearchFuture = executorService.submit(new RobotSearchTask(connectorList, this.controller));

            try {
                LOG.debug("Waiting for robot search results!");
                IConnector selectedRobot = robotSearchFuture.get();
                LOG.debug("Result {}", selectedRobot);
                this.controller.setConnector(selectedRobot);

                Future<Boolean> connectorFuture = executorService.submit(selectedRobot);

                while(!connectorFuture.isDone()) {
                    if(connectorShouldStop) {
                        connectorFuture.cancel(true);
                    }
                    Thread.sleep(1000);
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

    private static ResourceBundle getLocale() {
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle("messages", Locale.getDefault());
        } catch ( RuntimeException e ) {
            rb = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }

        LOG.info("Language {}", rb.getLocale());
        return rb;
    }

    public static void closeProgram() {
        System.exit(0);
    }
}
