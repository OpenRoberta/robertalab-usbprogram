package de.fhg.iais.roberta.connection.ev3;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Ev3Detector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(Ev3Detector.class);
    private static final String brickIp = PropertyHelper.getInstance().getProperty("brickIp");

    @Override
    public Robot getRobot() {
        return Robot.EV3;
    }

    @Override
    public boolean detectRobot() {
        Ev3Communicator ev3comm = new Ev3Communicator(brickIp);

        try {
            if ( ev3comm.checkBrickState().equals("false") ) { // false ^= no program is running
                ev3comm.shutdown();
                return true;
            } else {
                LOG.info("EV3 is executing a program");
                ev3comm.shutdown();
                return false;
            }
        } catch ( IOException e ) {
            ev3comm.shutdown();
            return false;
        }
    }
}
