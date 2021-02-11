var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#exchange").show();
        $("#cp").show();
        $("#asks").show();
        $("#bids").show();
        $("#opportunities").show();
        $("#exchangesUpdated").show();

    } else {
        $("#exchange").hide();
        $("#cp").hide();
        $("#asks").hide();
        $("#bids").hide();
        $("#opportunities").hide();
        $("#exchangesUpdated").hide();
    }
}

function connect() {
    var socket = new SockJS('/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        fillDropdown();
        stompClient.subscribe('/topic/orderbook', function (message) {
            showOrderBook(JSON.parse(message.body));
        });
        stompClient.subscribe('/topic/opportunities', function (message) {
            showOpportunities(JSON.parse(message.body));
        });
        stompClient.subscribe('/topic/exchanges', function (message) {
            showExchanges(JSON.parse(message.body));
        });
    });
}

function fillDropdown() {
    let dropdown = $('#exchanges');
    dropdown.empty();
    dropdown.append('<option value="-" selected disabled hidden>Choose Exchange</option>');

    const url = 'exchange/all';

    // Populate dropdown with list of exchanges
    $.getJSON(url, function (data) {
        $.each(data, function (key,entry) {
            dropdown.append($('<option></option>').attr('value', entry).text(entry));
        })
    });
}

function exchangeSelected() {
    let dropdown =  document.getElementById("exchanges").value;
    if (dropdown !== "-") {
        const url = 'exchange/' + dropdown;
        $.get(url);
    }
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
        $("#asks").append("<td>" + accounting.formatMoney(ask.limitPrice) + "</td>");
        $("#asks").append("<td>" + accounting.formatNumber(ask.originalAmount, 5) + "</td>");
        $("#asks").append("</tr>");
    }

    $("#bids").html("");
    for (bid of message.orderBook.bids) {
        $("#bids").append("<tr>");
        $("#bids").append("<td>" + accounting.formatMoney(bid.limitPrice) + "</td>");
        $("#bids").append("<td>" + accounting.formatNumber(bid.originalAmount, 5) + "</td>");
        $("#bids").append("</tr>");
    }
}

function showOpportunities(message) {
    $("#opportunities").html("");
    for (opportunity of message.values) {
        $("#opportunities").append("<tr>");
        $("#opportunities").append("<td>" + opportunity.currencyPair + "</td>");
        $("#opportunities").append("<td>" + accounting.formatMoney(opportunity.profit) + "</td>");
        $("#opportunities").append("<td>" + opportunity.from + "</td>");
        $("#opportunities").append("<td>" + opportunity.to + "</td>");
        $("#opportunities").append("<td>" + accounting.formatMoney(opportunity.ask) + "</td>");
        $("#opportunities").append("<td>" + accounting.formatMoney(opportunity.bid) + "</td>");
        $("#opportunities").append("<td>" + opportunity.created + "</td>");
        $("#opportunities").append("</tr>");
    }
}

function showExchanges(message) {
    $("#exchangesUpdated").html("");
    for (const [key, value] of Object.entries(message)) {
        $("#exchangesUpdated").append("<tr>");
        $("#exchangesUpdated").append("<td>" + key + "</td>");
        $("#exchangesUpdated").append("<td>" + value.timestamp + "</td>");
        $("#exchangesUpdated").append("<td>" + value.cps + "</td>");
        $("#exchangesUpdated").append("</tr>");
    }
}

$(function () {
    setConnected(false);
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    connect();
    $("#connect").click(function () {
        connect();
    });
    $("#disconnect").click(function () {
        disconnect();
    });
    $('#exchanges').click(function () {
        exchangeSelected();
    });
});

