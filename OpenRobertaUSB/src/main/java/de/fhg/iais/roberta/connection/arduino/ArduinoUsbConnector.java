package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.connection.AbstractConnector;
import de.fhg.iais.roberta.util.JWMI;
import de.fhg.iais.roberta.util.OraTokenGenerator;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArduinoUsbConnector extends AbstractConnector {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoUsbConnector.class);

    private static final Map<UsbDevice, ArduinoType> supportedRobots = new HashMap<>();
    private static final String ARDUINO_ID_FILE = "arduino-ids.txt";

    private String portName = null;
    private ArduinoCommunicator arduinoCommunicator = null;
    private ArduinoType type = ArduinoType.NONE;

    private final Map<Integer, String> readIdFileErrors = new HashMap<>();

    public ArduinoUsbConnector() {
        super("Arduino");

        loadArduinoIds();
    }

    private static String checkIdFileLineFormat(List<String> values) {
        if ( values.size() == 3 ) {
            try {
                String.valueOf(Integer.valueOf(values.get(0), 16));
            } catch ( NumberFormatException e ) {
                return "errorConfigVendorId";
            }
            try {
                String.valueOf(Integer.valueOf(values.get(1), 16));
            } catch ( NumberFormatException e ) {
                return "errorConfigProductId";
            }
            try {
                ArduinoType.fromString(values.get(2));
            } catch ( IllegalArgumentException e ) {
                return "errorConfigArduinoType";
            }
        } else {
            return "errorConfigFormat";
        }
        return "";
    }

    private void loadArduinoIds() {
        File file = new File(ARDUINO_ID_FILE);
        if ( !file.exists() ) {
            LOG.warn("Could not find {}, using default file!", ARDUINO_ID_FILE);
            file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(ARDUINO_ID_FILE)).getFile());
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNr = 1;
            while ( (line = br.readLine()) != null ) {
                if ( !line.isEmpty() && !line.startsWith("#") ) {
                    List<String> values = Arrays.asList(line.split(","));

                    String error = checkIdFileLineFormat(values);
                    if ( error.isEmpty() ) {
                        supportedRobots.put(new UsbDevice(values.get(0), values.get(1)), ArduinoType.fromString(values.get(2)));
                    } else {
                        this.readIdFileErrors.put(lineNr, error);
                    }
                }
                lineNr++;
            }
        } catch ( FileNotFoundException e ) {
            LOG.error("Could not find file {}: {}", ARDUINO_ID_FILE, e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong with the {} file: {}", ARDUINO_ID_FILE, e.getMessage());
            this.readIdFileErrors.put(0, e.getMessage());
        }
    }

    public Map<Integer, String> getReadIdFileErrors() {
        return new HashMap<>(this.readIdFileErrors);
    }

    @Override
    public boolean findRobot() {
        if ( SystemUtils.IS_OS_LINUX ) {
            LOG.debug("Linux detected: searching for Arduinos");
            this.type = findArduinoLinux();
        } else if ( SystemUtils.IS_OS_WINDOWS ) {
            LOG.debug("Windows detected: searching for Arduinos");
            this.type = findArduinoWindows();
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            LOG.debug("MacOS detected: searching for Arduinos");
            this.type = findArduinoMac();
        }
        switch ( this.type ) {
            // Classic Arduino types
            case UNO:
            case MEGA:
            case NANO:
                this.brickName = "Arduino " + this.type.getPrettyText();
                break;
            // Special Arduino types, use the pretty text instead
            default:
                this.brickName = this.type.getPrettyText();
        }
        return this.type != ArduinoType.NONE;
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
                        findRobot();
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

    // based on https://stackoverflow.com/questions/22042661/mac-osx-get-usb-vendor-id-and-product-id
    private ArduinoType findArduinoMac() {
        try {
            Runtime rt = Runtime.getRuntime();
            String commands[] = {
                "/bin/sh",
                "-c",
                "system_profiler SPUSBDataType"
                    + "    | awk '"
                    + "      /Product ID:/{p=$3}"
                    + "      /Vendor ID:/{v=$3}"
                    + "      /Manufacturer:/{sub(/.*: /,\"\"); m=$0}"
                    + "      /Location ID:/{sub(/.*: /,\"\"); printf(\"%s:%s %s (%s)\\n\", v, p, $0, m);}"
                    + "    '"
            };
            Process pr = rt.exec(commands);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                String line;
                while ( (line = reader.readLine()) != null ) {
                    for ( Entry<UsbDevice, ArduinoType> robotEntry : supportedRobots.entrySet() ) {
                        UsbDevice usbDevice = robotEntry.getKey();
                        // search the device in the commands output
                        Matcher m = Pattern.compile("(?i)0x" + usbDevice.vendorId + ":0x" + usbDevice.productId + " 0x(\\d{3}).* \\/").matcher(line);
                        if ( m.find() ) {
                            // the corresponding tty ID seems to be the third hex number
                            // TODO do better, this is just an ugly workaround that always takes the first tty.usbserial
                            // TODO i do not know any way to correlate the unique id of the port to the device
                            if ( usbDevice.vendorId.equalsIgnoreCase("0403") && usbDevice.productId.equalsIgnoreCase("6001") ) {
                                Process devPr = rt.exec("ls /dev/");
                                try (BufferedReader devReader = new BufferedReader(new InputStreamReader(devPr.getInputStream()))) {
                                    String devFolder;
                                    while ( (devFolder = devReader.readLine()) != null ) {
                                        if ( devFolder.contains("tty.usbserial") ) {
                                            this.portName = devFolder;
                                            LOG.info("Found robot: {}:{}, using portname {}", usbDevice.vendorId, usbDevice.productId, this.portName);
                                            return robotEntry.getValue();
                                        }
                                    }
                                }
                            }
                            this.portName = "tty.usbmodem" + m.group(1) + '1';
                            LOG.info("Found robot: {}:{}, using portname {}", usbDevice.vendorId, usbDevice.productId, this.portName);
                            return robotEntry.getValue();
                        }
                    }
                }
            }
        } catch ( IOException e ) {
            return ArduinoType.NONE;
        }
        return ArduinoType.NONE;
    }

    private ArduinoType findArduinoWindows() {
        try {
            for ( Entry<UsbDevice, ArduinoType> robotEntry : supportedRobots.entrySet() ) {
                UsbDevice usbDevice = robotEntry.getKey();

                String
                    ArduQueryResult =
                    JWMI.getWMIValue("SELECT * FROM Win32_PnPEntity WHERE PnPDeviceID "
                        + "LIKE '%VID_"
                        + usbDevice.vendorId
                        + "%PID_"
                        + usbDevice.productId
                        + "%'", "Caption");
                Matcher m = Pattern.compile(".*\\((COM\\d+)\\)").matcher(ArduQueryResult);
                if ( m.find() ) {
                    this.portName = m.group(1);
                    LOG.info("Found robot: {}:{}, using portname {}", usbDevice.vendorId, usbDevice.productId, this.portName);
                    return robotEntry.getValue();
                }
            }
            return ArduinoType.NONE;
        } catch ( Exception e ) {
            LOG.error("Something went wrong when finding Arduinos: {}", e.getMessage());
            return ArduinoType.NONE;
        }
    }

    private ArduinoType findArduinoLinux() {
        File devices = new File("/sys/bus/usb/devices");

        // check every usb device
        for ( File devicesDirectories : Objects.requireNonNull(devices.listFiles()) ) {
            File idVendorFile = new File(devicesDirectories, "idVendor");
            File idProductFile = new File(devicesDirectories, "idProduct");

            // if the id files exist check the content
            if ( idVendorFile.exists() && idProductFile.exists() ) {
                try {
                    String idVendor = Files.lines(idVendorFile.toPath()).findFirst().get();
                    String idProduct = Files.lines(idProductFile.toPath()).findFirst().get();

                    // see if the ids are supported
                    UsbDevice usbDevice = new UsbDevice(idVendor, idProduct);
                    if ( supportedRobots.keySet().contains(usbDevice) ) {
                        // recover the tty portname of the device
                        // it can be found in the subdirectory with the same name as the device
                        for ( File subdirectory : devicesDirectories.listFiles() ) {
                            if ( subdirectory.getName().contains(devicesDirectories.getName()) ) {
                                List<File> subsubdirs = Arrays.asList(subdirectory.listFiles());

                                // look for a directory containing tty, in case its only called tty look into it to find the real name
                                subsubdirs.stream()
                                    .filter(s -> s.getName().contains("tty"))
                                    .findFirst()
                                    .ifPresent(f -> this.portName = f.getName().equals("tty") ? f.list()[0] : f.getName());
                            }
                        }
                        LOG.info("Found robot: {}:{}, using portname {}", idVendor, idProduct, this.portName);
                        return supportedRobots.get(usbDevice);
                    }
                } catch ( IOException e ) {
                    // continue if id files do not exist
                }
            }
        }

        return ArduinoType.NONE;
    }

    public String getPort() {
        return this.portName;
    }
}
