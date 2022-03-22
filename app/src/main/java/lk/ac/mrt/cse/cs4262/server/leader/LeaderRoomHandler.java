package lk.ac.mrt.cse.cs4262.server.leader;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.ac.mrt.cse.cs4262.server.Store;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class LeaderRoomHandler {

    private ExecutorService threadPool;
    private static LeaderRoomHandler instance;
    private static final int MAX_THREADS = 20;

    private LeaderRoomHandler() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public static synchronized LeaderRoomHandler getInstance() {
        if (instance == null)
            instance = new LeaderRoomHandler();
        return instance;
    }

    public boolean handleCreateRoom(String roomID, String serverName) {
        // first check whether all servers a live as known locally
        if (SystemState.getInstance().allServerActive()) {
            // check whether all servers are actually live at the moment
            boolean allActive = true;
            SystemState s = SystemState.getInstance();
            String leaderServer = s.getLeader();

            for (String otherServerName : SystemState.getInstance().getSystemConfigMap().keySet()) {
                if (!(otherServerName.equals(leaderServer) || otherServerName.equals(serverName))) {
                    try {
                        Socket socket = new Socket(
                                s.getIPOfServer(otherServerName),
                                s.getCoordinatorPortOfServer(otherServerName));
                        socket.close();
                    } catch (IOException e) {                        
                        allActive = false;
                    }
                }
            }
            if (allActive) {
                if (!Store.getInstance().roomIDExist(roomID)) {
                    Store.getInstance().addRoom(roomID, serverName);
                    return false;
                }
                Store.getInstance().removeRoomIDFromTmp(roomID);
                return true;
            }
        }
        return true;
    }

    public void informAboutNewRoom(String roomID, String serverName) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "roomcreate");
        map.put("roomid", roomID);
        map.put("serverid", serverName);

        SystemState s = SystemState.getInstance();
        String leaderServer = s.getLeader();

        for (String otherServerName : SystemState.getInstance().getSystemConfigMap().keySet()) {
            if (!(otherServerName.equals(leaderServer) || otherServerName.equals(serverName))) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CoordinatorConnector cc = new CoordinatorConnector(
                                    s.getIPOfServer(otherServerName),
                                    s.getCoordinatorPortOfServer(otherServerName),
                                    true);
                            cc.sendMessage(Util.getJsonString(map));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                });

            }
        }

    }

    public void informAboutDeleteRoom(String roomID, String serverName) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "deleteroom");
        map.put("serverid", serverName);
        map.put("roomid", roomID);

        SystemState s = SystemState.getInstance();
        String leaderServer = s.getLeader();

        for (String otherServerName : SystemState.getInstance().getSystemConfigMap().keySet()) {
            if (!(otherServerName.equals(leaderServer) || otherServerName.equals(serverName))) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CoordinatorConnector cc = new CoordinatorConnector(
                                    s.getIPOfServer(otherServerName),
                                    s.getCoordinatorPortOfServer(otherServerName),
                                    true);
                            cc.sendMessage(Util.getJsonString(map));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                });

            }
        }
    }

}
