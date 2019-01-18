package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.IOraUiListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
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

    private List<Robot> robotList = null;

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

    public void setRobotList(List<Robot> robotList) {
        this.robotList = new ArrayList<>(robotList);
        this.mainView.showRobotList(this.robotList.stream().map(Enum::toString).collect(Collectors.toList()));
    }

    public void setState(State state) {
        LOG.info("setState {}", state);
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

        this.serialMonitorController.setConnector(connector);

        // TODO?
        connector.run();

        this.connector.unregisterListener(this::setState);
    }

    public void showConfigErrorPopup(Map<Integer, String> errors) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(System.lineSeparator());
        for ( Entry<Integer, String> entry : errors.entrySet() ) {
            sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
        }
        LOG.error("Something went wrong when loading the arduino id file:{}", sb);

        OraPopup.showPopup(this.mainView, this.rb.getString("attention"), this.rb.getString("errorReadConfig") + sb, null);
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
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
            AbstractButton button = (AbstractButton) e.getSource();
            if ( button.getActionCommand().equals("close") ) {
                LOG.debug("User close");
                closeApplication();
            } else if ( button.getActionCommand().equals("about") ) {
                LOG.debug("User about");
                showAboutPopup();
            } else if ( button.getActionCommand().equals("customaddress") ) {
                LOG.debug("User custom address");
                MainController.this.mainView.showAdvancedOptions();
            } else if ( button.getActionCommand().equals("scan") ) {
                LOG.debug("User scan");
                MainController.this.connector.interrupt();
                setDiscover();
            } else if ( button.getActionCommand().equals("serial")) {
                LOG.debug("User serial");
                MainController.this.serialMonitorController.showSerialMonitor();
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
            JList<?> source = (JList<?>) e.getSource();
            source.clearSelection();
            fire(MainController.this.robotList.get(e.getFirstIndex()));
        }
    }
}
