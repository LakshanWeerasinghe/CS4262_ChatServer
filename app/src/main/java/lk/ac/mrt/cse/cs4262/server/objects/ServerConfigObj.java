package lk.ac.mrt.cse.cs4262.server.objects;

public class ServerConfigObj {
    
    private final String name;
    private final String hostIp;
    private final Integer clientPort;
    private final Integer coordinatorPort;
    private Boolean isServerActive;

    public ServerConfigObj(String name, String hostIp, String clientPort, String coordinatorPort){
        this.name = name;
        this.hostIp = hostIp;
        this.clientPort = Integer.valueOf(clientPort);
        this.coordinatorPort = Integer.valueOf(coordinatorPort);
        this.isServerActive = false;
    }

    public String getName() {
        return name;
    }

    public String getHostIp() {
        return hostIp;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public Integer getCoordinatorPort() {
        return coordinatorPort;
    }


    public Boolean getIsServerActive() {
        return isServerActive;
    }

    public synchronized void setIsServerActive(Boolean isServerActive) {
        this.isServerActive = isServerActive;
    }

    @Override
    public String toString() {
        return "ServerConfigObj [clientPort=" + clientPort + ", coordinatorPort=" + coordinatorPort + ", hostIp="
                + hostIp + ", isServerActive=" + isServerActive + ", name=" + name + "]";
    }

    
}
