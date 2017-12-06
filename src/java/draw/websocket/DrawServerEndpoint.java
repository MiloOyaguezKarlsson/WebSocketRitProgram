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


@ServerEndpoint("/draw")
public class DrawServerEndpoint {

    static Set<Session> sessions = new HashSet<>();
    static String[] colors = {"red", "green", "blue", "yellow", "purple"};

    @OnOpen
    public void open(Session user) throws IOException {
        //random för att få en random färg från en array med 5 färger
        Random rnd = new Random();
        int rndNr = rnd.nextInt(5);

        user.getUserProperties().put("color", colors[rndNr]);
        //default användarnamn är unknown till användaren ändrar
        user.getUserProperties().put("username", "unknown"); 
        sessions.add(user);
        //välkommst meddelande
        user.getBasicRemote().sendText(buildMessageData("System", "Welcome!"));
        //skicka att användaren har kommit in och uppdatera användarlistan
        for (Session session : sessions) {
            if (session != user) { //ska bara skicka till alla ANDRA
                session.getBasicRemote().sendText(buildMessageData("System",
                        user.getUserProperties().get("username") + " has joined."));
            }
            session.getBasicRemote().sendText(buildUserData());
        }
    }

    @OnClose
    public void close(Session user) throws IOException {
        sessions.remove(user);
        //skicka utt meddelande till alla att användaren har lämnat
        //och uppdatera listan med användare
        for (Session session : sessions) {
            session.getBasicRemote().sendText(buildMessageData("System",
                    user.getUserProperties().get("username") + " has left."));
            session.getBasicRemote().sendText(buildUserData());
        }
    }

    @OnMessage
    public String onMessage(String message, Session user) throws IOException {
        //läsa in strängen och göra om det till ett jsonObjekt
        JsonReader jsonReader = Json.createReader(new StringReader(message));
        JsonObject messageJson = jsonReader.readObject();
        jsonReader.close();
        //göra olika saker med meddelandet beroende på vilken typ a meddelande det är
        //om det är användarnamn-ändring ska användarnamnet ändras och 
        //listan av användare ska uppdateras och skickas ut igen
        if (messageJson.getString("type").equals("username")) {
            //ändra användarnamnet
            user.getUserProperties().replace("username", messageJson.getString("username"));
            for (Session session : sessions) {
                session.getBasicRemote().sendText(buildUserData());
            }
        } else if (messageJson.getString("type").equals("draw")) { //rita-meddelanden
            for (Session session : sessions) {
                session.getBasicRemote().sendText(buildDrawData(user, message));
            }
        } else if (messageJson.getString("type").equals("message")) { //text-meddelanden
            for (Session session : sessions) {
                String username = (String) user.getUserProperties().get("username"); //vem som skriver
                session.getBasicRemote().sendText(buildMessageData(username, messageJson.getString("message")));
            }
        }

        return null;
    }

    //funktion för att bygga meddelanden som jsonobjekt
    private String buildMessageData(String username, String message) {
        String completeMessage = String.format("%s: %s", username, message);
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("type", "message")
                .add("message", completeMessage).build();

        return jsonObject.toString();
    }

    //funktion för att bygga ett jsonobject som representerar en punkt som ska ritas ut av klienten
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

    //funktion för att bygga en array med jsonobjekt som är alla användare/sessioner
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
