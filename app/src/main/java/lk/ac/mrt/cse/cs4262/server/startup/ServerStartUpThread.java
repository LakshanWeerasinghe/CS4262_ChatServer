package lk.ac.mrt.cse.cs4262.server.startup;

import java.io.IOException;
import java.net.Socket;

import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class ServerStartUpThread implements Runnable{

    private final ServerConfigObj serverConfigObj;

    public ServerStartUpThread(ServerConfigObj serverConfigObj){
        this.serverConfigObj = serverConfigObj;
    }

    @Override
    public void run() {
        while(true){
            try {
                Socket socket = new Socket(serverConfigObj.getHostIp(), serverConfigObj.getCoordinatorPort());
                serverConfigObj.setIsServerActive(true);
                socket.close();
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }   
        }
       
    }
    
}
