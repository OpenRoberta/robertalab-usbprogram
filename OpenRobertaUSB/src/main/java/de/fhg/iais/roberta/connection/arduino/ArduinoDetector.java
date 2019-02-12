package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.JWMI;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

public class ArduinoDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoDetector.class);

    private static final Map<UsbDevice, ArduinoType> supportedRobots = new HashMap<>();
    private static final String ARDUINO_ID_FILE = "arduino-ids.txt";

    private final Map<Integer, String> readIdFileErrors = new HashMap<>();

    private ArduinoType type = ArduinoType.NONE;
    private String portName = null;

    public ArduinoDetector() {
        loadArduinoIds();
    }

    @Override
    public Robot getRobot() {
        return Robot.ARDUINO;
    }

    public ArduinoType getType() {
        return this.type;
    }

    public String getPortName() {
        return this.portName;
    }

    private void loadArduinoIds() {
        File file = new File(ARDUINO_ID_FILE);

        if (!file.exists()) {
            LOG.warn("Could not find {}, using default file!", ARDUINO_ID_FILE);
        }

        try (InputStream inputStream = (file.exists()) ? new FileInputStream(file) : getClass().getClassLoader().getResourceAsStream(ARDUINO_ID_FILE);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
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

    @Override
    public boolean detectRobot() {
        if ( SystemUtils.IS_OS_LINUX ) {
            LOG.debug("Linux detected: searching for Arduinos");
            this.type = detectArduinoLinux();
        } else if ( SystemUtils.IS_OS_WINDOWS ) {
            LOG.debug("Windows detected: searching for Arduinos");
            this.type = detectArduinoWindows();
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            LOG.debug("MacOS detected: searching for Arduinos");
            this.type = detectArduinoMac();
        }
        return this.type != ArduinoType.NONE;
    }

    // based on https://stackoverflow.com/questions/22042661/mac-osx-get-usb-vendor-id-and-product-id
    private ArduinoType detectArduinoMac() {
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
                    for ( Map.Entry<UsbDevice, ArduinoType> robotEntry : supportedRobots.entrySet() ) {
                        UsbDevice usbDevice = robotEntry.getKey();
                        // detectRobots the device in the commands output
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

    private ArduinoType detectArduinoWindows() {
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

    private ArduinoType detectArduinoLinux() {
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
}
