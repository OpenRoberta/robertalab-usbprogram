package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.util.JWMI;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bob3USBConnector extends ArduinoUSBConnector {

    public Bob3USBConnector(ResourceBundle serverProps) {
        super(serverProps, "Bob3");
    }

    @Override
    protected AbstractArduinoCommunicator createArduinoCommunicator() {
        return new Bob3Communicator(this.brickName);
    }

    @Override
    protected boolean findArduinoMac() {
        try {
            File file = new File("/dev/");
            String[] directories = file.list();
            for ( String directory : directories ) {
                if ( directory.startsWith("cu.SLAB_USBtoUART") ) {
                    return true;
                }
            }
            return false;
        } catch ( Exception e ) {
            return false;
        }
    }

    @Override
    protected boolean findArduinoWindows() {
        try {
            return JWMI.getWMIValue("SELECT * FROM Win32_PnPEntity WHERE Caption LIKE '%(COM%' ", "Caption").contains("Silicon Labs");
        } catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean findArduinoLinux() {
        try {
            File file = new File("/dev/serial/by-id/");
            String[] directories = file.list();
            for ( String directory : directories ) {
                if ( directory.matches("usb-16c0_0933-if00") ) {
                    return true;
                }
            }
            return false;
        } catch ( Exception e ) {
            return false;
        }
    }

    @Override
    protected void getPortName() throws Exception {
        if ( SystemUtils.IS_OS_WINDOWS ) {
            String ArduQueryResult = JWMI.getWMIValue("SELECT * FROM Win32_PnPEntity WHERE Caption LIKE '%(COM%' ", "Caption");
            Matcher m = Pattern.compile("(Van Ooijen Technische Informatica \\()(.*)\\)").matcher(ArduQueryResult);
            while ( m.find() ) {
                portName = m.group(2);
            }

        } else if ( SystemUtils.IS_OS_LINUX ) {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("ls /dev/");

            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line;
            while ( (line = reader.readLine()) != null ) {
                Matcher m = Pattern.compile("(ttyACM)").matcher(line);
                if ( m.find() ) {
                    this.portName = line;
                    //  System.out.print(this.portName + "\n");
                }
            }

        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("ls /dev/");

            BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line;
            while ( (line = reader.readLine()) != null ) {
                Matcher m = Pattern.compile("(cu.SLAB_USBtoUART)").matcher(line);
                if ( m.find() ) {
                    this.portName = line;
                    //  System.out.print(this.portName + "\n");
                }
            }
        }
    }
}
