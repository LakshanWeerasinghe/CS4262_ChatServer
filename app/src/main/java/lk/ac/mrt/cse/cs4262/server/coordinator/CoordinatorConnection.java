package lk.ac.mrt.cse.cs4262.server.coordinator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lk.ac.mrt.cse.cs4262.server.Server;

public class CoordinatorConnection implements Runnable{
    
    private static final Logger log = LoggerFactory.getLogger(CoordinatorConnection.class);
    
    private final Socket coordinatorSocket;
    private BufferedReader coordinatorInputBuffer;
    private DataOutputStream coordinatorOutputBuffer;
    private Gson gson;
    
    public CoordinatorConnection(Socket coordinatorSocket, Server server){
        this.coordinatorSocket = coordinatorSocket;
        createInputBuffer(coordinatorSocket);
        createOutputBuffer(coordinatorSocket);
        this.gson = new Gson();
    }

    private void createInputBuffer(Socket coordinatorSocket){
        log.info("create coordinator input buffer for {}", coordinatorSocket);
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.coordinatorSocket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        log.info("create coordinator output buffer for {}", coordinatorSocket);
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.coordinatorSocket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (this.coordinatorSocket.isConnected()) {
                String bufferedMessage = this.coordinatorInputBuffer.readLine();
                

                if (this.gson == null) {
                    this.gson = new Gson();
                }

                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);

                    String messageType = jsonObject.get("type").getAsString();
                    System.out.println(jsonObject);

                    Map<String, Object> map = new HashMap<>();
                    switch (messageType) {

                        default:
                            break;
                    }

                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            try {
                coordinatorSocket.close();
            } catch (IOException e) {
                log.error("error is {}", e.getMessage());
            }
        }
    }


    public void send(String value) {
        log.info("send a message to coordinator message is {}", value);
        try {
            coordinatorOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

}
