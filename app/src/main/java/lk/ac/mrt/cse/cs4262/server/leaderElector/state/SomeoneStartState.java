package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.EventConstants;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public class SomeoneStartState extends LeaderElectorState{

    public SomeoneStartState(LeaderElector leaderElector) {
        super(leaderElector);
    }

    private void waitForNominationOrCoordinationMessage(LeaderElectionHandler owner) throws InterruptedException{
        Thread.sleep(EventConstants.TIME_INTERVAL_T4);
        if(getLeaderElector().getLeaderElectorState() instanceof SomeoneStartState){
            dispatchEvent(EventConstants.T4_EXPIRED, owner);
        }
    }

    @Override
    public void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException {
        switch(event){
            case EventConstants.RECEIVE_NOMINATION:
                getLeaderElector().setLeaderElectorState(new LeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.SEND_COORDINATOR, owner);
                break;

            case EventConstants.T4_EXPIRED:
                getLeaderElector().setLeaderElectorState(new StartUpState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.START, owner);
                break;
            
            case EventConstants.RECEIVE_ELECTION:
                waitForNominationOrCoordinationMessage(owner);
                break;
            
            case EventConstants.RECEIVE_COORDINATOR:
                getLeaderElector().setLeaderElectorState(new NotLeaderState(getLeaderElector()), owner);
                getLeaderElector().getLeaderElectorState().dispatchEvent(EventConstants.RECEIVE_COORDINATOR, owner);
                break;

            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "SomeoneStartState State";
    }
    
}
