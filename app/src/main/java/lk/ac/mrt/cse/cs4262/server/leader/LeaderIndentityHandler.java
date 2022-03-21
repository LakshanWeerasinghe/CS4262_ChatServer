package lk.ac.mrt.cse.cs4262.server.leader;

import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderIndentityHandler {

    private ExecutorService threadPool;
    private static LeaderIndentityHandler instance;
    private static final int MAX_THREADS = 20;
    private Boolean identityExists = false;

    private LeaderIndentityHandler() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public static synchronized LeaderIndentityHandler getInstance() {
        if (instance == null)
            instance = new LeaderIndentityHandler();
        return instance;
    }

    // ask whether the identity exist from other servers
    public Boolean askIndentityExist(String identity) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "checkidentityexist");
        map.put("identity", identity);

        SystemState s = SystemState.getInstance();
        String leaderServer = s.getLeader();

        for (String otherServerName : SystemState.getInstance().getSystemConfigMap().keySet()) {
            if (!(otherServerName.equals(leaderServer))) {
                threadPool.execute(new Runnable() {
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
                                identityExists = b.booleanValue();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                });

            }
        }
        return identityExists;

    }
}
