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

public class CoordinatorConnector implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(CoordinatorConnector.class);

    private Socket socket;
    private BufferedReader coordinatorInputBuffer;
    private DataOutputStream coordinatorOutputBuffer;
    private Gson gson;

    public CoordinatorConnector(String ip, int port) throws IOException{
        log.info("start to make a connection with server on ip {} port {}", ip, port);
        this.socket = new Socket(ip, port);
        log.info("successfully connected to the server on ip {} port {}", ip, port);
    }

    public CoordinatorConnector createInputOutputBuffers(){
        createInputBuffer(socket);
        createOutputBuffer(socket);
        return this;
    }

    private void createInputBuffer(Socket coordinatorSocket){
        log.info("create coordinator input buffer for {}", coordinatorSocket);
        try {
            this.coordinatorInputBuffer = new BufferedReader(new InputStreamReader(
                    this.socket.getInputStream(), "utf-8"));
        } catch (IOException e) {
            log.error("error occored while creating input buffer");
            log.error("error is {}", e.getMessage());
        }
    }

    private void createOutputBuffer(Socket coordinatorSocket){
        log.info("create coordinator output buffer for {}", coordinatorSocket);
        try {
            this.coordinatorOutputBuffer = new DataOutputStream(this.socket.getOutputStream());
        } catch (Exception e) {
            log.error("error occored while creating output buffer");
            log.error("error is {}", e.getMessage());
        }
    }
    

    public void sendMessage(String value) {
        log.info("send a message to coordinator message is {}", value);
        try {
            coordinatorOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            coordinatorOutputBuffer.flush();
        } catch (IOException e) {
           log.error("error occred while sending the message");
           log.error("error is {}", e.getMessage());
        }
    }

  
    @Override
    public void run() {
        log.info("start to listen for coordinator connections");
        handleMessage();
    }

    public Map<String, Object> handleMessage(){
        try {
            while (this.socket.isConnected()) {
                String bufferedMessage = CoordinatorConnector.this.coordinatorInputBuffer.readLine();
        
                if (this.gson == null) {
                    this.gson = new Gson();
                }
        
                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);
        
                    String messageType = jsonObject.get("type").getAsString();
        
                    Map<String, Object> map = new HashMap<>();
                    switch (messageType) {
                       
                        default:
                            break;
                    }
                    return map;
                }
            }
        } catch (SocketException e) {
            log.error("error socket connection interupted");
            log.error("error is {}", e.getMessage());
        } catch (IOException e) {
            log.error("error is {}", e.getMessage());
        }finally{
            try {
                socket.close();
            } catch (IOException e) {
                log.error("error is {}", e.getMessage());
            }
        }
        return null;
    }
}
