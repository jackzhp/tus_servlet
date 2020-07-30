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
  id2object: function (atests) {
    var otests = [];
    for (var i = 0; i < atests.length; i++) {
      var testid = atests[i];
      otests[i] = { id: testid };
    }
    return otests;
  },
  onLevelChosen: function (level) {
    var self = this;
    if (level) {
    } else {
      var e = document.getElementById('select_level');
      level = e.value;
    }
    self.levelChosen = level;

    var olevel = self.osys.levels[level];
    self.otests = self.id2object(olevel.tests);
    self.presentTests(self.otests, "testsA");
  },
  inLeftList: function (testid) {
    var self = this;
    var n = self.otests.length;
    for (var i = 0; i < n; i++) {
      if (self.otests[i].id === testid)
        return true;
    }
    return false;
  },
  presentTest: function (atests, idx, eid) {
    var self = this;
    var ekps = document.querySelector("#" + eid);
    var enew = document.createElement("li");
    var otest = atests[idx];
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
  presentTests: function (atests, eid) {
    var self = this;
    var e=document.querySelector('#'+eid);
    e.innerHTML="";
    self.getAndPresentTest(atests, 0, eid); //it will recursively call itself with other idx
  },
  getAndPresentTest: function (atests, idx, eid) { //
    var self = this;
    var p;
    if (atests[idx]) { } else {
      throw new Error(idx + "/" + atests.length + " :" + eid);
    }
    if (atests[idx].loaded) {
      p = Promise.resolve(atests[idx]);
    } else {
      var testid = atests[idx].id;
      var url = webPath + "test?act=test&testid=" + testid;
      p = self.getFromServer(url).then(ojson => {
        if (atests[idx].id != ojson.id) {
          console.log(ojson);
          throw new Error("id " + ojson.id + " not expected:" + atests[idx]);
        }
        atests[idx] = ojson;
        ojson.loaded = true;
        return ojson;
      });
    }
    p.then(ojson => {
      console.log(ojson);
      return self.presentTest(atests, idx, eid);
    }).then(tf => {
      idx++;
      if (idx < atests.length) {
        self.getAndPresentTest(atests, idx, eid);
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
    if (e) { } else {
      console.log("can not find element:" + eid);
      return;
    }
    // e.innerHTML = "";
    // if(eid==='#testsB'){
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
  searchKP_do: function (server, s) {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        // isStarted = false;  //TODO: ensure this is right in player
        //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
        var request = new XMLHttpRequest();
        //this is very costly, so I remove the timestamp, hence the result can be cached.
        var url = webPath + server + "?act=search&s=" + encodeURIComponent(s);// + "&t=" + new Date().getTime(); //&sys=" + sys + "
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
    var e = document.querySelector('#searchText');
    var s = e.value;
    //TODO: save search history
    console.log(s);
    //self.searchKP_do()
    Promise.resolve(self.okps).then(ojson => {
      self.okpsB = ojson;
      return self.presentKPs(-1, 'testsB', self.okpsB);
    }).catch(e => {
      console.log("exception 239:");
      console.log(e);
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
  searchTest: function () {
    var self = this;
    var e = document.querySelector('#searchText');
    var s = e.value;
    //TODO: save search history
    // console.log(s);
    // //self.searchKP_do()
    // Promise.resolve(self.okps).then(ojson => {
    //   self.okpsB = ojson;
    //   //return self.presentKPs(-1, 'testsB', self.okpsB);
    //   return self.presentTests(self.otestsB, "testsB");
    // }).catch(e => {
    //   console.log("exception 239:");
    //   console.log(e);
    // });
    var url = "test?act=searchTests&s=" + encodeURIComponent(s);
    self.postReq(url).then(ojson => {
      console.log(ojson);
      self.otestsB = ojson.tests;
      var n0 = ojson.tests.length, n1 = 0;
      if (self.otests) {
        for (var i = n0 - 1; i >= 0; i--) {
          var testid = ojson.tests[i];
          var a = [];
          if (self.inLeftList(testid)) {
            ojson.tests.splice(i, 1);
          } else {
            n1++;
          }
        }
      }
      n2 = ojson.tests.length;
      var n1 = n0 - n2;
      self.otestsB = self.id2object(ojson.tests);
      var e = document.querySelector('#nLeft');
      e.innerHTML = "" + n1;
      e = document.querySelector('#nRight');
      e.innerHTML = "" + n2;
      e = document.querySelector('#nLR');
      e.innerHTML = "" + n0;
      // console.log(n0 + "->" + n2);
      if (n2 > 0) {
        return self.presentTests(self.otestsB, "testsB");
      } else {
        return Promise.resolve(true);
      }
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