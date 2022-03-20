package lk.ac.mrt.cse.cs4262.server.leaderElector.state;

import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElectionHandler;
import lk.ac.mrt.cse.cs4262.server.leaderElector.LeaderElector;

public abstract class LeaderElectorState {
    
    private LeaderElector leaderElector;

    public LeaderElectorState(LeaderElector leaderElector){
        this.leaderElector= leaderElector;
    }

    protected LeaderElector getLeaderElector() {
        return leaderElector;
    }
    
    public abstract void dispatchEvent(String event, LeaderElectionHandler owner) throws InterruptedException;
}
