package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.arduino.ArduinoUsbConnector;
import de.fhg.iais.roberta.usb.UsbProgram;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.IOraUiListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
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

public class MainController implements IController, IOraListenable<IConnector> {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Collection<IOraListener<IConnector>> listeners = new ArrayList<>();

    // View related
    private final ResourceBundle rb;
    private final MainView mainView;

    private List<IConnector> connectorList = null;

    private boolean connected;

    private IConnector connector = null;

    // Child controllers of the main controller, this includes other windows/JFrames that are launched from the main controller
    private final SerialMonitorController serialMonitorController;

    public MainController(ResourceBundle rb) {
        this.mainView = new MainView(rb, new MainViewListener());
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        this.serialMonitorController = new SerialMonitorController(rb);
    }

    public void setConnectorList(List<IConnector> connectorList) {
        this.connectorList = new ArrayList<>(connectorList);

        List<String> robotNames = this.connectorList.stream().map(IConnector::getBrickName).collect(Collectors.toList());
        this.mainView.showRobotList(robotNames);
    }

    public void setState(State state) {
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;

                this.mainView.setWaitForConnect();

                if ( this.connector instanceof ArduinoUsbConnector ) {
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
                this.setDiscover();
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                this.showAttentionPopup("restartInfo");
                break;
            case UPDATE_FAIL:
                this.showAttentionPopup("updateFail");
                break;
            case ERROR_HTTP:
                this.showAttentionPopup("httpErrorInfo");
                break;
            case ERROR_DOWNLOAD:
                this.showAttentionPopup("downloadFail");
                break;
            case ERROR_BRICK:
                this.showAttentionPopup("httpBrickInfo");
                break;
            case TOKEN_TIMEOUT:
                this.showAttentionPopup("tokenTimeout");
                break;
            default:
                break;
        }
    }

    public void setConnector(IConnector connector) {
        LOG.debug("setConnector");
        this.connector = connector;
        this.connector.registerListener(this::setState);

        this.mainView.hideRobotList();

        LOG.info("GUI setup done. Using {}", connector.getClass().getSimpleName());

        if ( this.connector instanceof ArduinoUsbConnector ) {
            ArduinoUsbConnector arduinoUSBConnector = (ArduinoUsbConnector) this.connector;
            Map<Integer, String> errors = arduinoUSBConnector.getReadIdFileErrors();
            if ( !errors.isEmpty() ) {
                StringBuilder sb = new StringBuilder(200);
                sb.append(System.lineSeparator());
                for ( Entry<Integer, String> entry : errors.entrySet() ) {
                    sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
                }
                LOG.error("Something went wrong when loading the arduino id file:{}", sb);
                showConfigErrorPopup(sb.toString());
            }
        }

        this.serialMonitorController.setConnector(connector);
    }

    private void showConfigErrorPopup(String errors) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"), this.rb.getString("errorReadConfig") + errors, null);
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    private void showAdvancedOptions() {
        LOG.debug("showAdvancedOptions");
        this.mainView.showAdvancedOptions();
    }

    private void checkForValidCustomServerAddressAndUpdate() {
        LOG.debug("checkForValidCustomServerAddressAndUpdate");
        if ( this.mainView.isCustomAddressSelected() ) {
            String ip = this.mainView.getCustomIP();
            String port = this.mainView.getCustomPort();
            if ( ip.isEmpty() ) {
                LOG.info("Invalid custom address (null or empty) - Using default address");
                this.connector.resetToDefaultServerAddress();
            } else {
                if ( port.isEmpty() ) {
                    LOG.info("Valid custom ip {}, using default ports", ip);
                    this.connector.updateCustomServerAddress(ip);
                } else {
                    String address = ip + ':' + port;
                    LOG.info("Valid custom address {}", address);
                    this.connector.updateCustomServerAddress(address);
                }
            }
        } else {
            this.connector.resetToDefaultServerAddress();
        }
    }

    private void closeApplication() {
        LOG.debug("closeApplication");
        if ( this.connected ) {
            String[] buttons = {
                this.rb.getString("close"), this.rb.getString("cancel")
            };
            int n = OraPopup.showPopup(
                this.mainView,
                this.rb.getString("attention"),
                this.rb.getString("confirmCloseInfo"),
                new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/Roberta.png"))),
                buttons);
            if ( n == 0 ) {
                if ( this.connector != null ) {
                    this.connector.close();
                }
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private void showAttentionPopup(String key) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"),
            this.rb.getString(key), null);
    }

    private void showAboutPopup() {
        OraPopup.showPopup(
            this.mainView,
            this.rb.getString("about"),
            this.rb.getString("aboutInfo"),
            new ImageIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/iais_logo.gif"))).getImage()
                .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
    }

    private void showSerialMonitor() {
        this.serialMonitorController.showSerialMonitor();
    }

    @Override
    public void registerListener(IOraListener<IConnector> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<IConnector> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(IConnector object) {
        for ( IOraListener<IConnector> listener : this.listeners ) {
            listener.update(object);
        }
    }

    private class MainViewListener implements IOraUiListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractButton button = (AbstractButton) e.getSource();
            if ( button.getActionCommand().equals("close") ) {
                LOG.debug("User close");
                closeApplication();
            } else if ( button.getActionCommand().equals("about") ) {
                LOG.debug("User about");
                showAboutPopup();
            } else if ( button.getActionCommand().equals("customaddress") ) {
                LOG.debug("User custom address");
                showAdvancedOptions();
            } else if ( button.getActionCommand().equals("scan") ) {
                LOG.debug("User scan");
                UsbProgram.stopConnector();
                setDiscover();
            } else if ( button.getActionCommand().equals("serial")) {
                LOG.debug("User serial");
                showSerialMonitor();
            } else {
                if ( button.isSelected() ) {
                    LOG.debug("User connect");
                    if ( MainController.this.connector != null ) { //TODO
                        checkForValidCustomServerAddressAndUpdate();
                        MainController.this.connector.userPressConnectButton();
                    }
                } else {
                    LOG.debug("User disconnect");
                    if ( MainController.this.connector != null ) {
                        MainController.this.connector.userPressDisconnectButton();
                    }
                }
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.debug("User close");
            closeApplication();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            fire(MainController.this.connectorList.get(e.getFirstIndex()));
        }
    }
}
