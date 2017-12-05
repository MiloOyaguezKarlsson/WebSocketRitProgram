/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package draw.websocket;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author milooyaguez karlsson
 */
@ServerEndpoint("/draw")
public class DrawServerEndpoint {

    static Set<Session> sessions = new HashSet<>();
    static String[] colors = {"red", "green", "blue", "yellow", "purple"};

    @OnOpen
    public void open(Session user) throws IOException {
        Random rnd = new Random();
        int rndNr = rnd.nextInt(5);

        user.getUserProperties().put("color", colors[rndNr]);
        user.getUserProperties().put("username", "unknown");
        sessions.add(user);

        user.getBasicRemote().sendText(buildUserData());
        for (Session session : sessions) {
            session.getBasicRemote().sendText(buildUserData());
        }
    }

    @OnClose
    public void close(Session user) throws IOException {
        sessions.remove(user);
        for (Session session : sessions) {
            session.getBasicRemote().sendText(buildUserData());
        }
    }

    @OnMessage
    public String onMessage(String message, Session user) throws IOException {
        JsonReader jsonReader = Json.createReader(new StringReader(message));
        JsonObject messageJson = jsonReader.readObject();
        jsonReader.close();
        if (messageJson.getString("type").equals("username")) {
            user.getUserProperties().replace("username", messageJson.getString("username"));
            for (Session session : sessions) {
                session.getBasicRemote().sendText(buildUserData());
            }
        } else if (messageJson.getString("type").equals("draw")) {
            for (Session session : sessions) {
                session.getBasicRemote().sendText(buildDrawData(user, message));
            }
        } else if(messageJson.getString("type").equals("message")){
            for (Session session : sessions) {
                String username = (String) user.getUserProperties().get("username");
                session.getBasicRemote().sendText(buildMessageData(username, messageJson.getString("message")));
            }
        }

        return null;
    }

    private String buildMessageData(String username, String message){
        String completeMessage = String.format("%s: %s", username, message);
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("type", "message")
                .add("message", completeMessage).build();

        return jsonObject.toString();
    }
    private String buildDrawData(Session user, String message) {
        JsonReader jsonReader = Json.createReader(new StringReader(message));
        JsonObject positions = jsonReader.readObject();
        jsonReader.close();
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("type", "draw")
                .add("username", user.getUserProperties().get("username").toString())
                .add("color", user.getUserProperties().get("color").toString())
                .add("x", positions.getInt("x"))
                .add("y", positions.getInt("y")).build();
        return jsonObject.toString();
    }
    private String buildUserData() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (Session session : sessions) {
            try {
                String username = (String) session.getUserProperties().get("username");
                String color = (String) session.getUserProperties().get("color");
                jsonArrayBuilder.add(Json.createObjectBuilder()
                        .add("type", "user")
                        .add("username", username)
                        .add("color", color)
                        .build());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        return jsonArrayBuilder.build().toString();
    }
}
