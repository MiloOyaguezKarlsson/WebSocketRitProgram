var x = 0, y = 0;
var canvas, ctx, height, width; //canvas, context, canvas bredd och höjd initieras i onload-funktionen
var drawing = false; //håller koll på om man ritar nu
var drawDot = false; //för att rita en punkt om man bara enkelklickar
var url = "ws://localhost:8080/WebSocketRitProgram/draw";
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
};
//function för att rita
function draw(color, x, y) {
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.arc(x, y, 10, 0, Math.PI * 2, false);
    ctx.fill();
}
function findxy(event, e) {
    if (event === "down") {

        x = e.clientX - canvas.offsetLeft;
        y = e.clientY - canvas.offsetTop;

        drawing = true;
    }
    if (event === "up" || event === "out") {
        drawing = false;
    }
    if (event === "move") {
        if (drawing) {
            x = e.clientX - canvas.offsetLeft;
            y = e.clientY - canvas.offsetTop;
            sendDrawCoord();
        }
    }
}
function updateMessages(message){
    document.getElementById("messageArea").value += message + "\n";
}
//function för att skicka in koordinater som ska ritas
function sendDrawCoord() {
    var jsonData = {type: "draw", x: x, y: y};
    websocket.send(JSON.stringify(jsonData));
}
//function för att skicka in ett användarnamn
function sendUsername(username) {
    var jsonData = {type: "username", username: username};
    websocket.send(JSON.stringify(jsonData));
}
//function för att skicka meddelanden
function sendMessage(){
    var message = document.getElementById("messageInput").value.toString();
    var jsonData = {type: "message", message: message};
    websocket.send(JSON.stringify(jsonData));
}
websocket.onmessage = function processMessage(message) {
    var jsonData = JSON.parse(message.data);
    
    if (Array.isArray(jsonData)) {
        var output = "";
        for(var i = 0; i < jsonData.length; i++){
            output += jsonData[i].username + "(" + jsonData[i].color + ")\n";
        }
        document.getElementById("userList").value = output;

    } else {
        
        if(jsonData.type === "draw")
            draw(jsonData.color, jsonData.x, jsonData.y);
        else if(jsonData.type === "message")
            updateMessages(jsonData.message);
    }
};






