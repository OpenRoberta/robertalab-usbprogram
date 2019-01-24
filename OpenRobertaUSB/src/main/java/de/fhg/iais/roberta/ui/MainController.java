package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.IOraUiListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements IController, IOraListenable<Robot> {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Collection<IOraListener<Robot>> listeners = new ArrayList<>();

    // View related
    private final ResourceBundle rb;
    private final MainView mainView;

    // For the robot selection if there is more than one robot available
    private List<Robot> robotList = null;

    private boolean connected;

    private IConnector connector = null;

    // Child controllers of the main controller, this includes other windows/JFrames that are launched from the main controller
    private SerialMonitorController serialMonitorController = null;

    public MainController(ResourceBundle rb) {
        this.mainView = new MainView(rb, new MainViewListener());
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;
    }

    public void setRobotList(List<Robot> robotList) {
        this.robotList = new ArrayList<>(robotList);
        this.mainView.showRobotList(this.robotList.stream().map(Enum::toString).collect(Collectors.toList()));
    }

    @Override
    public void setState(State state) {
        LOG.info("setState: {}", state);
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;

                this.mainView.setWaitForConnect();

                if (this.connector.getRobot() == Robot.ARDUINO) {
                    this.mainView.showArduinoMenu();
                    this.mainView.setArduinoMenuText(this.connector.getBrickName());
                }
                break;
            case WAIT_FOR_SERVER:
                this.mainView.setNew(this.rb.getString("token") + ' ' + this.connector.getToken());
                this.mainView.setWaitForServer();
                break;
            case RECONNECT:
                this.mainView.setConnectButtonText(this.rb.getString("disconnect"));
            case WAIT_FOR_CMD:
                this.connected = true;
                this.mainView.setNew(this.rb.getString("name") + ' ' + this.connector.getBrickName());
                this.mainView.setWaitForCmd();
                break;
            case DISCOVER:
                setDiscover();
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                showAttentionPopup("restartInfo", "");
                break;
            case UPDATE_FAIL:
                showAttentionPopup("updateFail", "");
                break;
            case ERROR_HTTP:
                showAttentionPopup("httpErrorInfo", "");
                break;
            case ERROR_DOWNLOAD:
                showAttentionPopup("downloadFail", "");
                break;
            case ERROR_BRICK:
                showAttentionPopup("httpBrickInfo", "");
                break;
            case TOKEN_TIMEOUT:
                showAttentionPopup("tokenTimeout", "");
                break;
            default:
                break;
        }
    }

    @Override
    public void setConnector(IConnector connector) {
        LOG.info("setConnector: {}", connector.getRobot());
        this.connector = connector;
        this.connector.registerListener(this::setState);

        this.mainView.hideRobotList();

        // Serial monitor is only needed for arduino based robots
        if (connector.getRobot() == Robot.ARDUINO) {
            this.serialMonitorController = new SerialMonitorController(this.rb);
            this.serialMonitorController.setConnector(connector);
        }
    }

    public void showConfigErrorPopup(Map<Integer, String> errors) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(System.lineSeparator());
        for ( Entry<Integer, String> entry : errors.entrySet() ) {
            sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
        }
        LOG.error("Errors in config file:{}", sb);

        showAttentionPopup("errorReadConfig", sb.toString());
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    private void showAttentionPopup(String key, String additionalInfo) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"),
            this.rb.getString(key) + additionalInfo, null);
    }

    @Override
    public void registerListener(IOraListener<Robot> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<Robot> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(Robot object) {
        for ( IOraListener<Robot> listener : this.listeners ) {
            listener.update(object);
        }
    }

    private class MainViewListener implements IOraUiListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("actionPerformed: {}", e.getActionCommand());
            switch ( e.getActionCommand() ) {
                case "close":
                    closeApplication();
                    break;
                case "about":
                    showAboutPopup();
                    break;
                case "customaddress":
                    MainController.this.mainView.showAdvancedOptions();
                    break;
                case "scan":
                    MainController.this.connector.interrupt();
                    setDiscover();
                    break;
                case "serial":
                    MainController.this.serialMonitorController.showSerialMonitor();
                    break;
                case "connect":
                    checkForValidCustomServerAddressAndUpdate();
                    MainController.this.connector.userPressConnectButton();
                    break;
                case "disconnect":
                    MainController.this.connector.userPressDisconnectButton();
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.info("windowClosing");
            closeApplication();
        }

        // Sends event to all listeners waiting for the robot selection event when a robot was selected
        @Override
        public void valueChanged(ListSelectionEvent e) {
            LOG.info("valueChanged: {}", e.getFirstIndex());
            JList<?> source = (JList<?>) e.getSource();
            source.clearSelection();
            fire(MainController.this.robotList.get(e.getFirstIndex()));
        }

        private void showAboutPopup() {
            OraPopup.showPopup(MainController.this.mainView, MainController.this.rb.getString("about"), MainController.this.rb.getString("aboutInfo"),
                new ImageIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/iais_logo.gif"))).getImage()
                    .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
        }

        private void checkForValidCustomServerAddressAndUpdate() {
            LOG.debug("checkForValidCustomServerAddressAndUpdate");
            if ( MainController.this.mainView.isCustomAddressSelected() ) {
                String ip = MainController.this.mainView.getCustomIP();
                String port = MainController.this.mainView.getCustomPort();
                if ( ip.isEmpty() ) {
                    LOG.info("Invalid custom address (null or empty) - Using default address");
                    MainController.this.connector.resetToDefaultServerAddress();
                } else {
                    if ( port.isEmpty() ) {
                        LOG.info("Valid custom ip {}, using default ports", ip);
                        MainController.this.connector.updateCustomServerAddress(ip);
                    } else {
                        String address = ip + ':' + port;
                        LOG.info("Valid custom address {}", address);
                        MainController.this.connector.updateCustomServerAddress(address);
                    }
                }
            } else {
                MainController.this.connector.resetToDefaultServerAddress();
            }
        }

        private void closeApplication() {
            LOG.debug("closeApplication");
            if ( MainController.this.connected ) {
                String[] buttons = {
                    MainController.this.rb.getString("close"), MainController.this.rb.getString("cancel")
                };
                int n = OraPopup.showPopup(MainController.this.mainView, MainController.this.rb.getString("attention"), MainController.this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/Roberta.png"))),
                    buttons);
                if ( n == 0 ) {
                    if ( MainController.this.connector != null ) {
                        MainController.this.connector.close();
                    }
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        }
    }
}
