package de.fhg.iais.roberta;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.usb.RobotSearchTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class RobotSearchTaskTests {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        this.executorService.shutdown();
    }

    @Test
    void RobotSearchTask_ShouldReturnFoundConnector_WhenConnectorIsFound() {
        List<IConnector> connectorList = new ArrayList<>();
        connectorList.add(new TestNotFoundConnector());
        connectorList.add(new TestFoundConnector());

        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, (observable, o) -> {
        }, new Observable()));

        IConnector connector = null;
        try {
            connector = robotSearchFuture.get();
        } catch ( InterruptedException | ExecutionException e ) {
            // do nothing
        }

        assertThat(connector, is(equalTo(connectorList.get(1))));
    }

    @Test
    void RobotSearchTask_ShouldReturnSecondFoundConnector_WhenMultipleConnectorsAreFound() {
        List<IConnector> connectorList = new ArrayList<>();
        connectorList.add(new TestNotFoundConnector());
        connectorList.add(new TestFoundConnector());
        connectorList.add(new TestFoundConnector());

        TestObserverObservable testObserverObservable = new TestObserverObservable();
        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, testObserverObservable, testObserverObservable));

        IConnector connector = null;
        try {
            connector = robotSearchFuture.get();
        } catch ( InterruptedException | ExecutionException e ) {
            // do nothing
        }

        assertThat(connector, is(equalTo(connectorList.get(2))));
    }

    @Test
    void RobotSearchTask_ShouldKeepSearching_WhenNoConnectorIsFound() {
        List<IConnector> connectorList = new ArrayList<>();
        connectorList.add(new TestNotFoundConnector());
        connectorList.add(new TestNotFoundConnector());
        connectorList.add(new TestNotFoundConnector());

        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, (observable, o) -> {
        }, new Observable()));

        IConnector connector = null;
        try {
            connector = robotSearchFuture.get(100, TimeUnit.MILLISECONDS);
        } catch ( InterruptedException | ExecutionException | TimeoutException e) {
            // do nothing
        }

        assertThat(connector, is(nullValue()));
    }

    private static class TestObserverObservable extends Observable implements Observer {
        @Override
        public void update(Observable observable, Object o) {
            if ( observable instanceof RobotSearchTask ) {
                if ( o instanceof List ) {
                    List<?> connectors = (List<?>) o;

                    setChanged();
                    notifyObservers(connectors.get(1));
                }
            }
        }
    }

    private static class TestFoundConnector extends TestConnector {
        @Override
        public boolean findRobot() {
            return true;
        }
    }

    private static class TestNotFoundConnector extends TestConnector {
        @Override
        public boolean findRobot() {
            return false;
        }
    }

    private abstract static class TestConnector implements IConnector {
        @Override
        public void userPressConnectButton() {

        }

        @Override
        public void userPressDisconnectButton() {

        }

        @Override
        public void close() {

        }

        @Override
        public void notifyConnectionStateChanged(State state) {

        }

        @Override
        public String getToken() {
            return null;
        }

        @Override
        public String getBrickName() {
            return null;
        }

        @Override
        public void update() {

        }

        @Override
        public void updateCustomServerAddress(String customServerAddress) {

        }

        @Override
        public void resetToDefaultServerAddress() {

        }

        @Override
        public Boolean call() throws Exception {
            return null;
        }
    }
}
