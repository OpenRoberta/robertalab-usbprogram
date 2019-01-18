package de.fhg.iais.roberta.connection;

import com.fazecast.jSerialComm.SerialPort;
import de.fhg.iais.roberta.util.ORAListenable;
import de.fhg.iais.roberta.util.ORAListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

public class SerialLoggingTask implements Callable<Void>, ORAListenable<byte[]> {
    private static final Logger LOG = LoggerFactory.getLogger(SerialLoggingTask.class);

    private final Collection<ORAListener<byte[]>> listeners = new ArrayList<>();

    private final String port;
    private final int serialRate;

    public SerialLoggingTask(ORAListener<byte[]> listener, String port, int serialRate) {
        registerListener(listener);
        this.port = port;
        this.serialRate = serialRate;
    }

    // https://github.com/Fazecast/jSerialComm/wiki/Nonblocking-Reading-Usage-Example
    @Override
    public Void call() {
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        SerialPort comPort = Arrays.stream(serialPorts).filter(serialPort -> serialPort.getSystemPortName().contains(this.port)).findFirst().get();
        comPort.setBaudRate(this.serialRate);
        comPort.openPort(0);
        LOG.info("SerialPort {} {} {} opened, logging with baud rate of {}", comPort.getSystemPortName(), comPort.getDescriptivePortName(), comPort.getPortDescription(), comPort.getBaudRate());
        while(!Thread.currentThread().isInterrupted()) {
            try {
                while (comPort.bytesAvailable() == 0) {
                    Thread.sleep(200);
                }

                byte[] readBuffer = new byte[comPort.bytesAvailable()];
                comPort.readBytes(readBuffer, readBuffer.length);
                fire(readBuffer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        comPort.closePort();
        return null;
    }

    @Override
    public void registerListener(ORAListener<byte[]> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(ORAListener<byte[]> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(byte[] object) {
        for ( ORAListener<byte[]> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
