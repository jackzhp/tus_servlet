//  "use strict";
var webPath = "/Receiver/";

var g = {
    syss: ["misc"],
    sysChosen: "misc",
    osys: null, //the json file for the level system 
    levelChosen: null,
    okps: null, //not array, but an object
    okpsB: null,
    onSystemChosen: function (sys) {
        var self = this;
        if (sys) {
        } else {
            var e = document.getElementById('select_sys');
            sys = e.value;
        }
        self.sysChosen = sys;
        self.getLevels().then(ojson => {
            return self.presentLevels();
        }).catch(e => {
            console.log("exception:" + e);
        });
    },
    onLevelChosen: function (level) {
        var self = this;
        if (level) {
        } else {
            var e = document.getElementById('select_level');
            level = e.value;
        }
        self.levelChosen = level;
        self.getKPs(self.sysChosen, level).then(tf => {
            return self.presentKPs('#kpsA', self.okps);
        }).catch(e => {
            console.log("exception level:" + level);
            console.log(e);
        });
    },
    presentKPs: function (eid, okps) { //this is almost same as player.js'  presentTestInfo
        var self = this;
        var e = document.querySelector(eid);
        // e.innerHTML = "";
        // if(eid==='#kpsB'){
        //     e.innerHTML="hello";
        //     return;
        // }
        var html = "";
        var nConflicts = 0;
        for (var kpid in okps) {
            var kp = okps[kpid];
            // console.log(kpid + ":" + kp);
            if (kp.deleted) { } else {
                var eid = "kp" + kpid;
                var level = kp.levels[self.sysChosen];
                if (level === self.levelChosen) {
                } else {
                    nConflicts++;
                }
                html += '<li id="li' + eid + '">' + kpid + ":" + level + '  <input type="checkbox" id="' + eid + '"/><label for="' + eid + '">' + kp.desc + '</label><button onclick="tester.deleteKP(' + kpid + ')">Remove</button><button onclick="tester.editKP(' + kpid + ')">Edit</button><button onclick="g.changeLevel(' + kpid + ')">ChangeLevel</button> </li>';
            }
        }
        e.innerHTML = html;
        e = document.querySelector('#levelConflicts');
        e.innerHTML = "" + nConflicts;
    },
    getKPs: function (sys, level) {
        var self = this;
        return new Promise((resolve, reject) => {
            try {
                // isStarted = false;  //TODO: ensure this is right in player
                //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
                var request = new XMLHttpRequest();
                var url = webPath + "kp?act=kps&c=4level&sys=" + sys + "&level=" + level + "&t=" + new Date().getTime();
                console.log(url);
                request.open('GET', url, true); //path
                request.responseType = 'json';
                request.onload = function () {
                    var res = request.response;
                    if (res.ireason) {
                        reject("error:" + res.sreason);
                    } else {
                        self.okps = res.kps;
                        resolve(res);
                    }
                };
                // request.onFailed;  //TODO: ....
                request.send();
            } catch (e) {
                reject(e);
            }
        });
    },
    getLevels: function () {
        var self = this;
        return new Promise((resolve, reject) => {
            var sys = self.sysChosen;
            try {
                // isStarted = false;  //TODO: ensure this is right in player
                //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
                var request = new XMLHttpRequest();
                var url = webPath + "files?act=levels&sys=" + sys + "&t=" + new Date().getTime();
                console.log(url);
                request.open('GET', url, true); //path
                request.responseType = 'json';
                request.onload = function () {
                    var res = request.response;
                    if (res.ireason) {
                        reject("error:" + res.sreason);
                    } else {
                        self.osys = res;
                        resolve(res);
                    }
                };
                // request.onFailed;  //TODO: ....
                request.send();
            } catch (e) {
                reject(e);
            }
        });
    },
    presentLevelSystems: function () {
        var self = this;
        var eselect = document.getElementById('select_sys');
        //          eselect.options = []; //does not work
        while (true) {
            var i = eselect.options.length;
            if (i <= 0) break;
            i--;
            eselect.options.remove(i);
        }
        for (var i = 0; i < self.syss.length; i++) {
            var name = self.syss[i];
            eselect.options[eselect.options.length] = new Option(name, name); //TODO: maybe I can use ELevelSystem.id ?
        }
        if (self.syss.length == 1) {
            self.onSystemChosen(self.syss[0]);
        }
    },
    presentLevels: function () {
        var self = this;
        var eselect = document.getElementById('select_level');
        //          eselect.options = []; //does not work
        while (true) {
            var i = eselect.options.length;
            if (i <= 0)
                break;
            i--;
            eselect.options.remove(i);
        }
        eselect.options[eselect.options.length] = new Option("no level", "0.0");
        var map = self.osys.levels;
        for (var level in map) {
            eselect.options[eselect.options.length] = new Option(level, level);
        }
    },
    getSystems: function () {
        //for temp
        var self = this;
        self.syss = ["misc"];
        return Promise.resolve(true);
    },
    fixRelELevel: function () {
        var self = this;
        return new Promise((resolve, reject) => {
            var sys = self.sysChosen;
            try {
                // isStarted = false;  //TODO: ensure this is right in player
                //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
                var request = new XMLHttpRequest();
                //&sys=" + sys + "  with sys, then only relations with that ELevelSystem will be fixed.
                var url = webPath + "kp?act=fixHalfLevel&t=" + new Date().getTime();
                console.log(url);
                request.open('GET', url, true); //path
                request.responseType = 'json';
                request.onload = function () {
                    var res = request.response;
                    if (res.ireason) {
                        reject("error:" + res.sreason);
                    } else {
                        resolve(res);
                    }
                };
                // request.onFailed;  //TODO: ....
                request.send();
            } catch (e) {
                reject(e);
            }
        });
    },
    autoFixRelELevel: function () {
        var self = this;
        self.fixRelELevel().then(ojson => {
            // g.writeNumberField("fixed", n);
            // g.writeNumberField("levels", changed.size());
            var e = document.querySelector('#infoShort');
            e.innerHTML = "fixed:" + ojson.fixed + " over " + ojson.levels + " levels";
        }).catch(e => {
            console.log("exception 197:");
            console.log(e);
        });
    },
    searchKP_do: function (s) {
        var self = this;
        return new Promise((resolve, reject) => {
            try {
                // isStarted = false;  //TODO: ensure this is right in player
                //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
                var request = new XMLHttpRequest();
                //this is very costly, so I remove the timestamp, hence the result can be cached.
                var url = webPath + "kp?act=search&s=" + encodeURIComponent(s);// + "&t=" + new Date().getTime(); //&sys=" + sys + "
                console.log(url);
                request.open('GET', url, true); //path
                request.responseType = 'json';
                request.onload = function () {
                    var res = request.response;
                    if (res.ireason) {
                        reject("error:" + res.sreason);
                    } else {
                        resolve(res);
                    }
                };
                // request.onFailed;  //TODO: ....
                request.send();
            } catch (e) {
                reject(e);
            }
        });
    },
    searchKP: function () {
        var self = this;
        var e = document.querySelector('#searchKP');
        var s = e.value;
        //TODO: save search history
        console.log(s);
        //self.searchKP_do()
        Promise.resolve(self.okps).then(ojson => {
            self.okpsB = ojson;
            return self.presentKPs('#kpsB', self.okpsB);
        }).catch(e => {
            console.log("exception 239:");
            console.log(e);
        });
        var url = "kp?act=searchKPs&&s=" + encodeURIComponent(desc);
        self.postReq(url).then(ojson => {
          self.okpsR = ojson.kps;
          var n0 = 0, n1 = 0;
          for (var kpid in ojson.kps) {
            n0++;
            if (self.inLeftList(kpid)) {
              delete ojson.kps[kpid];
            } else {
              n1++;
            }
          }
          console.log(n0 + "->" + n1);
          self.presentKPs(true, ojson.kps);
        }).catch(e => {
          console.log(e);
          alert(e);
        });
    
    },

};

function init() {

    g.getSystems().then(ojson => {
        return g.presentLevelSystems();
    }).catch(e => {
        console.log("exception:" + e);
    });

}