package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.SerialLoggingTask;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.Observable;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SerialMonitorController implements IController {
    private static final Logger LOG = LoggerFactory.getLogger(SerialMonitorController.class);

    private final SerialMonitorView serialMonitorView;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<Void> serialLoggingFuture = null;

    private final SerialLoggingObserver serialLoggingObserver;
    private final ConnectorObserver connectorObserver;

    private IConnector connector = null;

    public SerialMonitorController(ResourceBundle rb) {
        this.serialMonitorView = new SerialMonitorView(rb, new SerialMonitorViewListener(this));
        this.serialMonitorView.setVisible(false);

        this.serialLoggingObserver = new SerialLoggingObserver(this);
        this.connectorObserver = new ConnectorObserver(this);
    }

    public void setState(State state) {
        switch ( state ) {
            case WAIT_UPLOAD:
                this.stopSerialLogging();
                break;
            case WAIT_EXECUTION:
                if ( this.serialMonitorView.isVisible() ) {
                    this.restartSerialLogging();
                }
                break;
            default:
                break;
        }
    }

    void showSerialMonitor() {
        LOG.debug("showSerialMonitor");

        this.serialMonitorView.setVisible(true);

        restartSerialLogging();
    }

    void restartSerialLogging() {
        LOG.debug("restartSerialLogging");
        stopSerialLogging();

        // TODO improve
        if ( this.connector instanceof ArduinoUSBConnector ) {
            this.serialLoggingFuture =
                this.executorService.submit(new SerialLoggingTask(this.serialLoggingObserver,
                    ((ArduinoUSBConnector) this.connector).getPort(),
                    this.serialMonitorView.getSerialRate()));
        }
    }

    void appendSerial(byte[] readBuffer) {
        SwingUtilities.invokeLater(() -> this.serialMonitorView.appendText(readBuffer));
    }

    void clearSerialLog() {
        this.serialMonitorView.clearText();
    }

    void stopSerialLogging() {
        if ( this.serialLoggingFuture != null ) {
            this.serialLoggingFuture.cancel(true);
        }
    }

    public void setConnector(IConnector connector) {
        this.connector = connector;
        ((Observable) this.connector).addObserver(this.connectorObserver);
    }
}
