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
        var right = false; //TODO: ...
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
                var html0 = '<li id="li' + eid + '"><input type="checkbox" id="' + eid + '"/><label for="' + eid + '">' + kpid + (kp.deleted ? "Deleted" : "") + ":" + level + ":" + kp.desc + '</label>';
                if (right) {
                    html0 += '<button onclick="tester.addKP(' + kpid + ')">Add</button><button onclick="g.editKP(' + kpid + ')">Edit</button>';
                } else {
                    html0 += '<button onclick="tester.deleteKP(' + kpid + ')">Remove</button><button onclick="g.editKP(' + kpid + ')">Edit</button>';
                }
                html0 += '</li>';
                html += html0;
            }
        }
        e.innerHTML = html;
        e = document.querySelector('#levelConflicts');
        e.innerHTML = "" + nConflicts;
    },
    presentKP: function (kpid) {
        var self = this;
        // var kp =self.getEKPbyID(kpid);
        var right = false, kp = self.okps[kpid]; //.testCurrent.kps[kpid + ""];
        if (kp) { } else {
            kp = self.okpsR[kpid + ""];
            right = true;
        }
        if (kp) {
        } else {
            console.log("can not find KP:" + kpid);
            return;
        }
        // console.log(kpid + ":" + kp);
        if (kp.deleted) { } else {
            var eid = "kp" + kpid;
            var level = kp.levels[self.sysChosen];
            if (level === self.levelChosen) {
            } else {
                // nConflicts++;
            }
            var html0 = '<input type="checkbox" id="' + eid + '"/><label for="' + eid + '">' + kpid + ":" + level + ":" + kp.desc + '</label>';
            if (right) {
                html0 += '<button onclick="tester.addKP(' + kpid + ')">Add</button><button onclick="g.editKP(' + kpid + ')">Edit</button>';
            } else {
                html0 += '<button onclick="tester.deleteKP(' + kpid + ')">Remove</button><button onclick="g.editKP(' + kpid + ')">Edit</button>';
            }
            var e = document.querySelector('#likp' + kpid);
            e.innerHTML = html0;
        }

    },
    getEKPbyID: function (kpid) {
        var self = this;
        var kp = self.okps[kpid]; //.testCurrent.kps[kpid + ""];
        if (kp) { } else {
            kp = self.okpsR[kpid + ""];
        }
        return kp;
    },
    editKP: function (kpid) {
        var self = this;
        var kp = self.getEKPbyID(kpid);
        var e = document.querySelector("#likp" + kpid);
        var sysName = g.sysChosen; //levelSystem;
        e.innerHTML = '<input type="text" id="kp' + kpid + '" value="' + kp.desc + '"/><input type="text" id="kplevel' + kpid + '" value="' + kp.levels[sysName] + '"/> <button onclick="g.editKPdone(' + kpid + ')">Done</button>';
    },
    editKPdone: function (kpid) {
        var self = this;
        var e = document.querySelector("#kp" + kpid);
        var desc = e.value;
        var kp = self.getEKPbyID(kpid);
        var kpChanged_desc = false;
        var el = document.querySelector("#kplevel" + kpid);
        var ls = el.value;
        var kpChanged_level = ls != kp.levels[g.sysChosen];
        if (desc === kp.desc) {
            p = Promise.resolve(self.okps[kpid]);
        } else {
            kpChanged_desc = true;
            var url = "kp?act=chgKPdesc&idkp=" + kpid + "&desc=" + encodeURIComponent(desc);
            p = self.postReq(url);
        }
        p = p.then(ojson => {
            if (kpChanged_desc) {
                self.okps[kpid] = ojson;
            }
            if (kpChanged_level) {
                var sysName = g.sysChosen; // user.levelSystem;
                var url = "kp?act=chgKPlevel&idkp=" + kpid + "&sys=" + sysName + "&level=" + encodeURIComponent(ls);
                return self.postReq(url);
            } else {
                return Promise.resolve(self.okps[kpid]);
            }
        });
        p.then(ojson => {
            console.log(ojson);
            if (kpChanged_desc || kpChanged_level) {
                self.okps[kpid] = ojson;
                self.presentKP(kpid); //self.updateTestInfo();
            } else {
                // return Promise.reject("nothing changed");
            }
        }).catch(e => {
            console.log(e);
            alert(e);
        });
    },
    getKPs: function (sys, level) {
        var self = this;
        var url = "kp?act=kps&c=4level&sys=" + sys + "&level=" + level + "&t=" + new Date().getTime();
        return self.getFromServer(url).then(ojson => {
            self.okps = ojson.kps;
        });
    },
    postReq: function (urlSuffix) { //if we do not care the result, this method can be used.
        var self = this;
        return new Promise((resolve, reject) => {
            try {
                var request = new XMLHttpRequest();
                var url = webPath + urlSuffix;
                request.open('POST', url, true); //&reviewOnly=false when false, can be omitted.
                request.responseType = 'json';
                request.onload = function () {
                    var ojson = request.response;
                    console.log(ojson);
                    if (ojson.ireason) {
                        reject(url + " returns: " + JSON.stringify(ojson));
                    } else {
                        // console.log("succeeded");
                        resolve(ojson);
                    }
                };
                // request.onFailed;  //TODO: ....
                request.send();
            } catch (e) {
                reject(e);
            }
        });
    },
    getFromServer: function (urlSuffix) {
        var url = webPath + urlSuffix;
        return new Promise((resolve, reject) => {
            try {
                // isStarted = false;  //TODO: ensure this is right in player
                //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
                var request = new XMLHttpRequest();
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
    listNoTest: function () {
        var self = this;
        if (confirm("this is safe only after the relationship between ETest & EKP has been checked")) {
        } else {
            return;
        }
        var url = "kp?act=kps&c=notest&t=" + new Date().getTime();
        self.getFromServer(url).then(ojson => {
            console.log(ojson);
            self.okps = ojson.kps;
            self.presentKPs('#kpsA', self.okps);
        }).catch(e => {
            console.log("exception 81");
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
                var url = webPath + "kp?act=searchKPs&&s=" + encodeURIComponent(s);// + "&t=" + new Date().getTime(); //&sys=" + sys + "
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
        // //TODO: redundant ...
        // self.searchKP_do(s).then(ojson => {  //Promise.resolve(self.okps)
        //     self.okpsB = ojson;
        //     return self.presentKPs('#kpsB', self.okpsB);
        // }).catch(e => {
        //     console.log("exception 239:");
        //     console.log(e);
        // });
        var url = "kp?act=searchKPs&&s=" + encodeURIComponent(s);
        self.postReq(url).then(ojson => {
            if(true){
            self.okpsB = ojson.kps;
            return self.presentKPs('#kpsB', self.okpsB);
            }
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