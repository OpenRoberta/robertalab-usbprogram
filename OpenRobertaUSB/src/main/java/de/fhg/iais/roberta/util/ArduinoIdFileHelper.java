package de.fhg.iais.roberta.util;

import de.fhg.iais.roberta.connection.arduino.ArduinoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArduinoIdFileHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ArduinoIdFileHelper.class);

    private static final String ARDUINO_ID_FILE = "arduino-ids.txt";

    public static Pair<Map<SerialDevice, ArduinoType>, Map<Integer, String>> loadArduinoIds() {
        Map<SerialDevice, ArduinoType> supportedRobots = new HashMap<>();
        Map<Integer, String> readIdFileErrors = new HashMap<>();

        File file = new File(ARDUINO_ID_FILE);

        if ( !file.exists() ) {
            LOG.warn("Could not find {}, using default file!", ARDUINO_ID_FILE);
        }

        try (InputStream inputStream = (file.exists()) ?
            new FileInputStream(file) :
            ArduinoIdFileHelper.class.getClassLoader().getResourceAsStream(ARDUINO_ID_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineNr = 1;
            while ( (line = br.readLine()) != null ) {
                if ( !line.isEmpty() && !line.startsWith("#") ) {
                    List<String> values = Arrays.asList(line.split(","));

                    String error = checkIdEntryFormat(values);
                    if ( error.isEmpty() ) {
                        supportedRobots.put(new SerialDevice(values.get(0), values.get(1), "", ""), ArduinoType.fromString(values.get(2)));
                    } else {
                        readIdFileErrors.put(lineNr, error);
                    }
                }
                lineNr++;
            }
        } catch ( FileNotFoundException e ) {
            LOG.error("Could not find file {}: {}", ARDUINO_ID_FILE, e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong while loading the {} file: {}", ARDUINO_ID_FILE, e.getMessage());
            readIdFileErrors.put(0, e.getMessage());
        }

        return new Pair<>(supportedRobots, readIdFileErrors);
    }

    public static void saveArduinoIds(Iterable<List<String>> arduinoIdEntries) {
        File file = new File(ARDUINO_ID_FILE);

        Map<Integer, String> readIdFileErrors = new HashMap<>();

        int lineNr = 0;
        for ( List<String> entry : arduinoIdEntries ) {
            String error = checkIdEntryFormat(entry);
            if ( !error.isEmpty() ) {
                readIdFileErrors.put(lineNr++, error);
            }
        }

        if (!readIdFileErrors.isEmpty()) {
            return;
        }

        try (FileOutputStream os = new FileOutputStream(file); OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            for ( List<String> entry : arduinoIdEntries ) {
                writer.write(entry.get(0) + ',' + entry.get(1) + ',' + entry.get(2) + System.lineSeparator());
            }
        } catch ( FileNotFoundException e ) {
            LOG.error("Could not find file {}: {}", ARDUINO_ID_FILE, e.getMessage());
        } catch ( IOException e ) {
            LOG.error("Something went wrong while writing the {} file: {}", ARDUINO_ID_FILE, e.getMessage());
        }
    }

    private static String checkIdEntryFormat(List<String> values) {
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
}
