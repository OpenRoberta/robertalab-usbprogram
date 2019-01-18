package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.util.ORAListenable;

import java.util.concurrent.Callable;

/**
 * Defines a set of states, keywords and methods for handling the USB connection of a robot to the Open Roberta server. This interface is intended to be
 * implemented by all connector classes for different robot types.
 *
 * @author dpyka
 */
public interface IConnector extends Callable<Boolean>, ORAListenable<State> {

    enum State {
        DISCOVER,
        RECONNECT,
        WAIT_FOR_CONNECT_BUTTON_PRESS,
        CONNECT_BUTTON_IS_PRESSED,
        WAIT_FOR_CMD,
        WAIT_UPLOAD,
        WAIT_EXECUTION,
        DISCONNECT,
        WAIT_FOR_SERVER,
        UPDATE,
        UPDATE_SUCCESS,
        UPDATE_FAIL,
        ERROR_HTTP,
        ERROR_UPDATE,
        ERROR_BRICK,
        ERROR_DOWNLOAD,
        TOKEN_TIMEOUT
    }

    String KEY_TOKEN = "token";
    String KEY_CMD = "cmd";
    String KEY_SUBTYPE = "subtype";

    String CMD_REGISTER = "register";
    String CMD_PUSH = "push";
    String CMD_ISRUNNING = "isrunning";

    String CMD_REPEAT = "repeat";
    String CMD_ABORT = "abort";
    String CMD_UPDATE = "update";
    String CMD_DOWNLOAD = "download";
    String CMD_DOWNLOAD_RUN = "download_run";
    String CMD_CONFIGURATION = "configuration";

    /**
     * Search for a specific robot type for auto detection at the beginning of the program. The robot is considered to not run a user program at this time to be
     * available.
     *
     * @return true if a robot is connected, false otherwise
     */
    boolean findRobot();

    /**
     * Tell the connector to collect necessary data from the robot and initialise a registration to Open Roberta.
     */
    void userPressConnectButton();

    /**
     * Disconnect the current robot properly and search for robots again (start condition of the USB program).
     */
    void userPressDisconnectButton();

    /**
     * Shut down the connector for closing the USB program.
     */
    void close();

    /**
     * Get the token to display in the gui.
     *
     * @return the token
     */
    String getToken();

    /**
     * Get the robot name to display in the gui.
     *
     * @return robot name
     */
    String getBrickName();

    /**
     * In this state, the connector will download system libraries from the server, and upload it to the robot.
     */
    void updateFirmware();

    /**
     * Update the server communicator's address to which it will connect.
     *
     * @param customServerAddress the specified server address
     */
    void updateCustomServerAddress(String customServerAddress);

    /**
     * If gui fields are empty but advanced options is checked, use the default server address.
     */
    void resetToDefaultServerAddress();
}
