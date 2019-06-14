package de.fhg.iais.roberta.connection.nao;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import net.lingala.zip4j.exception.ZipException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

public class NaoConnector extends AbstractConnector {
    private static final Logger LOG = LoggerFactory.getLogger(NaoConnector.class);

    private final String robotIp;
    private String robotUsername = "nao";
    private String robotPassword = "nao";

    private NaoCommunicator naocomm;

    public NaoConnector(Nao nao) {
        super(nao.getName());
        this.robotIp = nao.getAddress().getHostName();
    }

    public void setLogin(String username, String password) {
        this.robotUsername = username;
        this.robotPassword = password;
    }

    @Override
    protected void runLoopBody() {
        switch ( this.state ) {
            case DISCOVER:
                if ( this.robotIp.isEmpty() ) {
                    LOG.info("No NAO device connected");
                } else {
                    this.naocomm = new NaoCommunicator(this.brickName, this.robotIp, this.robotUsername, this.robotPassword);
                    JSONObject deviceInfo = this.naocomm.getDeviceInfo();
                    if ( !this.token.isEmpty() && !this.brickName.isEmpty() ) {
                        if ( this.brickName.equals(deviceInfo.optString("brickname", "")) ) {

                            this.fire(State.WAIT_FOR_CMD);
                        }
                    } else {
                        this.fire(State.WAIT_FOR_CONNECT_BUTTON_PRESS);
                    }
                }
                break;
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                // GUI initiates changing state to CONNECT
                NaoState naoState = this.naocomm.getNAOstate();
                if ( naoState == NaoState.PROGRAM_RUNNING || naoState == NaoState.DISCONNECTED ) {
                    this.reset(null);
                    LOG.info("RESET CONNECTION BECAUSE {}", naoState);
                    break;
                }
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.token = OraTokenGenerator.generateToken();
                this.fire(State.WAIT_FOR_SERVER);
                JSONObject deviceInfo = this.naocomm.getDeviceInfo();
                if ( deviceInfo == null ) {
                    this.reset(State.ERROR_BRICK);
                    break;
                }
                deviceInfo.put(KEY_TOKEN, this.token);
                deviceInfo.put(KEY_CMD, CMD_REGISTER);
                try {
                    //Blocks until the server returns command in its response
                    JSONObject serverResponse = this.serverCommunicator.pushRequest(deviceInfo);
                    String command = serverResponse.getString("cmd");
                    if ( command.equals(CMD_REPEAT) ) {

                        LOG.info("registration successful");
                        this.brickName = deviceInfo.getString("brickname");
                        this.fire(State.WAIT_FOR_CMD);
                    } else if ( command.equals(CMD_ABORT) ) {
                        LOG.info("registration timeout");
                        this.fire(State.TOKEN_TIMEOUT);
                        this.fire(State.DISCOVER);
                    } else {
                        throw new RuntimeException("Unexpected command " + command + "from server");
                    }
                } catch ( IOException | RuntimeException e ) {
                    LOG.info("SERVER COMMUNICATION ERROR {}", e.getMessage());
                    this.reset(State.ERROR_HTTP);
                    this.resetLastConnectionData();
                }
                break;
            case WAIT_FOR_CMD:
                JSONObject deviceInfoWaitCMD = this.naocomm.getDeviceInfo();
                if ( deviceInfoWaitCMD == null ) {
                    this.reset(State.ERROR_BRICK);
                    break;
                }
                deviceInfoWaitCMD.put(KEY_TOKEN, this.token);
                deviceInfoWaitCMD.put(KEY_CMD, CMD_PUSH);

                try {
                    JSONObject pushRequestResponse = this.serverCommunicator.pushRequest(deviceInfoWaitCMD);
                    String serverCommand = pushRequestResponse.getString(KEY_CMD);

                    if (serverCommand.equals(CMD_REPEAT)) {
                        // do nothing
                    } else if (serverCommand.equals(CMD_DOWNLOAD)) {
                        this.fire(State.WAIT_UPLOAD);
                    } else {
                        LOG.info("WAIT_FOR_CMD {}", "Unexpected response from server");
                        this.resetLastConnectionData();
                        this.reset(State.ERROR_HTTP);
                    }
                } catch ( IOException e ) {
                    LOG.info("WAIT_FOR_CMD {}", e.getMessage());
                    this.resetLastConnectionData();
                    this.reset(State.ERROR_HTTP);
                }
                break;
            case WAIT_UPLOAD:
                JSONObject deviceInfoDownloadCMD = this.naocomm.getDeviceInfo();
                if ( deviceInfoDownloadCMD == null ) {
                    this.reset(State.ERROR_BRICK);
                    break;
                }
                deviceInfoDownloadCMD.put(KEY_TOKEN, this.token);
                deviceInfoDownloadCMD.put(KEY_CMD, CMD_REGISTER);
                try {
                    byte[] binaryfile = this.serverCommunicator.downloadProgram(deviceInfoDownloadCMD);
                    String filename = this.serverCommunicator.getFilename();
                    this.naocomm.updateRobotLogin(this.robotUsername, this.robotPassword);
                    boolean success = this.uploadProgram(binaryfile, filename);
                    if ( success ) {
                        this.fire(State.WAIT_EXECUTION);
                    } else {
                        this.reset(State.ERROR_UPLOAD_TO_ROBOT);
                    }
                } catch ( IOException e ) {
                    LOG.info("Do not give up yet - make the next push request");
                    this.reset(State.ERROR_DOWNLOAD);
                }
                break;
            case WAIT_EXECUTION:
                NaoState robotState = this.naocomm.getNAOstate();
                if ( robotState == NaoState.WAITING_FOR_PROGRAM ) {
                    LOG.info("Program execution finished - enter WAIT_FOR_CMD state again");
                    this.fire(State.WAIT_FOR_CMD);
                    break;
                } else {
                    LOG.info("Robot does not wait for a program because {}", robotState);
                }
                break;

            default:
                break;
        }
    }

    private boolean uploadProgram(byte[] binaryfile, String filename) {
        try {
            this.naocomm.uploadFile(binaryfile, filename);
            return true;
        } catch ( Exception e ) {
            LOG.info("Download failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void userPressConnectButton() {
        super.userPressConnectButton();
        try {
            if ( !this.serverCommunicator.verifyHalChecksumNAO() ) {
                this.serverCommunicator.updateHalNAO();
            }
        } catch ( NoSuchAlgorithmException | ZipException | IOException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void userPressDisconnectButton() {
        this.userDisconnect = true;
        this.resetLastConnectionData();
        this.serverCommunicator.abortNAO(); // will throw exception, reset will be called in catch statement
    }

    private void resetLastConnectionData() {
        LOG.info("resetting");
        this.token = "";
        this.brickName = "";
    }

    @Override
    public void close() {
        super.close();
        this.serverCommunicator.shutdownNAO();
    }
}
