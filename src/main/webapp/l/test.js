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

    self.otests = [];
    var olevel = self.osys.levels[level];
    var atests = olevel.tests;
    for (var i = 0; i < atests.length; i++) {
      var testid = atests[i];
      self.otests[i] = { id: testid };
    }
    self.presentTests("kpsA");
  },
  presentTest: function (idx, eid) {
    var self = this;
    var ekps = document.querySelector(eid);
    var enew = document.createElement("li");
    var otest = self.otests[idx];
    var testid = otest.id;
    while (ekps.children.length <= idx) {
      enew.setAttribute("id", "litest" + ekps.children.length);
      ekps.append(enew);
    }
    //ekps.insertBefore(enew, ekps.children[0]);
    if (ekps.children.length > idx) {
      enew.setAttribute("id", "litest" + idx);
      ekps.children[idx] = enew;
    }
    var eidkps = 'test' + testid + 'kps';
    enew.innerHTML = testid + ' <span id="test' + testid + 'info">' + otest.info + '</span><br/><ul id="' + eidkps + '"></ul>'; //\'' + kpid + '\'
    self.presentKPs(testid, eidkps, otest.kps);
  },
  presentTests: function (eid) {
    var self = this;
    self.getAndPresentTest(0, "#" + eid); //it will recursively call itself with other idx
  },
  getAndPresentTest: function (idx, eid) { //
    var self = this;
    var p;
    if (self.otests[idx].loaded) {
      p = Promise.resolve(self.otests[idx]);
    } else {
      var testid = self.otests[idx].id;
      var url = webPath + "test?act=test&testid=" + testid;
      p = self.getFromServer(url).then(ojson => {
        if (self.otests[idx].id != ojson.id) {
          console.log(ojson);
          throw new Error("id " + ojson.id + " not expected:" + self.otests[idx]);
        }
        self.otests[idx] = ojson;
        ojson.loaded = true;
        return ojson;
      });
    }
    p.then(ojson => {
      console.log(ojson);
      return self.presentTest(idx, eid);
    }).then(tf => {
      idx++;
      if (idx < self.otests.length) {
        self.getAndPresentTest(idx, eid);
      }
      return true;
    }).catch(e => {
      console.log("exception:");
      console.log(e);
      self.getAndPresentTest(idx, eid);
    });
  },
  //testid could be -1: not known yet
  presentKPs: function (testid, eid, okps) { //this is almost same as player.js'  presentTestInfo
    var self = this;
    var e = document.querySelector("#" + eid);
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
        //<input type="checkbox" id="' + eid + '"/><label for="' + eid + '"> kp.desc </label>
        html += '<li id="li' + eid + '">' + kpid + ":" + level + '  ' + kp.desc + '<button onclick="g.removeKP(' + testid + "," + kpid + ')">Remove</button><button onclick="g.editKP(' + kpid + ')">Edit</button><button onclick="g.changeLevel(' + kpid + ')">ChangeLevel</button> </li>';
      }
    }
    e.innerHTML = html;
    e = document.querySelector('#levelConflicts');
    e.innerHTML = "" + nConflicts;
  },
  getTests: function (sys, level) {
    var self = this;
    //get tests seems not needed!
    var url = webPath + "test?act=tests&c=4level&sys=" + sys + "&level=" + level + "&t=" + new Date().getTime();
    return self.getFromServer(url).then(res => {
      if (res.ireason) {
        throw new Error("error:" + res.sreason);
      } else {
        self.okps = res.kps;
        return res; //TODO: check is this really needed?
      }
    });
  },
  getKPs: function (sys, level) {
    var self = this;
    var url = webPath + "kp?act=kps&c=4level&sys=" + sys + "&level=" + level + "&t=" + new Date().getTime();
    return self.getFromServer(url).then(res => {
      if (res.ireason) {
        throw new Error("error:" + res.sreason);
      } else {
        self.okps = res.kps;
        return res; //TODO: check is this really needed?
      }
    });
  },
  getFromServer: function (url) {
    var self = this;
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
          resolve(res);
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
            //{"name":"misc","levels":{"1.1":{},...}}            
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
      return self.presentKPs('kpsB', self.okpsB);
    }).catch(e => {
      console.log("exception 239:");
      console.log(e);
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