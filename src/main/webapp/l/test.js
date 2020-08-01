//  "use strict";
var webPath = "/Receiver/";

var g = {
  syss: ["misc"],
  sysChosen: "misc",
  osys: null, //the json file for the level system 
  levelChosen: null,
  okps: null, //not array, but an object. all KPs used by ETests on this page.
  okpsB: null,
  otests: null, //all the tests for levelChosen.
  otestsA: null, //newly added, this is for the left side. otests is kept for all ETests of the level.
  otestsB: null, //all the tests at the right side, usually exclude those in the left.
  targetTestToMerge: "", //merge related stuff
  targetKPToMerge: "", //merge related stuff
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
    var self = this;
    var otests = [];
    for (var i = 0; i < atests.length; i++) {
      var testid = atests[i];
      var otest = self.getETestByID(testid);
      otests[i] = otest ? otest : { id: testid };
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
    console.log("level chosen:" + level);
    var olevel = self.osys.levels[level];
    var p;
    if (olevel) {
      p = Promise.resolve(olevel);
    } else {
      p = Promise.resolve(null); //TODO: I should get the list of ETests without level.
    }
    p.then(olevel => {
      if (olevel) {
        self.otests = self.id2object(olevel.tests);
        self.otestsA = self.otests;
        self.presentTests(self.otestsA, "testsA");
      } else {
        var e = document.querySelector('#testsA');
        e.innerHTML = "";
      }
    }).catch(e => {
      console.log(e);
    })
  },
  inLeftList: function (testid) {
    var self = this;
    var n = self.otestsA.length;
    for (var i = 0; i < n; i++) {
      if (self.otestsA[i].id === testid)
        return true;
    }
    return false;
  },
  getTestIdxByID: function (testid, atests) {
    var self = this;
    for (var i = 0; i < atests.length; i++) {
      var test = atests[i];
      if (testid == test.id) {
        return i;
      }
    }
    return -1;
  },
  presentTest: function (atests, idx, eid) {
    var right = false; //TODO: .... in arguments.
    var self = this;
    var ekps = document.querySelector("#" + eid); //TODO: rename to etests
    var enew = document.createElement("li");
    var otest = atests[idx];
    var testid = otest.id;
    if (false) { //dead loop here.
      while (ekps.children.length <= idx) {
        enew.setAttribute("id", "litest" + ekps.children.length);
        ekps.append(enew);
      }
    } else { //we always do sequentially, so
      if (ekps.children.length < idx) {
        throw new Error(ekps.children.length + ":" + idx);
      }
      enew.setAttribute("id", "litest" + ekps.children.length);
      ekps.append(enew);
    }
    //ekps.insertBefore(enew, ekps.children[0]);
    if (ekps.children.length > idx) {
      // enew.setAttribute("id", "litest" + idx);
      // ekps.children[idx] = enew;
      enew = ekps.children[idx];
    }
    var eidkps = 'test' + testid + 'kps';
    var html = '<input type="checkbox" id="cb' + testid + '"/><label for="cb' + testid + '">' + testid + (otest.deleted ? "Deleted" : "") + '</label> <span id="info' + testid + '">' + otest.info + '<button onclick="g.editTest(' + testid + ')">Edit</button></span><button onclick="g.mergeSelectedKPs(' + testid + ')">Merge</button><button onclick="g.getAndUpdateTestInfo(' + testid + ')">Update</button>'; //\'' + kpid + '\'
    if (self.isOnly1) {
      html += '<button onclick="g.onlyMany()">many</button>';
    } else {
      html += '<button onclick="g.only1(' + testid + ')">only1</button>';
    }
    html += '<br/><ul id="' + eidkps + '"></ul>';
    enew.innerHTML = html;
    self.presentKPs(testid, eidkps, self.getKPs_o(otest.akps), right); //otest.kps
  },
  editTest: function (testid) {
    var self = this;
    var otest = self.getETestByID(testid);
    var e = document.querySelector('#info' + testid);
    // console.log(self.testCurrent);
    var html = '<input type="text" id="infotext" value="' + otest.info + '"/> <button onclick="g.editTestDone(' + testid + ')">Done</button>';
    e.innerHTML = html; //.fn; //.pathPlaying;// player.pathCurrent;
  },
  editTestDone: function (testid) {
    var self = this;
    var e = document.querySelector("#infotext");
    var info = e.value;
    var url = "test?act=chgInfo&idtest=" + testid + "&info=" + encodeURIComponent(info);
    self.postReq(url).then(ojson => {
      // self.presentTests(self.otestsA, "testsA");
      // self.updateTestInfo(testid);
      self.getAndUpdateTestInfo(testid);
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  onKPs: function (okps) { //return kpid in an array
    var self = this;
    if (self.okps) { } else {
      self.okps = {};
    }
    var akps = [];
    for (var kpid in okps) {
      akps.push(kpid);
      var kp = okps[kpid];
      var kpO = self.okps[kpid];
      var replace = false;
      if (kpO) {
        if (kpO.ts) {
          if (kp.ts) {
            replace = kp.ts > kpO.ts;
          }
        } else {
          if (kp.ts) {
            replace = kp.ts > kpO.ts;
          }
        }
      } else {
        replace = true;
      }
      if (replace) {
        self.okps[kpid] = kp;
      }
    }
    return akps;
  },
  getKPs_o: function (akps) {
    var self = this;
    var okps = {};
    for (var i = 0; i < akps.length; i++) {
      var kpid = akps[i];
      okps[kpid] = self.okps[kpid];
    }
    return okps;
  },
  // getKPs_a: function(okps){
  // },
  presentTests: function (atests, eid) {
    var self = this;
    console.log("will clear e:" + eid);
    var e = document.querySelector('#' + eid);
    e.innerHTML = "";
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
        self.onETest(ojson);
        return ojson;
      });
    }
    p.then(ojson => {
      console.log(ojson);
      return self.presentTest(atests, idx, eid);
    }, e => {
      return self.presentTest(atests, idx, eid);
    }).then(tf => {
      idx++;
      if (idx < atests.length) {
        self.getAndPresentTest(atests, idx, eid);
      }
      return true;
    }).catch(e => {
      console.log("exception:");
      // console.log(e);  //this get into dead loop?
      idx++;
      if (idx < atests.length) {
        self.getAndPresentTest(atests, idx, eid);
      }
    });
  },
  /* testid could be -1: when I search EKPs, they are not associated with any ETest.
  */
  presentKPs: function (testid, eid, okps, right) { //this is almost same as player.js'  presentTestInfo
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
        var eid = testid + "kp" + kpid;
        var level = kp.levels[self.sysChosen];
        if (level === self.levelChosen) {
        } else {
          nConflicts++;
        }
        //
        var html0 = '<li id="li' + eid + '">';
        if (right) { } else html0 += '<input type="checkbox" id="cb' + eid + '"/><label for="cb' + eid + '">';
        html0 += '<span ondblclick="g.checkKP(' + kpid + ')">' + kpid + ":" + level + '  ' + kp.desc + '</span>';
        if (right) {
          html0 += '<button onclick="g.addKP(' + kpid + ')">Add</button>'; //<button onclick="tester.editKP(' + kpid + ')">Edit</button>
        } else {
          html0 += ' </label>';
          html0 += '<button onclick="g.removeKP(' + testid + "," + kpid + ')">Remove</button><button onclick="g.editKP(' + testid + "," + kpid + ')">Edit</button>'; //<button onclick="g.changeLevel(' + kpid + ')">ChangeLevel</button>
        }
        html0 += '</li>';
        html += html0;
      }
    }
    e.innerHTML = html;
    e = document.querySelector('#levelConflicts');
    e.innerHTML = "" + nConflicts;
  },
  checkKP: function (kpid) {
    var self = this;
    var kp = self.okps[kpid];
    var p;
    if (kp) {
      p = Promise.resolve(kp);
    } else {
      p = null; //TODO: get it from server.
    }
    p.then(okp => {
      var otests = self.id2object(okp.tests);
      self.otestsB = otests;
      self.presentTests(self.otestsB, "testsB");
    }).catch(e => {
      console.log("exception");
      console.log(e);
    });
  },
  removeKP: function (testid, kpid) {
    var self = this;
    self.postToTestAndUpdate(testid, "deleteKP", kpid);
  },
  addKP: function (kpid) {
    var self = this;
    if (self.isOnly1) { } else {
      alert("please choose only1 in the left side");
      return;
    }
    var testid = self.otestsA[0].id;
    self.postToTestAndUpdate(testid, "addKP", kpid);
  },
  postToTestAndUpdate: function (testid, act, kpid) {
    var self = this;
    var url = "test?act=" + act + "&idtest=" + testid + "&idkp=" + kpid;
    self.postReq(url).then(ojson => {
      self.getAndUpdateTestInfo(testid);//self.updateTestInfo(testid);
    }).catch(e => {
      console.log(e);
      alert(e);
    });
    var eid = testid + "kp" + kpid;
    var e = document.querySelector("#li" + eid);
    if (e)
      e.parentNode.removeChild(e);
  },
  getKPsSelected: function (testid, akps) {
    var self = this;
    var selected = "";
    var first = true;
    for (var i = 0; i < akps.length; i++) {
      var kpid = akps[i];
      var eid = testid + "kp" + kpid;
      var e = document.querySelector('#cb' + eid);
      if (e) {
        if (e.checked) {
          if (first) {
            first = false;
          } else {
            selected += ",";
          }
          selected += kpid;
        }
      }
    }
    return selected;
  },
  mergeSelectedKPs: function (testid) { //TODO: merge Target should learn from mergeTests
    var self = this;
    var otest = self.getETestByID(testid);
    var selected = self.getKPsSelected(testid, otest.akps);
    var url = "kp?act=mergeKPs&kpids=" + selected + "&idtest=" + testid;
    self.postReq(url).then(ojson => {
      if (ojson.ireason != 0) {
        throw new Error("result for mergeSelected:" + JSON.stringify(ojson));
      } else {
        //TODO: I should go with EKP's, so all ETest could be updated.
        self.updateTestInfo(testid);
      }
    }).catch(e => {
      console.log("exception 457:");
      console.log(e);
    });
  },
  getTestsSelected: function (selected) {
    var self = this;
    var a = [self.otestsA, self.otestsB];
    for (var i = 0; i < a.length; i++) {
      var atests = a[i];
      if (atests) {
        for (var j = 0; j < atests.length; j++) {
          var otest = atests[j];
          var eid = 'cb' + otest.id;
          var e = document.querySelector('#' + eid);
          if (e.checked) {
            //deselect these? this is not needed, if merge succeeded, then all these will be updated.
            var id_s = "" + otest.id;
            if (selected.includes(id_s))
              continue;
            if (selected === "") {
            } else {
              selected += ",";
            }
            selected += id_s;
          }
        }
      }
    }
    return selected;
  },
  mergeTests: function () {
    var self = this;
    var target = self.targetTestToMerge;
    var selected = self.getTestsSelected(target);
    var idx = selected.indexOf(",");
    if (target) {
      self.targetTestToMerge = "";// null;//delete self.targetTestToMerge;
      var e = document.querySelector('#mergeTests');
      e.innerHTML = "merge selected Tests";
      if (idx === -1) {
        return;
      }
    } else {
      if (idx === -1) {
        self.targetTestToMerge = selected;
        var e = document.querySelector('#mergeTests');
        e.innerHTML = "merge Targeted";
        return;
      } else {
        //to avoid mistake, force the user to do it in 2 steps.
        alert("please select only 1 as merge target:" + selected);
        return;
      }
    }
    var url = "test?act=mergeTests&testids=" + selected;
    self.postReq(url).then(ojson => {
      if (ojson.ireason != 0) {
        throw new Error("result for mergeSelected:" + JSON.stringify(ojson));
      } else {
        //TODO: reload the target ETest. no. all the ETests in selected.
        self.updateTestInfo(testid);
      }
    }).catch(e => {
      console.log("exception 457:");
      console.log(e);
    });
  },
  changeLevel: function (kpid) {
    var self = this;
    var url = "kp?act=chgKPlevel&idkp=" + kpid + "&sys=" + self.sysChosen + "&level=" + encodeURIComponent(self.levelChosen);
    self.postReq(url).then(ojson => {
      if (ojson.ireason) {
        alert("failed:" + ojson.sreason);
        return;
      }
      self.okps[kpid] = ojson;
      var kp = self.okps[kpid];
      self.updateKPinfo(kp);
    }).catch(e => {
      console.log(e);
    });
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
  autoFixRelELevelEKP: function () {
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
  autoFixRelELevelETest: function () {
    alert("do it at server side");
  },
  autoFixRelETestEKP: function () {
    alert("do it at server side");
  },
  //when this is called?
  onETest: function (ojson) {
    try {
      var self = this;
      ojson.loaded = true;
      ojson.akps = self.onKPs(ojson.kps);
      ojson.kps = null;
      var a = [self.otests, self.otestsA, self.otestsB];
      var n = 0;
      for (var i = 0; i < a.length; i++) {
        var atests = a[i], idx;
        if (atests) {
          idx = self.getTestIdxByID(ojson.id, atests);
          if (idx == -1) {
            continue;
          }
          if (atests[idx].id != ojson.id) {
            console.log(ojson);
            throw new Error("id " + ojson.id + " not expected:" + atests[idx]);
          }
          atests[idx] = ojson;
          n++;
        }
      }
      if (n == 0)
        alert("can not find ETest for id:" + ojson.id);
    } catch (e) {
      console.log(ojson);
      console.log(e);
    }
  },
  getAndUpdateTestInfo: function (testid) {
    var self = this;
    var url = webPath + "test?act=test&testid=" + testid;
    self.getFromServer(url).then(ojson => {
      if (ojson.ireason) {
        console.log("failed:" + ojson.sreason);
        return;
      }
      self.onETest(ojson);
      return ojson;
    }).then(ojson => {
      self.updateTestInfo(testid);
    }).catch(e => {
      console.log(e);
    });
  },
  only1: function (testid) {
    var self = this;
    self.isOnly1 = true;
    var otest = self.getETestByID(testid);
    self.otestsA = [otest];
    self.presentTests(self.otestsA, "testsA");
  },
  onlyMany: function () {
    var self = this;
    self.isOnly1 = false;
    self.otestsA = self.otests;
    self.presentTests(self.otestsA, "testsA");
  },
  updateTestInfo: function (testid) {
    if (testid) {
      if (testid === -1) {
        throw new Error("testid ==-1");
      }
    } else throw new Error("testid undefined");
    var self = this;
    var atests = self.otestsA;
    var eid, idx = -1;
    if (atests) {
      eid = "testsA";
      idx = self.getTestIdxByID(testid, atests);
      if (idx != -1) {
        self.presentTest(atests, idx, eid);
      }
    }
    if (idx === -1) {
      atests = self.otestsB;
      if (atests) {
        eid = "testsB";
        idx = self.getTestIdxByID(testid, atests);
        if (idx != -1) {
          self.presentTest(atests, idx, eid);
        }
      }
    }
  },
  updateKPinfo: function (kp) {
    var self = this;
    for (var i = 0; i < kp.tests.length; i++) {
      var testid = kp.tests[i];
      self.updateTestInfo(testid);
    }
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
  getFromServer: function (url) { //TODO: this is almost same as postReq
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
          if (res.ireason) {
            reject("failed:" + res.sreason);
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
    var idxSelected = -1;
    for (var level in map) {
      var idx = eselect.options.length;
      eselect.options[idx] = new Option(level, level);
      if (level === self.levelChosen) {
        idxSelected = idx;
      }
    }
    if (self.levelChosen) {
      eselect.selectedIndex = idxSelected;
    }
  },
  getSystems: function () {
    //for temp
    var self = this;
    self.syss = ["misc"];
    return Promise.resolve(true);
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
    console.log(s);
    //TODO: save search history
    var url = "kp?act=searchKPs&&s=" + encodeURIComponent(s);
    // Promise.resolve(self.okps).then(ojson => {
    //   self.okpsB = ojson;
    //   return self.presentKPs(-1, 'testsB', self.okpsB);
    // }).catch(e => {
    //self.searchKP_do("kp", s)  //TODO: searchKP_do is not needed, remove it.
    self.postReq(url).then(ojson => { //self.postReq(url)
      self.okpsR = ojson.kps;
      var n0 = 0, n1 = 0;
      for (var kpid in ojson.kps) {
        n0++;
        // if (self.inLeftList(kpid)) {
        //   delete ojson.kps[kpid];
        // } else 
        {
          n1++;
        }
      }
      console.log(n0 + "->" + n1);
      self.presentKPs(-1, "testsB", ojson.kps, true);
    }).catch(e => {
      console.log(e);
      alert(e);
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
      if (self.otestsA) {
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
  getEKPbyID: function (kpid) {
    var self = this;
    var kp = self.okps[kpid];// self.testCurrent.kps[kpid + ""];
    // if (kp) { } else {
    //   kp = self.okpsR[kpid + ""];
    // }
    return kp;
  },
  getETest: function (atests, testid) {
    for (var i = 0; i < atests.length; i++) {
      var test = atests[i];
      if (test.id == testid)
        return test;
    }
    return null;
  },
  getETestByID: function (testid) {
    var self = this;
    var otest = null;
    if (self.otests)
      otest = self.getETest(self.otests, testid);
    if (otest) { } else {
      if (self.otestsB)
        otest = self.getETest(self.otestsB, testid);
    }
    return otest;
  },
  getETestLevel: function (otest) {
    var self = this;
    var sys = self.sysChosen;
    var lh = "0.0";
    for (var i = 0; i < otest.akps.length; i++) {
      var kpid = otest.akps[i];
      var kp = self.getEKPbyID(kpid);
      var lt = kp.levels[sys];
      if (lt > lh) {
        lh = lt;
      }
    }
    return lh;
  },
  editKP: function (testid, kpid) {
    var self = this;
    var kp = self.getEKPbyID(kpid);
    var eid = testid + "kp" + kpid;
    var e = document.querySelector("#li" + eid);
    var sysName = self.sysChosen;// user.levelSystem;
    e.innerHTML = '<input type="text" id="desc' + eid + '" value="' + kp.desc + '"/><input type="text" id="level' + eid + '" value="' + kp.levels[sysName] + '"/> <button onclick="g.editKPdone(' + testid + ',' + kpid + ')">Done</button>';
  },
  editKPdone: function (testid, kpid) {
    var self = this;
    var eid = testid + "kp" + kpid;
    var e = document.querySelector("#desc" + eid);
    var desc = e.value;
    var levelChanged_kp = false, levelChanged_test = false;
    var el = document.querySelector("#level" + eid);
    var levelNew = el.value;
    var sysName = self.sysChosen;//.levelChosen;// user.levelSystem;
    var kp = self.getEKPbyID(kpid);
    if (levelNew === kp.levels[sysName]) {
    } else {
      levelChanged_kp = true;
      var otest = self.getETestByID(testid);
      var ltO = self.getETestLevel(otest);
      levelChanged_test = levelNew > ltO;
    }
    if (desc === kp.desc) {
      p = Promise.resolve(kp);
    } else {
      var url = "kp?act=chgKPdesc&idkp=" + kpid + "&desc=" + encodeURIComponent(desc);
      p = self.postReq(url);
    }
    p = p.then(ojson => {
      if (ojson.ireason) {
        alert("failed:" + ojson.sreason);
        return;
      }
      self.okps[kpid] = ojson;
      if (levelChanged_kp) {
        var url = "kp?act=chgKPlevel&idkp=" + kpid + "&sys=" + sysName + "&level=" + encodeURIComponent(levelNew);
        return self.postReq(url);
      } else {
        return Promise.resolve(ojson);
      }
    });
    p.then(ojson => {
      if (ojson.ireason) {
        alert("failed:" + ojson.sreason);
        return;
      }
      // asdf asdf       
      self.okps[kpid] = ojson;
      self.updateKPinfo(ojson);
      if (levelChanged_test && levelNew !== self.levelChosen) {
        if (confirm("test's level has changed, once reload the test will go to level " + levelNew + ". reload?")) {
          self.onSystemChosen(self.sysChosen);
        }
      }
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  newKP: function () {
    var self = this;
    var ekps = document.querySelector("#kpsA");
    var enew = document.createElement("li");
    enew.setAttribute("id", "likpnew");
    var kpid = "new";
    ekps.insertBefore(enew, ekps.children[0]);
    enew.innerHTML = '<input type="text" id="kp' + kpid + '" value="description of the KP"/> <input type="text" id="kplevel' + kpid + '" value="1.1"/><button onclick="tester.searchKP()">Search</button> <button onclick="tester.newKPdone()">Add</button>'; //\'' + kpid + '\'
  },
  newKPdone: function () {
    //this KP might exist already, so I have to allow the user to choose one of the exists.
    var self = this;
    var testid = 0; //TODO: ...
    var e = document.querySelector("#kpnew");
    var desc = e.value;
    var e = document.querySelector("#kplevelnew");
    var level = e.value;
    var url = "test?act=newKP&idtest=" + testid + "&desc=" + encodeURIComponent(desc) + "&level=" + encodeURIComponent(level);
    self.postReq(url).then(ojson => {
      self.updateTestInfo(testid);
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  loadTest: function () {
    var self = this;
    var e = document.querySelector('#testid');
    var testid = e.value;
    console.log(testid);
    self.otestsA = [{ id: testid }];
    self.getAndUpdateTestInfo(testid);
  },




};

function init() {

  g.getSystems().then(ojson => {
    return g.presentLevelSystems();
  }).catch(e => {
    console.log("exception:" + e);
  });

}