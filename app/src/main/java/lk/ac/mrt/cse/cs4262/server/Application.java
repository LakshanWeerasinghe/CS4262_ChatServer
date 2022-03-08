package lk.ac.mrt.cse.cs4262.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.chatRoom.MainHall;
import lk.ac.mrt.cse.cs4262.server.client.command.CreateRoomHandler;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;
import lk.ac.mrt.cse.cs4262.server.coordinator.CoordinatorConnector;
import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;
import lk.ac.mrt.cse.cs4262.server.startup.ServerStartUpThread;
import lk.ac.mrt.cse.cs4262.server.util.ConfigUtil;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args){
        log.info("application starting...");

        if(args.length != 2 ){
            log.error("need to pass two arguments <Server-Identifier> <Config-File-Path>");
            System.exit(1);
        }

        String serverName = args[0];
        String configFilePath = args[1];
        String mainHallName = "MainHall-" + serverName;
        Map<String, ServerConfigObj> serverConfigMap = null;
        Properties properties = null;

        try {
            properties = ConfigUtil.loadProperties();
        } catch (IOException e) {
            log.error("error occred while loading properties file");
            log.error("error is {}", e.getMessage());
            System.exit(1);
        }

        try {
            serverConfigMap = ConfigUtil.loadSystemConfig(configFilePath);
        } catch (FileNotFoundException e) {
            log.error("error occred while loading config file in path at {}", configFilePath);
            log.error("error is {}", e.getMessage());
            System.exit(1);
        }

        Server server = new Server(serverName)
                            .createCoordinatorServerSocket(serverConfigMap.get(serverName).getCoordinatorPort())
                            .createClientHandlerServerSocket(serverConfigMap.get(serverName).getClientPort());

        serverConfigMap.get(serverName).setIsServerActive(true);
        SystemState systemState = SystemState.getInstance();
        systemState.setLeader(properties.getProperty("leader"));
        systemState.setSystemConfigMap(serverConfigMap);

        server.setSystemState(SystemState.getInstance());
        server.setStore(Store.getInstance());
        server.setMainHall(MainHall.getInstance(mainHallName, null));
        server.setNewIdentityHandler(new NewIdentityHandler(Store.getInstance(),
                                        MainHall.getInstance(mainHallName, null)));
        server.setCreateRoomHandler(new CreateRoomHandler(Store.getInstance()));
        server.startListenOnCoordinatorSocket();

        for (String otherServerName : serverConfigMap.keySet()) {
            if(otherServerName != serverName){
                new Thread(new ServerStartUpThread(serverConfigMap.get(otherServerName))).start();
            }
        }

        server.startListenOnClientSocket();
    }
    
}
