var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    } else {
        $("#conversation").hide();
    }
    $("#asks").html("");
}

function connect() {
    var socket = new SockJS('/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/orderbook', function (orderBook) {
            showOrderBook(JSON.parse(orderBook.body));
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}


function showOrderBook(orderBook) {
    $("#asks").html("");
    for (ask of orderBook.asks) {
        $("#asks").append("<tr>");
        $("#asks").append("<td>" + ask.limitPrice + "</td>");
        $("#asks").append("<td>" + ask.originalAmount + "</td>");
        $("#asks").append("</tr>");
    }

    $("#bids").html("");
    for (bid of orderBook.bids) {
        $("#bids").append("<tr>");
        $("#bids").append("<td>" + bid.limitPrice + "</td>");
        $("#bids").append("<td>" + bid.originalAmount + "</td>");
        $("#bids").append("</tr>");
    }
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
});

