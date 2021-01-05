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
        stompClient.subscribe('/topic/orderbook', function (message) {
            showOrderBook(JSON.parse(message.body));
        });
        stompClient.subscribe('/topic/opportunities', function (message) {
            showOpportunities(JSON.parse(message.body));
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


function showOrderBook(message) {
    $("#exchange").html(message.exchange);
    $("#cp").html(message.currencyPair);
    $("#asks").html("");
    for (ask of message.orderBook.asks) {
        $("#asks").append("<tr>");
        $("#asks").append("<td>" + ask.limitPrice.toFixed(1) + "</td>");
        $("#asks").append("<td>" + ask.originalAmount.toFixed(5) + "</td>");
        $("#asks").append("</tr>");
    }

    $("#bids").html("");
    for (bid of message.orderBook.bids) {
        $("#bids").append("<tr>");
        $("#bids").append("<td>" + bid.limitPrice.toFixed(1) + "</td>");
        $("#bids").append("<td>" + bid.originalAmount.toFixed(5) + "</td>");
        $("#bids").append("</tr>");
    }
}

function showOpportunities(message) {
    $("#opportunities").html("");
    for (cpo of message.values) {
        for (opportunity of cpo.opportunities) {
            console.log("opportunity: " + opportunity.currencyPair);
            $("#opportunities").append("<tr>");
            $("#opportunities").append("<td>" + opportunity.currencyPair + "</td>");
            $("#opportunities").append("<td>" + opportunity.from + "</td>");
            $("#opportunities").append("<td>" + opportunity.to + "</td>");
            $("#opportunities").append("<td>" + opportunity.ask.toFixed(1) + "</td>");
            $("#opportunities").append("<td>" + opportunity.bid.toFixed(1) + "</td>");
            $("#opportunities").append("</tr>");
        }
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

