package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.SystemState;
import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class NotLeaderState extends LeaderElectorState{

    public NotLeaderState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    private void updateLeader(LeaderElectionHandler owner){
        SystemState.getInstance().setLeader(owner.getCoordinatingServerName());
        SystemState.getInstance().updateLeaderElectionStatus(true);
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_ELECTION:
                getLeaderElector().setLeaderElectorState(new SomeoneStartState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_ELECTION, owner);
                break;

            case EventConstants.RECEIVE_COORDINATOR:
                updateLeader(owner);
                Server.getInstance().startHeartbeatMonitor();
                break;

            case EventConstants.T1_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START, owner);
                break;

            default:
                break;
            
        }
        
    }
    
}
