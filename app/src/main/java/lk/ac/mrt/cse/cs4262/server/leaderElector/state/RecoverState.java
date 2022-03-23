package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;
import lk.ac.mrt.cse.cs4262.server.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecoverState extends LeaderElectorState{

    private static final Logger log = LoggerFactory.getLogger(ChoosingState.class);

    private Set<String> liveServerNames = new HashSet<>();
    private Integer myPriority = -1;
    private Integer highestPriority = -1;
    private String highestPriorityServer;

    public RecoverState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    private void sendIamUpMsg(LeaderElectionHandler owner) throws InterruptedException {

        SystemState.getInstance().getSystemConfigMap().values().forEach(
                x -> {
                    if(!x.getName().equals(Server.getInstance().getServerName())){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    CoordinatorConnector serverConnector =
                                            new CoordinatorConnector(x.getHostIp(), x.getCoordinatorPort(), true);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("type", "iamup");
                                    map.put("serverid", Server.getInstance().getServerName());
                                    serverConnector.sendMessage(Util.getJsonString(map));
    
                                    List<String> liveServerNameList = (List<String>) serverConnector
                                                                        .handleMessage().get("liveServerNames");
                                    liveServerNames.addAll(liveServerNameList);
                                } catch (IOException e) {
                                    log.error("error sending iamup msg to server {}", x.getName());
                                    log.error("error is {}", e.getMessage());
                                }
                            }
                        }).start();
                    }
                }
        );

        Thread.sleep(EventConstants.TIME_INTERVAL_T2);
        if (liveServerNames.size() == 0) {
            // no view messages received
            dispatchEvent(EventConstants.T2_EXPIRED, owner);
        } else {
            handleViewMessages(liveServerNames, owner);
        }
    }

    private void handleViewMessages(Set<String> currentLiveServers, LeaderElectionHandler owner) 
                                                                                        throws InterruptedException {
        
        currentLiveServers.forEach(x -> {
            SystemState.getInstance().getSystemConfigMap().get(x).setIsServerActive(true);
            if(SystemState.getInstance().getSystemConfigMap().get(x).getPriority() > highestPriority){
                highestPriority = SystemState.getInstance().getSystemConfigMap().get(x).getPriority();
                highestPriorityServer = x;
            }
        });

        if (highestPriority > myPriority) {
            // you are not the coordinator
            getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECOVER_AS_NOT_LEADER, owner);
        } else {
            // you are the coordinator
            getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECOVER_AS_LEADER, owner);
        }
    }

    private void makeOtherServersInactive() {
        SystemState.getInstance().getSystemConfigMap().values().forEach(
                x -> {
                    if (!(x.getName() != Server.getInstance().getServerName())) {
                        x.setIsServerActive(false);
                    }
                }
        );
    }

    private void makeYourselfAlive() {
        SystemState.getInstance().getSystemConfigMap().get(Server.getInstance()
                    .getServerName()).setIsServerActive(true);
        myPriority = SystemState.getInstance().getSystemConfigMap().get(Server.getInstance()
                    .getServerName()).getPriority();
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.START_RECOVERY:
                makeYourselfAlive();
                sendIamUpMsg(owner);
                break;

            case EventConstants.T2_EXPIRED:
                makeOtherServersInactive();
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()), owner);
                break;

            case EventConstants.RECOVER_AS_LEADER:
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.SEND_COORDINATOR, owner);
                break;

            case EventConstants.RECOVER_AS_NOT_LEADER:
                SystemState.getInstance().setLeader(highestPriorityServer);
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECOVER_AS_NOT_LEADER, owner);
                break;

            default:
                break;
        }

    }

    @Override
    public String toString() {
        return "Recover State";
    }
    
}
