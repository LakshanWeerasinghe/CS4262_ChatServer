package lk.ac.mrt.cse.cs4262.server.leader;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderIndentityHandler {

    private static LeaderIndentityHandler instance;
    private final Map<String, Boolean> identityExistanceMap = new HashMap<>();

    private LeaderIndentityHandler() { }

    public static synchronized LeaderIndentityHandler getInstance() {
        if (instance == null)
            instance = new LeaderIndentityHandler();
        return instance;
    }

    // ask whether the identity exist from other servers
    public Boolean askIndentityExist(String identity, String originalRequestedServerName) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "checkidentityexist");
        map.put("identity", identity);
        map.put("serverid", originalRequestedServerName);
        SystemState s = SystemState.getInstance();
        String leaderServer = s.getLeader();

        String threadId = String.valueOf(Thread.currentThread().getId());
        identityExistanceMap.put(threadId, false);

        List<Thread> threads = new ArrayList<Thread>();

        for (String otherServerName : SystemState.getInstance().getSystemConfigMap().keySet()) {
            if (!(otherServerName.equals(leaderServer)) && !((otherServerName.equals(originalRequestedServerName)))) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CoordinatorConnector cc = new CoordinatorConnector(
                                    s.getIPOfServer(otherServerName),
                                    s.getCoordinatorPortOfServer(otherServerName),
                                    true
                            );
                            cc.sendMessage(Util.getJsonString(map));

                            Boolean b = (Boolean) cc.handleMessage().get("exist");
                            if (b.booleanValue()) {
                                identityExistanceMap.put(threadId, true);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                });
                threads.add(t);
                t.start();
            }
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Boolean identityExists = identityExistanceMap.get(threadId);
        identityExistanceMap.remove(threadId);
        return identityExists;

    }
}
