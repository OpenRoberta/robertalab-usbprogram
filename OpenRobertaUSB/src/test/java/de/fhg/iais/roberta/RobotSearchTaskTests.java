package de.fhg.iais.roberta;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.usb.RobotSearchTask;
import de.fhg.iais.roberta.util.AbstractTestConnector;
import de.fhg.iais.roberta.util.ORAListener;
import de.fhg.iais.roberta.util.TestListenable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, object -> {
        }, new TestListenable<>()));

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

        TestListenerListenable testListenerListenable = new TestListenerListenable();
        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, testListenerListenable, testListenerListenable));

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

        Future<IConnector> robotSearchFuture = this.executorService.submit(new RobotSearchTask(connectorList, object -> {}, new TestListenable<>()));

        IConnector connector = null;
        try {
            connector = robotSearchFuture.get(100, TimeUnit.MILLISECONDS);
        } catch ( InterruptedException | ExecutionException | TimeoutException e) {
            // do nothing
        }

        assertThat(connector, is(nullValue()));
    }

    private static class TestFoundConnector extends AbstractTestConnector {
        @Override
        public boolean findRobot() {
            return true;
        }
    }

    private static class TestNotFoundConnector extends AbstractTestConnector {
        @Override
        public boolean findRobot() {
            return false;
        }
    }

    private static class TestListenerListenable extends TestListenable<IConnector> implements ORAListener<List<IConnector>> {
        @Override
        public void update(List<IConnector> object) {
            fire(object.get(1));
        }
    }
}
