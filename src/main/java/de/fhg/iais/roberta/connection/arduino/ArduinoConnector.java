package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ArduinoConnector extends AbstractConnector {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoConnector.class);

    private ArduinoCommunicator arduinoCommunicator = null;

    private final ArduinoType type;
    private final String portName;

    public ArduinoConnector(ArduinoType type, String portName) {
        super(determineArduinoName(type));
        this.type = type;
        this.portName = portName;
    }

    private static String determineArduinoName(ArduinoType type) {
        switch ( type ) {
            // Classic Arduino types
            case UNO:
            case MEGA:
            case NANO:
                return Robot.ARDUINO + " " + type.getPrettyText();
            // Special Arduino types, use the pretty text instead
            default:
                return type.getPrettyText();
        }
    }

    @Override
    protected void runLoopBody() {
        switch ( this.state ) {
            case DISCOVER:
                if ( this.portName.isEmpty() ) {
                    LOG.info("No Arduino device connected");
                } else {
                    // if the user disconnected check the arduino type again, it might've changed
                    if ( this.userDisconnect ) {
//                        findRobot();
                    }
                    this.arduinoCommunicator = new ArduinoCommunicator(this.brickName, this.type);
                    this.state = State.WAIT_FOR_CONNECT_BUTTON_PRESS;
                    fire(this.state);
                    break;
                }
                break;
            case WAIT_EXECUTION:
                this.state = State.WAIT_FOR_CMD;
                fire(this.state);

                break;
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                //                    // GUI initiates changing state to CONNECT
                break;
            case CONNECT_BUTTON_IS_PRESSED:
                this.token = OraTokenGenerator.generateToken();
                this.state = State.WAIT_FOR_SERVER;
                fire(this.state);
                this.brickData = this.arduinoCommunicator.getDeviceInfo();
                this.brickData.put(KEY_TOKEN, this.token);
                this.brickData.put(KEY_CMD, CMD_REGISTER);
                try {
                    JSONObject serverResponse = this.serverCommunicator.pushRequest(this.brickData);
                    String command = serverResponse.getString("cmd");
                    switch ( command ) {
                        case CMD_REPEAT:
                            this.state = State.WAIT_FOR_CMD;
                            fire(this.state);
                            LOG.info("Robot successfully registered with token {}, waiting for commands", this.token);
                            break;
                        case CMD_ABORT:
                            LOG.info("registration timeout");
                            fire(State.TOKEN_TIMEOUT);
                            this.state = State.DISCOVER;
                            fire(this.state);
                            break;
                        default:
                            throw new RuntimeException("Unexpected command " + command + "from server");
                    }
                } catch ( IOException | RuntimeException io ) {
                    LOG.info("CONNECT {}", io.getMessage());
                    reset(State.ERROR_HTTP);
                }
                break;
            case WAIT_FOR_CMD:
                this.brickData = this.arduinoCommunicator.getDeviceInfo();
                this.brickData.put(KEY_TOKEN, this.token);
                this.brickData.put(KEY_CMD, CMD_PUSH);
                try {
                    JSONObject response = this.serverCommunicator.pushRequest(this.brickData);
                    String cmdKey = response.getString(KEY_CMD);
                    if ( cmdKey.equals(CMD_REPEAT) ) {
                        break;
                    } else if ( cmdKey.equals(CMD_DOWNLOAD) ) {
                        LOG.info("Download user program");
                        try {
                            byte[] binaryfile = this.serverCommunicator.downloadProgram(this.brickData);
                            String filename = this.serverCommunicator.getFilename();
                            File temp = File.createTempFile(filename, "");

                            temp.deleteOnExit();

                            if ( !temp.exists() ) {
                                throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
                            }

                            try (FileOutputStream os = new FileOutputStream(temp)) {
                                os.write(binaryfile);
                            }

                            this.state = State.WAIT_UPLOAD;
                            fire(this.state);
                            this.arduinoCommunicator.uploadFile(this.portName, temp.getAbsolutePath());
                            this.state = State.WAIT_EXECUTION;
                            fire(this.state);
                        } catch ( IOException io ) {
                            LOG.info("Download and run failed: {}", io.getMessage());
                            LOG.info("Do not give up yet - make the next push request");
                            this.state = State.WAIT_FOR_CMD;
                        }
                    } else if ( cmdKey.equals(CMD_CONFIGURATION) ) {
                        LOG.info("Configuration");
                    } else if ( cmdKey.equals(CMD_UPDATE) ) {
                        LOG.info("Firmware updated not necessary and not supported!");// LOG and go to abort
                    } else if ( cmdKey.equals(CMD_ABORT) ) {
                        throw new RuntimeException("Unexpected response from server");
                    }
                } catch ( RuntimeException | IOException r ) {
                    LOG.info("WAIT_FOR_CMD {}", r.getMessage());
                    reset(State.ERROR_HTTP);
                }
                break;
            default:
                break;
        }
    }

    public String getPortName() {
        return this.portName;
    }

    @Override
    public Robot getRobot() {
        return Robot.ARDUINO;
    }
}
