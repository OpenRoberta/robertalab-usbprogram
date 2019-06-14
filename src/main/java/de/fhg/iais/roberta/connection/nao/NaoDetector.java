package de.fhg.iais.roberta.connection.nao;

import de.fhg.iais.roberta.connection.IDetector;
import de.fhg.iais.roberta.usb.Robot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NaoDetector implements IDetector {
    private static final Logger LOG = LoggerFactory.getLogger(NaoDetector.class);

    private JmDNS jmDNS;

    private final List<Robot> detectedRobots = new ArrayList<>();

    public NaoDetector() {
        try {
            this.jmDNS = JmDNS.create();

            this.jmDNS.addServiceListener("_nao._tcp.local.", new SampleListener());
        } catch ( IOException e ) {
            LOG.error("JMDNS could not be initialized: {}", e.getMessage());
        }
    }

    private class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            LOG.info("Nao added");
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            InetAddress[] inetAddresses = event.getInfo().getInetAddresses();

            if (inetAddresses.length > 0) {
                NaoDetector.this.detectedRobots.removeIf(nao -> ((Nao) nao).getAddress().equals(inetAddresses[0]));
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            InetAddress[] inetAddresses = event.getInfo().getInetAddresses();

            if (inetAddresses.length > 0) {
                NaoDetector.this.detectedRobots.add(new Nao(event.getName(), inetAddresses[0]));
            }
        }
    }

    @Override
    public List<Robot> detectRobots() {
        return this.detectedRobots;
    }
}
