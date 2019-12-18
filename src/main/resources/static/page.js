var second = 0;
var position_slot = 100;
var position_division = 100 / position_slot;
var LOG_INTERVAL = 500;
var url_hash = getHashCode(location.href);
var leave_count = 0;
var last_y = -1;
var last_cursor_x = 0;
if (typeof EventType == "undefined") {
    var EventType = {};
    EventType.Position = 'positionEvent';
    EventType.Click = 'clickEvent';
}
var localRecords = 0;

window.onload = function () {
    console.log("URL Hash:" + url_hash);
    console.log("Device Id:" + getDeviceId());
    console.log("Page Width:"+document.body.scrollWidth);
    console.log("Page Height:"+document.body.scrollHeight);
}

window.setInterval(function () {
    var y = window.pageYOffset + (window.screen.height / 2);
    if(last_y == y){
        leave_count++;
    }else{
        last_y = y;
        leave_count = 0;
    }
    if(leave_count > 200){
        console.log("user leaved");
        return;
    }
    var y_persentage = (window.pageYOffset + (window.screen.height / 1.8)) / document.body.scrollHeight;
    second++;
    saveData(EventType.Position, y, y_persentage);
}, LOG_INTERVAL);

//Mouse scroll
var scrollFunc = function (e) {
    e = e || window.event;
    if (e.wheelDelta) {//IE/Opera/Chrome
        mouseScroll();
    } else if (e.detail) {//Firefox
        mouseScroll();
    }
}
if (document.addEventListener) {
    document.addEventListener('DOMMouseScroll', scrollFunc, false);
}//W3C
window.onmousewheel = document.onmousewheel = scrollFunc;

var mouseScroll = function () {

}


//Mouse click
document.onmousedown = function mousedown(event) {
    // for debug, clean data by mouse middle button
    if (event.button == 1) {
        console.error("Cleaned data for debug!");
        cleanLocalEvent();
        return;
    }

    var e = window.event;
    var x_percentage = (e.clientX / document.body.scrollWidth);
    var y_percentage = (window.pageYOffset + e.clientY) / document.body.scrollHeight;
    saveData(EventType.Click, x_percentage, y_percentage);
    // console.log("mouse down, x="+(e.clientX/document.body.scrollWidth)+", y="+(window.pageYOffset+e.clientY)/document.body.scrollHeight);
}

function saveData(type, x, y) {
    if (type == EventType.Position) {
        console.log("Current Position:", x, normalize(EventType.Position, y));
        var positionArr = getLocalStorage(EventType.Position);
        if (positionArr == "") {
            positionArr = new Array(position_slot);
        }
        var positionY = normalize(EventType.Position, y);
        positionArr[positionY] += second;
        second = 0;
        setLocalStorage(EventType.Position, positionArr);

        var positionMap = {
            'url': location.href,
            'time': second,
            'refer': getReferrer(),
            'timeIn': Date.parse(new Date()),
            'timeOut': Date.parse(new Date()) + (second * 1000)
        };
        localRecords++;

        // console.log("Latest positions:", positionArr);
    } else if (type == EventType.Click) {
        var clickArr = getLocalStorage(EventType.Click);
        if (clickArr == "") {
            clickArr = new Array();
        }
        var x_percentage = normalize(EventType.Click, x);
        var y_percentage = normalize(EventType.Click, y);
        clickArr.push({ 'x': x_percentage, 'y': y_percentage });
        setLocalStorage(EventType.Click, clickArr);
        console.log("Latest click array:", clickArr);
        localRecords++;
    }



    if (localRecords >= 10) {
        submitData();
        localRecords = 0;
    }
}

function setLocalStorage(key, data) {
    localStorage.setItem(url_hash + key, JSON.stringify(data));
}

function getLocalStorage(key) {
    var plainData = localStorage.getItem(url_hash + key);
    if (plainData == "" || plainData == null) {
        console.log("Cannnot read localStorage: " + key);
        return "";
    } else {
        return JSON.parse(plainData);
    }
}

function getDeviceId() {
    var deviceId = localStorage.getItem('event_tricking_id');
    if (deviceId == "" || deviceId == null) {
        deviceId = randomString(20);
        localStorage.setItem('event_tricking_id', deviceId);
    }
    return deviceId;
}

function normalize(type, data) {
    if (type == EventType.Position) {
        return Math.round(data * 100);
        // var xxx = Math.ceil(data * 100 / position_division);
        // return xxx;
    } else if (type == EventType.Click) {
        return Math.round(data * 10000) / 100;
    }
}

function submitData() {
    var payload = {};
    var events = [];
    var positionArr = getLocalStorage(EventType.Position);
    var clickArr = getLocalStorage(EventType.Click);
    if (positionArr != "") {
        for (var i = 0; i < position_slot; i++) {
            if (positionArr[i] > 0) {
                var event = {
                    'event_type': 'position',
                    'page_section': i,
                    'stay_time': positionArr[i],
                    'user_id' : getDeviceId(),
                    'url_hash' : url_hash
                }
                events.push(event);
            }
        }
     }
    if (clickArr != "") {
        for (var i = 0; i < clickArr.length; i++) {
            var event = {
                'event_type': 'click',
                'cursor_x': clickArr[i].x,
                'cursor_y': clickArr[i].y,
                'user_id' : getDeviceId(),
                'url_hash' : url_hash
            }
            events.push(event);
        }
    }
    if (events.length == 0) {
        console.log("Submit data, cannot found data.");
        return;
    }
    payload.events = events;
    payload = JSON.stringify(payload);
    $.ajax({
        url: 'https://dc.b.dev.infinitycloud.io/v3/bgk1fh0lim',
        type: 'POST',
        async: true,
        data: payload,
        contentType: "application/json",
        success: function () {
            console.log("Submitted data.", payload);
            cleanLocalEvent();
        },
        error: function (XMLHttpResponse) {
            console.log("Submitted data.", payload);
            cleanLocalEvent();
        }
    });
}

function cleanLocalEvent() {
    setLocalStorage(EventType.Position, "");
    setLocalStorage(EventType.Click, "");
    localRecords = 0;
}

var tjArr = localStorage.getItem("jsArr") ? localStorage.getItem("jsArr") : '[{}]';
$.cookie('tjRefer', getReferrer(), { expires: 1, path: '/' });
window.onbeforeunload = function () {
    console.log("Submit data!");
    submitData();
};

function getHashCode(str, caseSensitive) {
    if (!caseSensitive) {
        str = str.toLowerCase();
    }
    var hash = 1315423911, i, ch;
    for (i = str.length - 1; i >= 0; i--) {
        ch = str.charCodeAt(i);
        hash ^= ((hash << 5) + ch + (hash >> 2));
    }

    return (hash & 0x7FFFFFFF) + '';
}

function getReferrer() {
    var referrer = '';
    try {
        referrer = window.top.document.referrer;
    } catch (e) {
        if (window.parent) {
            try {
                referrer = window.parent.document.referrer;
            } catch (e2) {
                referrer = '';
            }
        }
    }
    if (referrer === '') {
        referrer = document.referrer;
    }
    return referrer;
}

function randomString(len) {
    len = len || 32;
    var $chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678';
    var maxPos = $chars.length;
    var pwd = '';
    for (i = 0; i < len; i++) {
        pwd += $chars.charAt(Math.floor(Math.random() * maxPos));
    }
    return pwd;
}