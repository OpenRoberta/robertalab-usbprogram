package de.fhg.iais.roberta.ui.main;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.arduino.Arduino;
import de.fhg.iais.roberta.connection.arduino.ArduinoConnector;
import de.fhg.iais.roberta.ui.IController;
import de.fhg.iais.roberta.ui.OraPopup;
import de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorController;
import de.fhg.iais.roberta.ui.serialMonitor.SerialMonitorController;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.CustomAddressHelper;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.IOraUiListener;
import de.fhg.iais.roberta.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static de.fhg.iais.roberta.ui.main.HelpDialog.CMD_CLOSE_HELP;
import static de.fhg.iais.roberta.ui.main.HelpDialog.CMD_SELECT_EV3;
import static de.fhg.iais.roberta.ui.main.HelpDialog.CMD_SELECT_OTHER;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_ABOUT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_COPY;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_EXIT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_CONNECT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_CUSTOMADDRESS;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_DISCONNECT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_HELP;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_ID_EDITOR;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_SCAN;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_SERIAL;
import static de.fhg.iais.roberta.ui.main.MainView.IMAGES_PATH;

public class MainController implements IController, IOraListenable<Robot> {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Collection<IOraListener<Robot>> listeners = new ArrayList<>();

    // View related
    private final ResourceBundle rb;
    private final MainView mainView;

    private final CustomAddressHelper addresses = new CustomAddressHelper();

    // For the robot selection if there is more than one robot available
    private List<Robot> robotList = null;

    private boolean connected;

    private IConnector connector = null;

    // Child controllers of the main controller, this includes other windows/JFrames that are launched from the main controller
    private SerialMonitorController serialMonitorController = null;
    private final DeviceIdEditorController deviceIdEditorController;

    private final HelpDialog helpDialog;

    public MainController(ResourceBundle rb) {
        MainController.MainViewListener mainViewListener = new MainController.MainViewListener();
        this.mainView = new MainView(rb, mainViewListener);
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        this.mainView.setCustomAddresses(this.addresses.get());

        this.helpDialog = new HelpDialog(this.mainView, rb, mainViewListener);

        this.deviceIdEditorController = new DeviceIdEditorController(rb);
    }

    public void setRobotList(List<? extends Robot> robotList) {
        this.robotList = new ArrayList<>(robotList);
        this.mainView.showTopRobots(this.robotList.stream().map(robot -> robot.getClass().getSimpleName() + ": " + robot.getName()).collect(Collectors.toList()));
    }

    @Override
    public void setState(IConnector.State state) {
        LOG.info("Setting state to {}", state);
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;

                this.mainView.setWaitForConnect();

                if ( this.connector instanceof ArduinoConnector ) {
                    this.mainView.showArduinoMenu();
                    this.mainView.setArduinoMenuText(this.connector.getBrickName());
                }
                break;
            case WAIT_FOR_SERVER:
                this.mainView.setNew(this.rb.getString("token"), this.connector.getToken(), this.connector.getServerAddress(), true);
                this.mainView.setWaitForServer();
                break;
            case RECONNECT:
                this.mainView.setConnectButtonText(this.rb.getString("disconnect"));
            case WAIT_FOR_CMD:
                this.connected = true;
                this.mainView.setNew(this.rb.getString("name"), this.connector.getBrickName(), this.connector.getServerAddress(), false);
                this.mainView.setWaitForCmd();
                break;
            case WAIT_UPLOAD:
                break;
            case DISCOVER:
                this.setDiscover();
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                this.showAttentionPopup("restartInfo", "");
                break;
            case UPDATE_FAIL:
                this.showAttentionPopup("updateFail", "");
                break;
            case ERROR_HTTP:
                this.showAttentionPopup("httpErrorInfo", "");
                break;
            case ERROR_DOWNLOAD:
                this.showAttentionPopup("downloadFail", "");
                break;
            case ERROR_BRICK:
                this.showAttentionPopup("httpBrickInfo", "");
                break;
            case TOKEN_TIMEOUT:
                this.showAttentionPopup("tokenTimeout", "");
                break;
            default:
                break;
        }
    }

    @Override
    public void setConnector(IConnector connector) {
        LOG.debug("setConnector: {}", connector.getClass().getSimpleName());
        this.connector = connector;
        this.connector.registerListener(this::setState);

        this.mainView.showTopTokenServer();

        // Serial monitor is only needed for arduino based robots
        if ( connector instanceof ArduinoConnector ) {
            this.serialMonitorController = new SerialMonitorController(this.rb);
            this.serialMonitorController.setConnector(connector);
        }
    }

    public void showConfigErrorPopup(Map<Integer, String> errors) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(System.lineSeparator());
        for ( Map.Entry<Integer, String> entry : errors.entrySet() ) {
            sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
        }
        LOG.error("Errors in config file:{}", sb);

        this.showAttentionPopup("errorReadConfig", sb.toString());
    }

    public void showHelp() {
        this.helpDialog.setLocation(this.mainView.getRobotButtonLocation());
        this.helpDialog.setVisible(true);
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    private void showAttentionPopup(String key, String additionalInfo) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"), this.rb.getString(key) + additionalInfo, null);
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
            LOG.info("User performed action {}", e.getActionCommand());
            switch ( e.getActionCommand() ) {
                case CMD_EXIT:
                    this.closeApplication();
                    break;
                case CMD_ABOUT:
                    this.showAboutPopup();
                    break;
                case CMD_CUSTOMADDRESS:
                    MainController.this.mainView.toggleAdvancedOptions();
                    break;
                case CMD_SCAN:
                    MainController.this.connector.interrupt();
                    MainController.this.setDiscover();
                    break;
                case CMD_SERIAL:
                    MainController.this.serialMonitorController.showSerialMonitor();
                    break;
                case CMD_CONNECT:
                    this.checkForValidCustomServerAddressAndUpdate();
                    MainController.this.connector.userPressConnectButton();
                    break;
                case CMD_DISCONNECT:
                    MainController.this.connector.userPressDisconnectButton();
                    break;
                case CMD_HELP:
                    this.toggleHelp();
                    break;
                case CMD_ID_EDITOR:
                    MainController.this.deviceIdEditorController.showEditor();
                    if ( MainController.this.connector != null ) {
                        MainController.this.connector.interrupt();
                    }
                    MainController.this.setDiscover();
                    break;
                case CMD_SELECT_EV3:
                    MainController.this.helpDialog.dispose();
                    try {
                        Desktop.getDesktop().browse(new URI(MainController.this.rb.getString("linkEv3UsbWiki")));
                    } catch ( IOException | URISyntaxException e1 ) {
                        LOG.error("Could not open browser: {}", e1.getMessage());
                    }
                    break;
                case CMD_SELECT_OTHER:
                    MainController.this.helpDialog.dispose();
                    MainController.this.deviceIdEditorController.showEditor();
                    if ( MainController.this.connector != null ) {
                        MainController.this.connector.interrupt();
                    }
                    MainController.this.setDiscover();
                    break;
                case CMD_CLOSE_HELP:
                    MainController.this.helpDialog.dispose();
                    break;
                case CMD_COPY:
                    Transferable stringSelection = new StringSelection(MainController.this.connector.getToken());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.info("User closed main window");
            this.closeApplication();
        }

        // Sends event to all listeners waiting for the robot selection event when a robot was selected
        @Override
        public void valueChanged(ListSelectionEvent e) {
            LOG.debug("valueChanged: {}", e.getFirstIndex());
            JList<?> source = (JList<?>) e.getSource();
            source.clearSelection();
            MainController.this.fire(MainController.this.robotList.get(e.getFirstIndex()));
        }

        private void showAboutPopup() {
            OraPopup.showPopup(
                MainController.this.mainView,
                MainController.this.rb.getString("about"),
                MainController.this.rb.getString("aboutInfo"),
                new ImageIcon(new ImageIcon(Objects.requireNonNull(this.getClass()
                    .getClassLoader()
                    .getResource("images" + File.separator + "iais_logo.gif"))).getImage().getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
        }

        private void toggleHelp() {
            MainController.this.helpDialog.setLocation(MainController.this.mainView.getRobotButtonLocation());
            MainController.this.helpDialog.setVisible(!MainController.this.helpDialog.isVisible());
        }

        private void checkForValidCustomServerAddressAndUpdate() {
            LOG.debug("checkForValidCustomServerAddressAndUpdate");
            if ( MainController.this.mainView.isCustomAddressSelected() ) {
                Pair<String, String> address = MainController.this.mainView.getCustomAddress();
                String ip = address.getFirst();
                String port = address.getSecond();

                if ( ip.isEmpty() ) {
                    LOG.warn("Invalid custom address - Using default address");
                    MainController.this.connector.resetToDefaultServerAddress();
                } else {
                    if ( port.isEmpty() ) {
                        LOG.info("Valid custom ip {}, using default ports", ip);
                        MainController.this.connector.updateCustomServerAddress(ip);
                        MainController.this.addresses.add(address);
                        MainController.this.mainView.setCustomAddresses(MainController.this.addresses.get());
                    } else {
                        if ( CustomAddressHelper.validatePort(port) ) {
                            String formattedAddress = ip + ':' + port;
                            LOG.info("Valid custom address {}", formattedAddress);
                            MainController.this.connector.updateCustomServerAddress(formattedAddress);
                            MainController.this.addresses.add(address);
                            MainController.this.mainView.setCustomAddresses(MainController.this.addresses.get());
                        } else {
                            LOG.warn("Invalid port {}", port);
                        }
                    }
                }
            } else {
                MainController.this.connector.resetToDefaultServerAddress();
            }
        }

        private void closeApplication() {
            LOG.debug("closeApplication");
            MainController.this.addresses.save();
            if ( MainController.this.connected ) {
                String[] buttons = {
                    MainController.this.rb.getString("exit"), MainController.this.rb.getString("cancel")
                };
                int n = OraPopup.showPopup(
                    MainController.this.mainView,
                    MainController.this.rb.getString("attention"),
                    MainController.this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource(IMAGES_PATH + "Roberta.png"))),
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
