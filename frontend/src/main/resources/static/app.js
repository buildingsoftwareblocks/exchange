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
        stompClient.subscribe('/user/topic/orderbook', function (message) {
            showOrderBook(JSON.parse(message.body));
        });
        stompClient.subscribe('/user/topic/opportunities', function (message) {
            showOpportunities(JSON.parse(message.body));
        });
        stompClient.subscribe('/user/topic/exchanges', function (message) {
            showExchanges(JSON.parse(message.body));
        });
        fillExchangeDropdown();
        fillCurrencyDropdown();
    });
}

function fillExchangeDropdown() {
    let dropdown = $('#exchanges');
    dropdown.empty();
    dropdown.append('<option value="-" selected disabled hidden>Choose Exchange</option>');

    const url = 'exchange/all';

    // Populate dropdown with list of exchanges
    $.getJSON(url, function (data) {
        $.each(data, function (key, entry) {
            dropdown.append($('<option></option>').attr('value', entry).text(entry));
        })
    });
}

function fillCurrencyDropdown() {
    let dropdown = $('#currencies');
    dropdown.empty();
    dropdown.append('<option value="-" selected disabled hidden>Choose Currency Pair</option>');

    if ($("#exchanges").val()) {
        const url = 'exchange/currencies/' + $("#exchanges").val();

        // Populate dropdown with list of exchanges
        $.getJSON(url, function (data) {
            $.each(data, function (key, entry) {
                dropdown.append($('<option></option>').attr('value', entry).text(entry));
            })
        });
    }
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);

    // clear environment
    $("#exchange").html("");
    $("#cp").html("");
    $("#asks").html("");
    $("#bids").html("");
    $("#opportunities").html("");
    $("#exchangesUpdated").html("");

    console.log("Disconnected");
}


function showOrderBook(message) {
    $("#exchange").html(message.exchange);
    $("#cp").html(message.currencyPair);
    $("#orderbook_timestamp").html(message.timestamp);
    $("#asks").html("");
    for (ask of message.orders.asks) {
        $("#asks").append("<tr>");
        $("#asks").append("<td>" + accounting.formatMoney(ask.limitPrice) + "</td>");
        $("#asks").append("<td>" + accounting.formatNumber(ask.originalAmount, 5) + "</td>");
        $("#asks").append("</tr>");
    }

    $("#bids").html("");
    for (bid of message.orders.bids) {
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
    // disable all caching for ajax calls
    $.ajaxSetup({cache: false});

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
    $('#exchanges').change(function () {
        stompClient.send('/app/exchange', {}, JSON.stringify($("#exchanges").val()));
        fillCurrencyDropdown();
    });
    $('#currencies').change(function () {
        stompClient.send('/app/cp', {}, JSON.stringify($("#currencies").val()));
    });
});

