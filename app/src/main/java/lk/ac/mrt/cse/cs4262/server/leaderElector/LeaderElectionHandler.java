package lk.ac.mrt.cse.cs4262.server.leaderElector;

public class LeaderElectionHandler extends Thread{

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
                e.printStackTrace();
            }
        }
    }

    public synchronized void stopThread(){
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
