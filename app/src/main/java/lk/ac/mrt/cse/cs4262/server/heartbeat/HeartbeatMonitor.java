package lk.ac.mrt.cse.cs4262.server.heartbeat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.LeaderState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.state.NotLeaderState;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);

    private final Object configLock = new Object();
    private final Object checkerLock = new Object();
    private int serverChecks;

    private static final long MAX_TIMEOUT = 5;
    private int timeTick;
    private boolean shouldWait;
    private static final long LEADER_INTERVAL = 20000;
    private static final long SUBORDINATE_INTERVAL = 25000;

    private final String checkingMessage;
    private final Map<String, Object> failureNotice = new HashMap<>();
    private List<String> failedServers;

    private Map<String, Checker> checkers;

    private ExecutorService threadPool;
    private int maxPoolSize;

    private boolean subordinateStarted;
    private boolean isLeaderActive;

    private Thread subordinateHeatbeatMonitorThread = null;
    private Thread leaderHeatbeatMonitorThread = null;

    private static HeartbeatMonitor instance;

    private HeartbeatMonitor() {

        Map<String, Object> map = new HashMap<>();
        map.put("type", "heartbeatcheck");
        checkingMessage = Util.getJsonString(map);
    }

    public static HeartbeatMonitor getInstance() {
        if (instance == null) {
            instance = new HeartbeatMonitor();
        }
        return instance;
    }

    private void initializeLeaderMonitor() {
        checkers = new HashMap<>();
        for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()){
            if (!serverName.equals(SystemState.getInstance().getLeader())){
                checkers.put(serverName, new Checker(false));
            }
        }
        failedServers = new ArrayList<>();
        maxPoolSize = SystemState.getInstance().getSystemConfigMap().size() + 5;
        threadPool = Executors.newFixedThreadPool(maxPoolSize);
        failureNotice.put("type", "failurenotice");
    }

    private void initializeSubordinateMonitor() {
        subordinateStarted = false;
        maxPoolSize = 5;
        threadPool = Executors.newFixedThreadPool(maxPoolSize);
    }

    public void startHeartbeatMonitor(String serverName) {
        if (serverName.equals(SystemState.getInstance().getLeader())) {
            initializeLeaderMonitor();
            interuptSubordinateHeartBeatMonitorThread();
            startLeaderHeartbeat();
        } else {
            initializeSubordinateMonitor();
            executeMonitor();
        }
    }

    private void startLeaderHeartbeat() {
        log.info("begin heartbeat monitering as leader");
        leaderHeatbeatMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (LeaderElector.getInstance().getLeaderElectorState() instanceof LeaderState) {
                    failedServers.clear();
                    // block other threads requesting the config update
                    synchronized (configLock) {
                        // safely reset checking variables
                        synchronized (checkerLock) {
                            for (String serverName : checkers.keySet()) {
                                checkers.get(serverName).setValue(false);
                            }
                            serverChecks = 0;
                        }
                        // send heartbeat checking messages to all subordinate servers
                        for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                            if (!serverName.equals(SystemState.getInstance().getLeader()) &&
                                    SystemState.getInstance().getSystemConfigMap().get(serverName).getIsServerActive()) {
                                try {
                                    CoordinatorConnector cc = new CoordinatorConnector(
                                            SystemState.getInstance().getIPOfServer(serverName),
                                            SystemState.getInstance().getCoordinatorPortOfServer(serverName),
                                            true);
                                    threadPool.execute(cc);
                                    cc.sendMessage(checkingMessage);
                                    synchronized (checkerLock) {
                                        serverChecks++;
                                    }

                                } catch (IOException e) {
                                    log.error("cannot connect to subordinate server {} for heartbeat", serverName);
                                    log.error("error is {}", e.getMessage());
                                }
                            }
                        }
                        // wait until timeout till all subordinates reply
                        timeTick = 0;
                        shouldWait = true;
                        while (timeTick < MAX_TIMEOUT) {
                            synchronized (checkerLock) {
                                shouldWait = (serverChecks > 0);
                            }
                            if (!shouldWait) {
                                break;
                            }
                            try {
                                Thread.sleep(1000);
                                timeTick++;
                            } catch (InterruptedException e) {
                                log.error("subordinates heartbeat thread interupted while waiting for reply");
                                log.error("error is {}", e.getMessage());
                            }
                        }
                        boolean failureExists = false;
                        // update the system config
                        synchronized (checkerLock) {
                            boolean isActive;
                            for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                                if (!serverName.equals(SystemState.getInstance().getLeader())) {
                                    isActive = checkers.get(serverName).getValue();
                                    SystemState.getInstance().getSystemConfigMap().get(serverName)
                                            .setIsServerActive(isActive);
                                    if (!isActive) {
                                        failureExists = true;
                                        failedServers.add(serverName);
                                        checkers.remove(serverName);
                                        Store.getInstance().removeFaildServerDetails(serverName);
                                    }
                                }
                            }
                        }
                        // if failures exist, send notification to all subordinates about failure
                        if (failureExists) {
                            failureNotice.put("failed", failedServers);
                            String message = Util.getJsonString(failureNotice);
                            for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                                if (!serverName.equals(SystemState.getInstance().getLeader()) && 
                                    SystemState.getInstance().getSystemConfigMap().get(serverName).getIsServerActive()) {
                                    CoordinatorConnector cc;
                                    try {
                                        cc = new CoordinatorConnector(
                                                SystemState.getInstance().getIPOfServer(serverName),
                                                SystemState.getInstance().getCoordinatorPortOfServer(serverName),
                                                true);
                                        cc.sendMessage(message);
                                    } catch (IOException e) {
                                        log.error("cannot connect to subordinate server {} to update about heartbeat " 
                                                    + "faiuler", serverName);
                                        log.error("error is {}", e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                    try {
                        Thread.sleep(LEADER_INTERVAL);
                    } catch (InterruptedException e) {
                        log.error("subordinates heartbeat thread interupted while sleeping");
                        log.error("error is {}", e.getMessage());
                    }

                }
            }
        });
        leaderHeatbeatMonitorThread.start();
    }

    public void acknowledge(String serverName) {
        synchronized (checkerLock) {
            if (LeaderElector.getInstance().getLeaderElectorState() instanceof LeaderState) {
                checkers.get(serverName).setValue(true);
                serverChecks--;
            } else {
                isLeaderActive = true;
            }
        }
    }

    private class Checker {
        private boolean value;

        public Checker(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }
    }

    private void executeSubordinateHeartbeat() {
        log.info("begin heartbeat monitering as subordinate");

        // start monitoring thread only if has not started
        if (!subordinateStarted) {
            subordinateStarted = true;

            subordinateHeatbeatMonitorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (subordinateStarted) {
                        /*
                         * Sleep during the monitoring interval
                         * This is done at first because, leader is the one
                         * who initiates the subordinate
                         */
                        try {
                            Thread.sleep(SUBORDINATE_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // assume leader is inactive
                        synchronized (checkerLock) {
                            isLeaderActive = false;
                        }
                        // send checking message to leader
                        try {
                            CoordinatorConnector cc = new CoordinatorConnector(
                                    SystemState.getInstance().getLeaderConfig().getHostIp(),
                                    SystemState.getInstance().getLeaderConfig().getCoordinatorPort(),
                                    true);
                            threadPool.execute(cc);
                            cc.sendMessage(checkingMessage);
                        } catch (IOException e) {
                            log.error("cannot connect to leader server {} for heartbeat", 
                                        SystemState.getInstance().getLeaderConfig().getName());
                            log.error("error is {}", e.getMessage());
                        }

                        log.info("checking message sending finished");

                        // wait for the leader to reply
                        timeTick = 0;
                        shouldWait = true;
                        while (timeTick < MAX_TIMEOUT) {
                            log.info("wait until leader to reply");
                            synchronized (checkerLock) {
                                shouldWait = !isLeaderActive;
                            }
                            if (!shouldWait) {
                                break;
                            }
                            try {
                                Thread.sleep(1000);
                                timeTick++;
                            } catch (InterruptedException e) {
                                log.error("leader heartbeat thread interupted while waiting for reply");
                                log.error("error is {}", e.getMessage());
                            }
                        }
                        // if leader is inactive, stop monitor and start leader election
                        if (!isLeaderActive) {
                            subordinateStarted = false;
                            log.info("leader election started by server {}", Server.getInstance().getServerName());

                            SystemState.getInstance().getSystemConfigMap()
                                .get(SystemState.getInstance().getLeader()).setIsServerActive(false);
                            SystemState.getInstance().setLeader(null);
                            new LeaderElectionHandler(EventConstants.T1_EXPIRED, null).start();
                        }
                    }
                    interuptSubordinateHeartBeatMonitorThread();
                }
            });
            subordinateHeatbeatMonitorThread.start();
        }
    }

    public void executeMonitor() {
        if (LeaderElector.getInstance().getLeaderElectorState() instanceof NotLeaderState){
            executeSubordinateHeartbeat();
        }
    }

    public Map<String, Boolean> getConfigUpdate() {
        Map<String, Boolean> map = new HashMap<>();
        synchronized (configLock) {
            SystemState s = SystemState.getInstance();
            for (String serverName : s.getSystemConfigMap().keySet()) {
                map.put(serverName, s.getSystemConfigMap().get(serverName).getIsServerActive());
            }
        }
        if (!map.isEmpty()) {
            return map;
        }
        return null;
    }

    public boolean isSubordinateStarted() {
        return subordinateStarted;
    }

    public void setSubordinateStarted(boolean subordinateStarted) {
        this.subordinateStarted = subordinateStarted;
    }

    public void interuptSubordinateHeartBeatMonitorThread(){
        if(subordinateHeatbeatMonitorThread != null){
            log.info("interupt the subordinate heartbeat monitering thread");
            subordinateHeatbeatMonitorThread.interrupt();
            subordinateHeatbeatMonitorThread = null;
        }
    }

    public void interuptLeaderHeartBeatMonitorThread(){
        if(leaderHeatbeatMonitorThread != null){
            log.info("interupt the leader heartbeat monitering thread");
            leaderHeatbeatMonitorThread.interrupt();
            leaderHeatbeatMonitorThread = null;
        }
    }

    public void updateChecker(String serverName){
        if(LeaderElector.getInstance().getLeaderElectorState() instanceof LeaderState){
            synchronized(checkerLock){
                checkers.put(serverName, new Checker(true));
            }
        }
    }
    
}
