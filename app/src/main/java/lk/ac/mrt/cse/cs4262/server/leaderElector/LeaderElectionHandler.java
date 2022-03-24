package lk.ac.mrt.cse.cs4262.server.leaderElector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaderElectionHandler extends Thread{

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionHandler.class);

    private Thread currentThread = null;
    private final String event;
    private String coordinatingServerName;

    public LeaderElectionHandler(String event, String coordinatingServerName){
        this.event = event;
        this.coordinatingServerName = coordinatingServerName;
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        while(true){
            try {
                LeaderElector.getInstance()
                                .getLeaderElectorState()
                                .dispatchEvent(event, this);
            } catch (InterruptedException e) {
                log.error("thread {} interupted executing initial event {}", currentThread.getId(), event);
            }
            break;
        }
    }

    public synchronized void stopThread(){
        log.error("start thread {} interupted by thread {} executing initial event {}", currentThread.getId(), 
                    Thread.currentThread().getId(), event);
        currentThread.interrupt();
    }

    public Thread getCurrentThread() {
        return currentThread;
    }

    public void setCurrentThread(Thread currentThread) {
        this.currentThread = currentThread;
    }

    public String getEvent() {
        return event;
    }

    public String getCoordinatingServerName() {
        return coordinatingServerName;
    }

    public void setCoordinatingServerName(String coordinatingServerName) {
        this.coordinatingServerName = coordinatingServerName;
    }
    
}
