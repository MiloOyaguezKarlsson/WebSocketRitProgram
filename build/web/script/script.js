var x = 0, y = 0; //variabler för musens koordinater
var canvas, ctx, height, width; //canvas, context, canvas bredd och höjd initieras i onload-funktionen
var drawing = false; //håller koll på om man ritar nu
var drawDot = false; //för att rita en punkt om man bara enkelklickar
var url = "ws://localhost:8080/WebSocketRitProgram/draw";

//ville göra detta i samband med att ett användarnamn skrevs in men fick då
// problem men att användarnamnet skickades innan uppkopplingen fanns
var websocket = new WebSocket(url); 

window.onload = function () {
    canvas = document.getElementById("canvas");
    ctx = canvas.getContext("2d");
    width = canvas.width;
    height = canvas.height;

    canvas.addEventListener("mousemove", function (e) { //rita
        findxy("move", e);
    }, false);
    canvas.addEventListener("mousedown", function (e) { //börja rita
        findxy("down", e);
    }, false);
    canvas.addEventListener("mouseup", function (e) { //sluta rita
        findxy("up", e);
    }, false);
    canvas.addEventListener("mouseout", function (e) { //om musen går utanför canvasen
        findxy("out", e);
    }, false);

    document.getElementById("submitUsernameBtn").addEventListener("click", function () {
        var username = document.getElementById("usernameInput").value;
        sendUsername(username);
        document.getElementById("usernameInput").value = "";
    });
    //detta funkar inte av någon anledning men onclick på button-elementet gör
    //det funkar även på knappen som skickar in användarnamnet
    document.getElementById("submitMessageBtn").addEventListener("click", sendMessage());
};
//funktion för att rita
//ritar en iffyld cirkel i en specifik färg vid den koordinaten som tas emot
function draw(color, x, y) {
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.arc(x, y, 10, 0, Math.PI * 2, false);
    ctx.fill();
}
//hämtar musens koordinater och gör lite olika saker beroende på vad som görs med musen
function findxy(event, e) {
    if (event === "down") {
        x = e.clientX - canvas.offsetLeft;
        y = e.clientY - canvas.offsetTop;
        drawing = true; //börja rita
    }
    //om musen går utanför canvasen eller slutas hållas ner så ska man inte rita mer
    if (event === "up" || event === "out") {
        drawing = false;
    }
    //när musen rör på sig så ska det ritas om "drawing" är sant
    if (event === "move") {
        if (drawing) {
            x = e.clientX - canvas.offsetLeft;
            y = e.clientY - canvas.offsetTop;
            sendDrawCoord();
        }
    }
}
//funktion för att bara mata in meddelanden i meddelanderutan
function updateMessages(message){
    document.getElementById("messageArea").value += message + "\n";
}
//funktion för att skicka in koordinater som ska ritas
function sendDrawCoord() {
    var jsonData = {type: "draw", x: x, y: y};
    websocket.send(JSON.stringify(jsonData));
}
//funktion för att skicka in ett användarnamn
function sendUsername(username) {
    var jsonData = {type: "username", username: username};
    websocket.send(JSON.stringify(jsonData));
}
//funktion för att skicka meddelanden
function sendMessage(){
    var message = document.getElementById("messageInput").value; //hämtar meddelandet
    document.getElementById("messageInput").value = ""; //funkar inte av någon anledning???
    var jsonData = {type: "message", message: message};
    websocket.send(JSON.stringify(jsonData));//funkar
}
websocket.onmessage = function processMessage(message) {
    var jsonData = JSON.parse(message.data);
    //om det är en array så kan det bara var listan med användare
    //om inte så är det antingen coordinater som ska ritas ut eller meddelanden
    if (Array.isArray(jsonData)) {
        //skapa en lista med användare och mata in i användar rutan
        var output = "";
        for(var i = 0; i < jsonData.length; i++){
            output += jsonData[i].username + "(" + jsonData[i].color + ")\n";
        }
        document.getElementById("userList").value = output;
    } else {
        //koordinater
        if(jsonData.type === "draw"){
            //rita ut koordinaterna i den färgen som användaren har
            draw(jsonData.color, jsonData.x, jsonData.y); 
        } 
        //meddelanden
        else if(jsonData.type === "message") {
            //mata in meddelanden i meddelanderutan
            updateMessages(jsonData.message);
        }
            
    }
};






