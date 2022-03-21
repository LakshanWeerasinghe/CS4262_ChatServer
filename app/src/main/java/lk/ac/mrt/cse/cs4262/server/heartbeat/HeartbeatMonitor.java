package lk.ac.mrt.cse.cs4262.server.heartbeat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class HeartbeatMonitor {

    private final String leaderName;

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

    private boolean iamLeader;

    private static HeartbeatMonitor instance;

    private HeartbeatMonitor() {
        leaderName = SystemState.getInstance().getLeader();

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
        for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
            if (!serverName.equals(leaderName)) {
                checkers.put(serverName, new Checker(false));
            }
        }
        failedServers = new ArrayList<>();
        maxPoolSize = SystemState.getInstance().getSystemConfigMap().size() + 5;
        threadPool = Executors.newFixedThreadPool(maxPoolSize);
        failureNotice.put("type", "failurenotice");

        iamLeader = true;
    }

    private void initializeSubordinateMonitor() {
        subordinateStarted = false;
        maxPoolSize = 5;
        threadPool = Executors.newFixedThreadPool(maxPoolSize);
        iamLeader = false;
    }

    public void startHeartbeatMonitor(String serverName) {
        if (serverName.equals(leaderName)) {
            initializeLeaderMonitor();
            startLeaderHeartbeat();
        } else {
            initializeSubordinateMonitor();
        }
    }

    private void startLeaderHeartbeat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
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
                            if (!serverName.equals(leaderName)) {
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
                                    e.printStackTrace();
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
                                e.printStackTrace();
                            }
                        }
                        boolean failureExists = false;
                        // update the system config
                        synchronized (checkerLock) {
                            boolean isActive;
                            for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                                if (!serverName.equals(leaderName)) {
                                    isActive = checkers.get(serverName).getValue();
                                    SystemState.getInstance().getSystemConfigMap().get(serverName)
                                            .setIsServerActive(isActive);
                                    if (!isActive) {
                                        failureExists = true;
                                        failedServers.add(serverName);
                                    }
                                }
                            }
                        }
                        // if failures exist, send notification to all subordinates about failure
                        if (failureExists) {
                            failureNotice.put("failed", failedServers);
                            String message = Util.getJsonString(failureNotice);
                            for (String serverName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                                if (!serverName.equals(leaderName)) {
                                    CoordinatorConnector cc;
                                    try {
                                        cc = new CoordinatorConnector(
                                                SystemState.getInstance().getIPOfServer(serverName),
                                                SystemState.getInstance().getCoordinatorPortOfServer(serverName),
                                                true);
                                        cc.sendMessage(message);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    try {
                        Thread.sleep(LEADER_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
        ;
    }

    public void acknowledge(String serverName) {
        System.out.println("acknowledge " + serverName);
        synchronized (checkerLock) {
            if (iamLeader) {
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
        // start monitoring thread only if has not started
        if (!subordinateStarted) {
            subordinateStarted = true;

            new Thread(new Runnable() {
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
                            e.printStackTrace();
                        }
                        // wait for the leader to reply
                        timeTick = 0;
                        shouldWait = true;
                        while (timeTick < MAX_TIMEOUT) {
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
                                e.printStackTrace();
                            }
                        }
                        // if leader is inactive, stop monitor and start leader election
                        if (!isLeaderActive) {
                            subordinateStarted = false;
                            System.out.println("Starting Leader Election");
                        }
                    }
                }
            }).start();
        }
    }

    public void executeMonitor() {
        if (!iamLeader) {
            executeSubordinateHeartbeat();
        }
    }

    public void updateFailedServers(List<String> failedList) {
        System.out.println("failure update");
        for (String f : failedList) {
            SystemState.getInstance().getSystemConfigMap().get(f)
                    .setIsServerActive(false);
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
}
