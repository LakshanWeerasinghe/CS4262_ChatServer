package lk.ac.mrt.cse.cs4262.server.client;

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

import lk.ac.mrt.cse.cs4262.server.Server;
import lk.ac.mrt.cse.cs4262.server.chatRoom.Room;
import lk.ac.mrt.cse.cs4262.server.client.command.NewIdentityHandler;
import lk.ac.mrt.cse.cs4262.server.util.Util;

public class Client implements Runnable {

    private Socket clientSocket;
    private BufferedReader clientInputBuffer;
    private DataOutputStream clientOutputBuffer;
    private Gson gson;
    private NewIdentityHandler newIdentityHandler;
    private String clientIdentifier;
    private Room room;

    public Client(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        try {
            this.clientInputBuffer = new BufferedReader(new InputStreamReader(
                    this.clientSocket.getInputStream(), "utf-8"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.clientOutputBuffer = new DataOutputStream(this.clientSocket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.gson = new Gson();
        this.newIdentityHandler = server.getNewIdentityHandler();
    }

    @Override
    public void run() {

        try {
            while (true) {
                String bufferedMessage = this.clientInputBuffer.readLine();

                if (this.gson == null) {
                    this.gson = new Gson();
                }

                if (bufferedMessage != null) {
                    JsonObject jsonObject = this.gson.fromJson(bufferedMessage, JsonObject.class);

                    String messageType = jsonObject.get("type").getAsString();
                    String response = null;
                    System.out.println(jsonObject);

                    Map<String, String> map = new HashMap<>();
                    switch (messageType) {
                        case "newidentity":
                            String identity = jsonObject.get("identity").getAsString();
                            response = newIdentityHandler.handleNewIdentity(identity, Client.this);
                            send(response);

                            if (this.room != null) {
                                map.put("type", "roomchange");
                                map.put("identity", identity);
                                map.put("former", "");
                                map.put("roomid", this.room.getRoomName());
                                String message = Util.getJsonString(map);

                                room.broadcast(Client.this, message);
                                send(message);
                            }
                            break;

                        case "message":
                            map.put("type", "message");
                            map.put("identity", this.clientIdentifier);
                            map.put("content", jsonObject.get("content").getAsString());
                            room.broadcast(Client.this, Util.getJsonString(map));
                            break;

                        default:
                            break;
                    }

                }
                // detect command or message
                // handle the messge accordingly
            }
        } catch (SocketException e) {
            System.out.println("Client connection interupted");
            // remove the client name from the store
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void send(String value) {
        try {
            clientOutputBuffer.write((value + "\n").getBytes("UTF-8"));
            clientOutputBuffer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
    }

    public void setClientIdentifier(String value) {
        this.clientIdentifier = value;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Room getRoom() {
        return room;
    }
}
