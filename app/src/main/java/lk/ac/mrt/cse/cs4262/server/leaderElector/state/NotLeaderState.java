package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.heartbeat.HeartbeatMonitor;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class NotLeaderState extends LeaderElectorState{

    public NotLeaderState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION, owner);
                break;

            case EventConstants.RECEIVE_COORDINATOR:
                SystemState.getInstance().setLeader(owner.getCoordinatingServerName());
                HeartbeatMonitor.getInstance().startHeartbeatMonitor(Server.getInstance().getServerName());
                break;

            case EventConstants.T1_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START, owner);
                break;

            case EventConstants.RECOVER_AS_NOT_LEADER:
                HeartbeatMonitor.getInstance().startHeartbeatMonitor(Server.getInstance().getServerName());
                break;

            default:
                break;
            
        }
        
    }

    @Override
    public String toString() {
        return "Not Leader State";
    }
    
}
