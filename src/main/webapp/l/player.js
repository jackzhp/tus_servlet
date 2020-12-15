//  "use strict";
var webPath = "/Receiver/";
//the built in loop is not good. there is no delay before rewind to the loopStart
// because of this, the structure of the code will be changed accordingly.
var player = {
  audioCtx: null,
  e_startBtn: null, //susresBtn,   TODO: add prefix e_ to this type of variables.
  e_stopBtn: null,
  e_timeDisplay: null,
  e_playbackControl: null,
  e_playbackValue: null,
  e_loopstartControl: null,
  e_loopstartValue: null,
  e_loopendControl: null,
  e_loopendValue: null,
  delayUponEnded: 1000, //1 second delay. TODO: should be adjustable by the user.
  // define variables
  // pathCurrent: null, //TODO: rename it to pathCurrent.
  dataReady: false, //loaded and decoded, so ready to be played
  isStarted: false, //start is called or not. do not change it to isPlaying! whether is paused, check this.audioCtx.state.
  //  audioCtx:null,
  source: null,
  buffer: null,
  songLength: null, //      self.songLength = buffer.duration;  TODO: so remove it.
  playSpeed: 1.0,
  loopStart: null, loopEnd: null, //these 2 are not needed, use source.loopStart and source.loopEnd directly.
  tsStartCtx: null,
  isStopped: null, //tells whether the sound is going on? no longer needed. previous to show the timestamp
  nTimes: 0, //how many times played
  p_stop_resolve: null, //when really stopped, this should be called if succeeded
  p_reject_resolve: null, //  this should be called if failed.
  stopRequested: false, //user wanted to stop playing the current test.
  rangeGranularity: 0.1,

  init: function () {
    var self = this;
    self.play = document.querySelector('#pauseCtx'); //class  .play
    // var stop = document.querySelector('.stop');

    self.e_playbackControl = document.querySelector('.playback-rate-control');
    self.e_playbackValue = document.querySelector('.playback-rate-value');
    // self.e_playbackControl.setAttribute('disabled', 'disabled');

    self.e_loopstartControl = document.querySelector('#loopstart-control');
    self.e_loopstartValue = document.querySelector('.loopstart-value');
    // self.e_loopstartControl.setAttribute('disabled', 'disabled');

    self.e_loopendControl = document.querySelector('#loopend-control');
    self.e_loopendValue = document.querySelector('.loopend-value');
    // self.e_loopendControl.setAttribute('disabled', 'disabled');
    self.play.onclick = function () {
      try {
        self.onPauseResumeClicked();
      } catch (e) {
        console.log("52", e);
      }
    };

    //stop is not really needed for this application.
    // stop.onclick = function () {
    //   //if use stop, then the source can not be reused
    //   if (false) {
    //     source.stop(0);
    //     play.removeAttribute('disabled');
    //     e_playbackControl.setAttribute('disabled', 'disabled');
    //     e_loopstartControl.setAttribute('disabled', 'disabled');
    //     e_loopendControl.setAttribute('disabled', 'disabled');
    //     dataReady = false;
    //     //TODO: or we can just reload the current data.
    //     isStopped = true;
    //     getDataAudio(pathCurrent);
    //   } else { //suspend the context


    //   }
    // };

    self.e_playbackControl.oninput = function () {
      //TODO: better to have a reset button.
      self.source.playbackRate.value = self.playSpeed = self.e_playbackControl.value; //TODO: this should be put off
      self.e_playbackValue.innerHTML = self.e_playbackControl.value;
    };

    self.e_loopstartControl.oninput = function () {
      // self.source.loopStart = loopStart = self.e_loopstartControl.value;
      var v = self.e_loopstartControl.value; //it is a %. no! it is not a %
      self.onLoopStartChanged(v);
    };

    self.e_loopendControl.onchange = function () {
      // self.source.loopEnd = self.loopEnd = self.e_loopendControl.value;
      var v = self.e_loopendControl.value; //it is a %. not is not a %, but *10
      self.onLoopEndChanged(v);
    };


    // e_startBtn = document.querySelector('button:nth-of-type(1)');
    // // susresBtn = document.querySelector('#pauseCtx'); //button:nth-of-type(2).   the pause and resume is useless for this application.
    // e_stopBtn = document.querySelector('button:nth-of-type(2)'); //button:nth-of-type(3)


    // // susresBtn.setAttribute('disabled', 'disabled');
    // e_stopBtn.setAttribute('disabled', 'disabled');
    // e_startBtn.onclick = function () {
    //   e_startBtn.setAttribute('disabled', 'disabled');
    //   // susresBtn.removeAttribute('disabled');
    //   e_stopBtn.removeAttribute('disabled');

    //   // create web audio api context
    //   AudioContext = window.AudioContext || window.webkitAudioContext;
    //   audioCtx = new AudioContext();

    //   // create Oscillator and gain node
    //   var oscillator = audioCtx.createOscillator();
    //   var gainNode = audioCtx.createGain();

    //   // connect oscillator to gain node to speakers

    //   oscillator.connect(gainNode);
    //   gainNode.connect(audioCtx.destination);

    //   // Make noise, sweet noise
    //   oscillator.type = 'square';
    //   oscillator.frequency.value = 100; // value in hertz
    //   oscillator.start(0);

    //   gainNode.gain.value = 0.1;

    //   // report the state of the audio context to the
    //   // console, when it changes

    //   audioCtx.onstatechange = function () {
    //     console.log(audioCtx.state);
    //   }
    // };

    // suspend/resume the audiocontext

    // susresBtn.onclick = function () {
    //   if (audioCtx.state === 'running') {
    //     audioCtx.suspend().then(function () {
    //       susresBtn.textContent = 'Resume context';
    //     });
    //   } else if (audioCtx.state === 'suspended') {
    //     audioCtx.resume().then(function () {
    //       susresBtn.textContent = 'Suspend context';
    //     });
    //   }
    // }

    // close the audiocontext
    // e_stopBtn.onclick = function () {
    //   audioCtx.close().then(function () {
    //     e_startBtn.removeAttribute('disabled');
    //     // susresBtn.setAttribute('disabled', 'disabled');
    //     e_stopBtn.setAttribute('disabled', 'disabled');
    //   });
    // };
  },
  number4present: function (v) { //turn 14.000000000001 into "14.0"
    var s = "" + v;
    var iloc = s.indexOf(".");
    if (iloc != -1) {
      s = s.substring(0, iloc + 2);
    }
    return s;
  },
  onLoopStartChanged: function (v) {
    var self = this;
    self.loopStart = v * self.rangeGranularity;// Math.floor(v * self.songLength / 100);
    self.e_loopstartValue.innerHTML = self.number4present(self.loopStart);
  },
  onLoopEndChanged: function (v) {
    var self = this;
    self.loopEnd = v * self.rangeGranularity; //Math.ceil(v * self.songLength / 100);
    //I can not set it to self.source.loopEnd since at this point in time, source might be null
    if (self.source) {
      self.source.loopEnd = self.loopEnd;
    }
    //now present the loopEnd
    self.e_loopendValue.innerHTML = self.number4present(self.loopEnd);
  },
  onPauseResumeClicked: function () {
    var self = this;
    if (self.dataReady) {
      if (self.isStarted) {
        if (self.audioCtx.state === 'running') {
          self.pauseCtx();
        } else if (self.audioCtx.state === 'suspended') {
          self.resumeCtx();
        } else {
          console.log("unprocessed state:" + self.audioCtx.state);
        }
      } else {
        self.tsStartCtx = self.audioCtx.currentTime + 1;
        var startSucceeded = false;
        if (false) {
          // console.log(when + " " + source.loopStart + "->" + source.loopEnd);
          /* be noted that: loop start and end works after play started.
           but seems not working when set before play started.
           it works. according to the web audio api specification, the offset & duration is always being used.
           if the loop region is inside the start region, then it will be looped.
           */
          // source.start(when); //, offset, duration
          //the built in loop is not good. there is no delay before rewind to the loopStart
          var doLoop = true; //TODO: get it from control
          if (doLoop) {
            self.source.loopStart = self.loopStart;  //put it off
            self.source.loopEnd = self.loopEnd;// songLength;
            self.source.loop = true;
          } else {
            self.source.loopStart = self.loopStart;  //put it off
            self.source.loopEnd = self.loopEnd;// songLength;
            self.source.loop = false;
          }
          self.source.playbackRate.value = self.playSpeed; // self.e_playbackControl.value; //TODO: put this off
          var when = self.tsStartCtx, offset = self.loopStart, duration = self.loopEnd - self.loopStart;
          console.log(when + " " + offset + "+" + duration);
          // source.start(when, offset, duration); //with this, does not do loop
          self.isStopped = false;
          self.source.start(when); //with this, do loop?
        } else {
          //the built in loop is not good. there is no delay before rewind to the loopStart
          //TODO: sometimes, self.source is null. 
          //since every time, self.source is reinited, at that time, it is null
          if (self.source) {
            self.source.loop = false;
            self.source.playbackRate.value = self.playSpeed; // self.e_playbackControl.value; //TODO: put this off
            // console.log("play speed:" + self.source.playbackRate.value);
            var when = self.tsStartCtx, offset = self.loopStart, duration = self.loopEnd - self.loopStart;
            // console.log(when + " " + offset + "+" + duration);
            self.isStopped = false;
            self.source.start(when, offset, duration); //with this, does not do loop
            startSucceeded = true;
          } else {
            console.log("self.source is null");
          }
        }
        if (startSucceeded) {
          self.onPlayStarted();
        }
      }
    } else {
      var msg = "please load data before play";
      console.log(msg);   //TODO: some times, this is printed. why??
      // alert(msg);
    }
  },
  displayTime: function () {
    if (true) { //discard all stuff related to this. since it is not meaningful.
      return;
    }
    if (audioCtx && audioCtx.state !== 'closed') {
      var tsStart = loopStart;
      var duration = loopEnd - loopStart;
      var dt = audioCtx.currentTime - tsStart;
      if (false) {
        dt %= duration;
      } else {
        //this logic is just not good. I need other approach. I need event when loop starts and loop ends.
        //   with web Audio specification, there is no events, so this should be discarded.
        var n = Math.floor(dt / duration);
        dt -= n * duration;
      }
      e_timeDisplay.textContent = '' + (tsStart + dt).toFixed(3);
    } else {
      e_timeDisplay.textContent = 'No context exists.'
    }
    if (isStopped) {
    } else {
      requestAnimationFrame(displayTime);
    }
  },
  onDataReady: function () {
    console.log("on data ready");
    var self = this;
    self.dataReady = true;
    self.isStarted = false;
    self.stopRequested = false;
    self.p_stop_resolve = null;
    self.p_reject_resolve = null;
    self.songLength = self.buffer.duration;
    console.log("decoded length:" + self.songLength);
    //I don't use percentage, the granularity is 0.1 second
    var rangeMax = Math.ceil(self.songLength * 10); //floor
    self.e_playbackControl.removeAttribute('disabled'); //the rate
    self.e_loopstartControl.removeAttribute('disabled');
    self.e_loopstartControl.setAttribute('max', rangeMax);
    self.onLoopStartChanged(self.e_loopstartControl.value = 0);
    self.e_loopendControl.removeAttribute('disabled');
    self.e_loopendControl.setAttribute('max', rangeMax);
    //self.e_loopendControl.setAttribute('value', rangeMax);
    //self.e_loopendControl.onchange();  
    self.onLoopEndChanged(self.e_loopendControl.value = rangeMax);
    document.querySelector('#songLength').innerHTML = "/" + self.songLength.toFixed(2);
    self.nTimes = 0;
    // self.startPlay();
  },
  decodeDataAndPlay: function (audioData) { //do not pass the test object into the player.
    //as a player, we don't need to know the test object.
    //what we need to know is the audio data.
    var self = this;
    self.dataReady = false;
    if (false) {
      self.decodeData(audioData, function () {
        self.play.onclick();
        self.resumeCtx();
      });
      return Promise.reject("should not be used");
    } else {
      return self.decodeData(audioData).then(buffer => {
        self.buffer = buffer;
        self.onDataReady();
        self.startPlay();
      });
    }
  },
  decodeData: function (audioData, fnReady) {
    var self = this;
    //audioCtx can be reused, or we must create a new one?
    if (this.audioCtx) {
      console.log("will reuse the context");
    } else {
      if (window.webkitAudioContext) {
        this.audioCtx = new window.webkitAudioContext();
      } else {
        this.audioCtx = new window.AudioContext();
      }
    }
    if (false) {
      self.audioCtx.decodeAudioData(audioData, function (buffer) {
        // myBuffer = buffer; //remove this unneeded variable.
        self.buffer = buffer;
        self.onDataReady();
      }, function (e) {
        "Error with decoding audio data" + e.error
      });
    } else {
      return self.audioCtx.decodeAudioData(audioData);
    }
  },
  onPlayEnded: function () {
    var self = this;
    self.isStarted = false;
    self.nTimes++;
    // console.log("played " + self.nTimes + " times");
    try {
      // self.source.stop(0);
      self.source.disconnect(self.audioCtx.destination);
      self.source = null;
    } catch (e) {
      console.log(e);
    }
    if (self.stopRequested) {
      self.p_stop_resolve(true);
    } else {
      setTimeout(function () {
        if (self.stopRequested) {
          self.p_stop_resolve(true);
        } else {
          self.startPlay();
        }
      }, self.delayUponEnded);
    }
  },
  startPlay: function () {
    var self = this;
    // self.loopStart = 0;  //put this off
    // self.loopEnd = self.songLength;
    self.audioCtx.destination.disconnect();
    self.source = self.audioCtx.createBufferSource(); //is this the only place to create one? Yes!
    self.source.buffer = self.buffer;
    self.source.connect(self.audioCtx.destination);
    self.source.onended = function () {
      self.onPlayEnded();
    };
    // source.loopStart = 0;  //put it off
    // source.loopEnd = songLength;
    // source.loop = true;
    //      if (fnReady) {
    //        fnReady();
    //      }
    self.play.onclick();
    self.resumeCtx();
  },
  onPlayStarted: function () {
    var self = this;
    try {
      self.isStarted = true;
      // play.setAttribute('disabled', 'disabled');
      //enable them when the length of the song is known.
      // self.e_playbackControl.removeAttribute('disabled'); //the rate
      // self.e_loopstartControl.removeAttribute('disabled');
      // self.e_loopendControl.removeAttribute('disabled');
      self.e_timeDisplay = document.querySelector('#tsCurrent'); //TODO: use id instead //tag 'p'
      self.play.textContent = 'Suspend context'; //susresBtn
      tester.presentTestInfo();
      // self.displayTime();
      // tester.presentTiming(); //memory retention curve related stuff
    } catch (e) {
      console.log(e);
    }
  },
  closeSource: function () {
    var self = this;
    var p;
    if (self.isStarted) { //the meaning of it? source can not be started twice.
      var state = self.audioCtx.state;
      if (state === "running" || state === "suspended") {
        p = new Promise((resolve, reject) => {
          self.p_stop_resolve = resolve;
          self.p_reject_resolve = reject;
          self.stopRequested = true; //self.source.stop(0); //then onend handler will be called
          // self.source.disconnect(self.audioCtx.destination); in the onend handler
        });
        if (state === "suspended") {
          //I have to make it up, otherwise, it won't be called.
          self.onPlayEnded();
        }
      } else {
        var s = state === "closed" ? "state is closed" : "unknown state:" + state;
        p = Promise.reject(s);
      }
    } else {
      p = Promise.resolve(true);
    }
    return p;
  },
  pauseCtx: function () {
    var self = this;
    if (self.audioCtx) {
      var state = self.audioCtx.state; // self.audioCtx.onstatechange  handler can be added
      if (state === 'running') {
        self.audioCtx.suspend().then(function () {
          self.isSuspended = true;
          // self.isPlaying = false;
          self.play.textContent = 'Resume context'; //susresBtn
        });
      } else {
        console.log("state is not running, but " + state);
      }
    }
  },
  resumeCtx: function () {
    var self = this;
    var state = self.audioCtx.state;
    if (state === 'suspended') {
      self.audioCtx.resume().then(function () {
        // self.isPlaying = true;
        self.isSuspended = false;
        self.play.textContent = 'Suspend context'; //susresBtn
      });
    } else {
      // console.log("state is not suspended, but " + state);
    }
  },
};

var tester = {
  /*
   at present, for each test, there is only 2 possible results: good or bad.
   */
  e_good: null,
  e_bad: null,
  // tests: [], stop using this one, use testCurrent, testNext & testNext2 instead.
  autoStartNext: true, //when a test is done, we just start the next one automatically.
  preloadNext: false, //true, //false, //TODO: do preload.
  // dataPreloaded: null,  //in promise, so not needed.
  // dataPreloaded_p: null, moved into self.testNext. No. removed!
  // pathLast: null, //the one is loading or just loaded. if paralle, then this miht be different from player.pathCurrent.
  /* extra field to test object: flagsLoad=false/true, dataAudio
flagsLoad: use 0,1,2,3,4. 1: loading info, 2: load info Succeeded, 4: load info Failed,   128: loading audio data, 256: succeeded, 512: failed.  
  */
  testNext2: null,
  testNext: null,
  testCurrent: null, //the test is being played.
  idtestLoading: -1,
  // pathPlaying: null, //the one is being played. TODO: remove this one.
  init: function () {
    var self = this;
    self.e_good = document.querySelector('#result_good');
    self.e_good.onclick = function () {
      player.pauseCtx();
      document.querySelector('#testkey').focus();
    };
    self.e_bad = document.querySelector('#result_bad');
    self.e_bad.onclick = function () {
      self.testInfoPresented = false;
      self.presentTestInfo();
    };
  },
  presentTestNext_do: function () {
    var self = this;
    var e = document.querySelector('#info');
    e.innerHTML = "loading";
    return self.presentTest(); //present the next test
  },
  presentTestNext: function () {
    var self = this;
    try {
      // player.pauseCtx(); //this is not good. the source should be closed
      self.presentTestNext_do().catch(e => {
        alert("close error:" + e);
      });
    } catch (e) {
      console.log(e);
      alert(e);
    }
  },
  mergeSelected: function () {
    var self = this;
    var selected = self.getKPsSelected();
    var url = "kp?act=mergeKPs&kpids=" + selected;
    self.postReq(url).then(ojson => {
      if (ojson.ireason != 0) {
        throw new Error("result for mergeSelected:" + JSON.stringify(ojson));
      } else {
        self.updateTestInfo();
      }
    }).catch(e => {
      console.log("exception 457:");
      console.log(e);
    });
  },
  selectAllKPs: function () {
    var self = this;
    for (var kpid in self.testCurrent.kps) {
      var kp = self.testCurrent.kps[kpid];
      // console.log(kpid + ":" + kp);
      if (kp.deleted) { } else {
        var eid = "kp" + kpid;
        var e = document.querySelector('#' + eid);
        e.checked = true;
      }
    }
  },
  //TODO: send the test result to server. player.nTimes played.
  // the name is not good. here, we are sending bads to the server.
  submitNew: function () {
    var self = this;
    var selected = self.getKPsSelected();
    var url = "user?act=result&idtest=" + self.testCurrent.id + "&bads=" + selected;
    if (false) {
      self.postReq(url).then(ojson => {
        return self.presentTestNext_do();
      }).catch(e => {
        console.log(e);
        alert(e);
      });
    } else {
      if (self.preloadNext) {
        self.presentTestNext_do().then(tf => {
          return self.postReq(url);
        }).then(ojson => {
          user.olevels = ojson.levels;
          console.log(ojson.kpdone);
          return user.updateLevelInfo();
        }).then(tf => {
          //TODO: I need an array of testid's. and then I load each ETest with its id.
          //   only with this approach can I do preload.
          // if I do not specifiy the testid, "preload" in fact becomes "reload".
          //         since before test result is submitted, the same ETest will be returned.
          console.log("will preload:" + self.preloadNext);
          if (self.preloadNext) { //TODO: enable this.
            // self.prepareNextTest();
            return self.preloads();
          } else return Promise.resolve(false);
        }).catch(e => {
          console.log(e);
          alert(e);
        });
      } else { //no preload
        self.postReq(url).then(ojson => {
          user.olevels = ojson.levels;
          console.log(ojson.kpdone);
          return user.updateLevelInfo();
        }).then(tf => {
          return self.presentTestNext_do();
        }).catch(e => {
          console.log(e);
          alert(e);
        });
      }

    }
  },
  getKPsSelected: function () {
    var self = this;
    if (self.testCurrent) {
    } else {
      alert("please click startTest");
    }
    var selected = "";
    var first = true;
    for (var kpid in self.testCurrent.kps) {
      var kp = self.testCurrent.kps[kpid];
      // console.log(kpid + ":" + kp);
      if (kp.deleted) { } else {
        var eid = "kp" + kpid;
        var e = document.querySelector('#' + eid);
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
    }
    return selected;
  },
  postReq: function (urlSuffix) { //if we do not care the result, this method can be used.
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + urlSuffix;
        request.open('POST', url, true);
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
  setCurrent: function () {
    var self = this;
    try {
      var set = false;
      if (self.isReady(self.testNext)) {
        self.testCurrent = self.testNext;//  self.pathPlaying = self.pathLast;
        self.testNext = self.testNext2;
        self.testNext2 = null;
        set = true;
      } else if (self.isReady(self.testNext2)) {
        self.testCurrent = self.testNext2;//  self.pathPlaying = self.pathLast;
        self.testNext2 = null; //this is not needed. indeed needed.
        set = true;
      } else {
        // throw "none is ready 546";
      }
      if (set) {
        return Promise.resolve(true);
      } else {
        return self.preloads().then((tf) => { //TODO: now the data is save to self.testCurrent.dataAudio
          if (tf) {
            //when audio data is ready, we change it from self.testNext to self.testCurrent.
            if (self.isReady(self.testNext)) {
              self.testCurrent = self.testNext;//  self.pathPlaying = self.pathLast;
              self.testNext = self.testNext2; //this is not needed. indeed needed.
              self.testNext2 = null; //this is not needed. indeed needed.
            } else if (self.isReady(self.testNext2)) {
              self.testCurrent = self.testNext2;//  self.pathPlaying = self.pathLast;
              self.testNext2 = null; //this is not needed. indeed needed.
            } else {
              throw "none is ready 565";
            }
            return true;
          } else throw Error("expecting true, but " + tf);
        });
      }
    } catch (e) {
      return Promise.reject(e);
    }
  },
  presentTest: function () { //TODO: ?? should I change its name to presentTestNext?
    var self = this;
    // var p;
    // if (self.preloadNext && self.dataPreloaded_p) {  //TODO: utilize preloadNext
    //   p = self.dataPreloaded_p;
    //   self.dataPreloaded_p = null;
    // } else {
    // }
    //prepareNextTest
    return self.setCurrent().then(tf => {
      console.log("current:" + self.testCurrent.id);
      return player.closeSource();
    }).then(tf => {
      self.clearTestInfo();
      return player.decodeDataAndPlay(self.testCurrent.dataAudio);
      // }).then(tf => {
    }).catch(ex => {
      var msg = "";
      // msg+= "#oftests:";
      // if (self.tests) {
      //   msg += self.tests.length;
      // } else {
      //   msg += "null";
      // }
      msg += " current:";
      if (self.testCurrent) {
        msg += self.testCurrent.fnAudio;
      } else {
        msg += "null";
      }
      msg += " next:";
      if (self.testNext) {
        msg += self.testNext.fnAudio
      } else {
        msg += "null";
      }
      msg += " next2:";
      if (self.testNext2) {
        msg += self.testNext2.fnAudio
      } else {
        msg += "null";
      }
      msg += " ex:" + ex;
      console.log(msg);
      console.log(ex);
      var e = document.querySelector('#info');
      e.innerHTML = msg;
      //document.querySelector('#songLength').innerHTML = ex;
      // alert("failed to load data:" + self.pathLast + ":" + e);
    });
  },
  // setNextTest: function () { //TODO: now this is simpler.
  //   var self = this;
  //   try {
  //     if (self.testNext) {
  //       return Promise.resolve(self.testNext);
  //     } else {
  //       var tests = self.tests;
  //       if (tests.length > 0) { //TODO: move this check outside.
  //         // idx = Math.floor(Math.random() * tests.length);
  //         var ilimit = self.tests.length;
  //         for (var i = 0; i < ilimit; i++) {
  //           var otest = tests[i];
  //           if (otest.flagsLoad && otest.dataAudio) {
  //             self.testNext = otest;
  //             self.tests.splice(i, 1);
  //             return Promise.resolve(otest);
  //           }
  //         }
  //         //none is ready.
  //         return self.preload().then(nReady => {
  //           if (nReady > 0)
  //             return self.setNextTest();
  //           else
  //             throw new Error("none is ready 619");
  //         });
  //       } else {
  //         return self.getTests().then(nReady => {
  //           if (nReady > 0)
  //             return self.preload();
  //           else
  //             throw new Error("none is ready2");
  //         }).then(tf => {
  //           //results saved to self.tests.
  //           return self.setNextTest(); //TODO: recursive is dangerous
  //         });
  //       }
  //     }
  //   } catch (e) {
  //     return Promise.reject(e);
  //   }
  // },
  // prepareNextTestAudioData: function (otest) { //what is promised? rename getDataAudio0
  //   var self = this;
  //   var p = null;
  //   var path = otest.fnAudio;
  //   console.log("audio path:" + path);
  //   p = self.getDataAudio(otest); //path
  //   // if (self.tests.length == 0) {
  //   //   self.getTests1().then(tf => { //getTests
  //   //     //results saved to self.tests.
  //   //     // alert("all test has been preloaded, now what to reload?");
  //   //   }).catch(e => {
  //   //     alert(e);
  //   //   });
  //   // }
  //   return p;//self.testNext.dataPreloaded_p =
  // },
  // use XHR to load an audio track, and
  // decodeAudioData to decode it and stick it in a buffer.
  // Then we put the buffer into the source
  /*
   path is relative, such as 'outfoxing.mp3'. can it be absolute?  
   var path = 'outfoxing.mp3';
   self.getDataAudio(path);
   dataAudio is promised, and it is also saved to otest.dataAudio
   */
  getDataAudio: function (otest) { //path
    var self = this;
    // self.pathLast = path;
    var path = otest.fnAudio;
    console.log("audio path:" + path);
    return new Promise((resolve, reject) => {
      try {
        if (otest.dataAudio) { //self.testNext is otest
          resolve(otest.dataAudio);
        } else {
          // isStarted = false;  //TODO: ensure this is right in player
          //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
          var request = new XMLHttpRequest();
          var url = webPath + "files?act=audio&fn=" + otest.fnAudio;
          console.log(url);
          request.open('GET', url, true); //path
          request.responseType = 'arraybuffer';
          request.onload = function () {
            var res = request.response;
            var msg = "data loaded, will decode it. typeof:" + typeof (res) + " type:" + request.responseType;
            msg += " arraybuffer:" + (res instanceof ArrayBuffer);
            msg += " string:" + (res instanceof String);
            // msg += " domstring:" + (res instanceof DOMString); //ReferenceError: DOMString is not defined
            msg += " doc:" + (res instanceof Document);
            msg += " blob:" + (res instanceof Blob);
            // console.log(msg); //why false?
            // console.log(res);
            otest.dataAudio = request.response;
            otest.flagsLoad |= 256;
            resolve(request.response);
            // if(self.preloadNext){
            //   self.dataPreloaded=request.response;
            // }
            // self.decodeData(request.response);

          };
          // request.onFailed;  //TODO: ....
          request.send();
        }
      } catch (e) {
        reject(e);
      }
    });
  },
  updateTest: function (testid) { //TODO: remove this function
    var self = this;
    // return new Promise((resolve, reject) => {
    //   try {
    //     var request = new XMLHttpRequest();
    //     var url = webPath + "test?act=test&testid=" + self.testCurrent.id;
    //     request.open('GET', url, true); 
    //     request.responseType = 'json';
    //     request.onload = function () {
    //       var ojson = request.response;
    //       if (ojson.ireason) {
    //         reject(url + " returns: " + JSON.stringify(ojson));
    //       } else {
    //         // self.tests[0] = ojson; //this will cause self.testCurrent to be the next test again.
    //         self.testCurrent = ojson;
    //         resolve(true);
    //       }
    //     };
    //     // request.onFailed;  //TODO: ....
    //     request.send();
    //   } catch (e) {
    //     reject(e);
    //   }
    // });
    return self.getTestInfo(testid).then(ojson => {
      // self.tests[0] = ojson; //this will cause self.testCurrent to be the next test again.
      var dataAudio = null;
      if (self.testCurrent) {
        if (self.testCurrent.id == ojson.id) {
          dataAudio = self.testCurrent.dataAudio;
        }// else throw Error("id " + self.testCurrent.id + " is not expected:" + ojson.id);
      }
      self.testCurrent = ojson;
      ojson.flagsLoad |= 2; //info is loaded
      if (dataAudio) {
        ojson.dataAudio = dataAudio;
        ojson.flagsLoad |= 256; //data is loaded
      }
    });
  },
  getTestInfo: function (testid) {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + "test?act=test&testid=" + testid;
        request.open('GET', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var ojson = request.response;
          if (ojson.ireason) {
            reject(url + " returns: " + JSON.stringify(ojson));
          } else {
            // // self.tests[0] = ojson; //this will cause self.testCurrent to be the next test again.
            // self.testCurrent = ojson;
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
  getTests: function () {
    var self = this;
    if (self.testNext && self.testNext2) {
      return Promise.resolve(true);
    }
    return self.getTests_do();
  },
  getTests_do: function () {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var e = document.querySelector('#startReview');
        var url = webPath + "user?act=tests&t=" + new Date().getTime(); //webPath + "test"
        if (e.checked) {
          url += "&startReview=true";
          e.checked = false;
        }
        request.open('GET', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var ojson = request.response;
          if (ojson.ireason) {
            reject(ojson.sreason);
          } else {
            //if (e.checked) 
            {
              e = document.querySelector('#todo');
              e.innerHTML = ojson.todo;
            }
            ojson = ojson.tests; //originally, an array of testid; now an array of objects.
            //self.tests = request.response.tests;
            for (var i = 0; i < ojson.length; i++) {
              var testid = ojson[i].id;//.id; //TODO: at present, it is the whole object.
              if (true) {
                if (self.testCurrent) {
                  if (self.testCurrent.id == testid)
                    continue;
                } else {
                  // self.testCurrent = { id: testid };
                }
                if (self.testNext) {
                  if (self.testNext.id == testid)
                    continue;
                  if (self.testNext2) {
                    if (self.testNext2.id == testid)
                      continue;
                    else break; //throw away the rest if any
                  } else {
                    self.testNext2 = ojson[i];// { id: testid };
                  }
                } else {
                  self.testNext = ojson[i];// { id: testid };
                }
              } else {
                for (var j = 0; j < self.tests.length; j++) {
                  if (self.tests[j].id == testid) {
                    //found it
                  } else {
                    self.tests.push({ id: testid });
                  }
                }
              }
            }
            resolve(ojson.length > 0);
          }
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  },
  /*
  self.tests is ready, load testInfo and dataAudio
  return the first loaded test(just the test info, does not include dataAudio)
  return true if any one of self.textNest or self.testNext2 is ready.
  */
  preload: function (otest) {
    var self = this;
    if (true) {
      try {
        if (otest.flagsLoad) {
          //is loading
          if (otest.dataAudio) { //completely loaded
            return Promise.resolve(true);
          }
          if (otest.resolves) {
          } else {
            otest.resolves = [];
            otest.rejects = [];
          }
          return new Promise((resolve, reject) => {
            otest.resolves.push(resolve);
            otest.rejects.push(reject);
          });
        } else {
          //not loading
          otest.flagsLoad = 1;
        }
        // var ilimit = self.tests.length;
        // for (var i = 0; i < ilimit; i++) {
        //   var otest = self.tests[i];
        //   if (otest.flagsLoad) {
        //     return Promise.resolve(true);
        //   } else {
        //     if (self.testidLoading == test.id)
        //       continue;
        //     self.testidLoading = test.id;
        //     break;
        //   }
        // }
        //TODO: stop using self.testidLoading
        var pInfo;
        if (otest.flagsLoad & 2) { //info has been loaded.
          pInfo = Promise.resolve(otest);
        } else {
          pInfo = self.getTestInfo(otest.id).then(otestNew => {
            if (false) {
              for (var i = 0; i < ilimit; i++) {
                var test = self.tests[i];
                if (test.id == self.otest.id) {
                  var o = self.tests[i];
                  self.tests[i] = otestNew;
                  otestNew.dataAudio = o.dataAudio;
                  otestNew.flagsLoad = true;
                  return otestNew; //!!otestNew.dataAudio;
                }
              }
              throw "should not get here";
            }
            var o = null;
            if (self.testNext) {
              if (self.testNext.id == otestNew.id) {
                o = self.testNext;
                otestNew.kp4 = o.kp4;
                self.testNext = otestNew;
              }
            } else if (self.testNext2) {
              if (self.testNext2.id == otestNew.id) {
                o = self.testNext2;
                otestNew.kp4 = o.kp4;
                self.testNext2 = otestNew;
              }
            } else if (self.testCurrent) {
              if (self.testCurrent.id == otestNew.id) {
                o = self.testCurrent;
                otestNew.kp4 = o.kp4;
                self.testCurrent = otestNew;
              }
            }
            if (o) {
              otestNew.flagsLoad = o.flagsLoad;
              otestNew.flagsLoad |= 2;
              if (o.dataAudio) {
                otestNew.dataAudio = o.dataAudio;
                otestNew.flagsLoad |= 256;
              }
              return otestNew;
            } else {
              throw otest.id + " is missing";
            }
          });
        }
        return pInfo.then(otest => {
          //do I have to check tf? always true, otherwise rejected.
          if (otest.dataAudio) {
            otest.flagsLoad |= 256;
            return Promise.resolve(otest.dataAudio);
          } else
            return self.getDataAudio(otest);
        }).then(dataAudio => {
          otest.flagsLoad |= 256;
          console.log((dataAudio instanceof ArrayBuffer) + " audio data:" + dataAudio + " " + (otest.flagsLoad & 258));
          // self.testidLoading = -1;
          // return self.preload();
          return true;
        });
      } catch (e) {
        console.log(e);
        return Promise.reject(e);
      }
    }
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + "user?act=test";
        request.open('GET', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var otestNew = request.response;
          console.log(otestNew);
          if (otestNew.ireason) {
            reject(url + " returns: " + JSON.stringify(otestNew));
          } else {
            self.tests[0] = otestNew; // request.response.tests;
            resolve(true);
          }
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  },
  //getTest,prepareNextTest is the old name
  preloads: function () { //TODO: remove this one, use preload instead
    var self = this;
    // return self.setNextTest();
    // .then(tf => {
    //   //do I have to check tf? always true, otherwise rejected.
    //   var otest = self.testNext;
    //   return self.getDataAudio(otest);
    // });
    return self.getTests().then(tf => {
      if (false) {
        var ap = [];
        if (self.testNext) ap.push(self.preload(self.testNext));
        if (self.testNext2) ap.push(self.preload(self.testNext2));
        return Promise.any(ap);
      }
      if (self.isReady(self.testNext) || self.isReady(self.testNext2)) return Promise.resolve(true);
      if (self.testNext) return self.preload(self.testNext);
      if (self.testNext2) return self.preload(self.testNext2);
      throw Error("should not happen"); //TODO: happened. 
    });
  },
  isReady: function (otest) {
    if (otest) {
      console.log("flags for " + otest.id + ":" + otest.flagsLoad);
      return (otest.flagsLoad & 2) && (otest.flagsLoad & 256);
    }
    return false;
  },
  updateTestInfo: function () {
    var self = this;
    return self.updateTest(self.testCurrent.id).then(tf => {
      self.testInfoPresented = false;
      self.presentTestInfo();
    });
  },
  clearTestInfo: function () {
    var e = document.querySelector('#info');
    e.innerHTML = "";//clear any info for previous test
    e = document.querySelector('#kpsA');
    e.innerHTML = "";
  },
  testInfoPresented: false,
  presentTestInfo: function () {
    var self = this;
    if (self.testInfoPresented)
      return;
    self.testInfoPresented = true;
    try {
      //I will have to present some info about this test.
      var e = document.querySelector('#info');
      // console.log(self.testCurrent);
      var s4 = "";
      try {
        s4 = "(" + self.testCurrent.kp4.kpid + ")";
      } catch (ex) {
        console.log(ex);
      }
      var html = self.testCurrent.id + s4 + ":" + self.testCurrent.info;
      html += '<button onclick="tester.editTest()">Edit</button>';
      e.innerHTML = html; //.fn; //.pathPlaying;// player.pathCurrent;
      //now present those knowledge points
      //we will allow the user to tell us which points is newly learned.
      self.presentKPs(false, self.testCurrent.kps);
      self.presentTiming();
    } catch (ex) {
      console.log(ex);
    }
  },
  inLeftList: function (kpid) { //TODO: seems there is a function with better performance.
    var self = this;
    var okps = self.testCurrent.kps;
    for (var kpidt in okps) {
      if (kpid === kpidt) {
        return true;
      }
    }
    return false;
  },
  format_m: function (m) { //minutes since 1970
    var d = new Date(m * 1000 * 60);
    return d.getFullYear() + "." + d.getMonth() + "." + d.getDate() + "-" + d.getHours() + ":" + d.getMinutes();
  },
  getDT: function (dt) { //in minutes
    var s = dt % 60;
    dt = Math.floor(dt / 60); //hours
    if (dt > 0) {
      s = (dt % 24) + ":" + s;
      dt = Math.floor(dt / 24); //days
      if (dt > 0) {
        s = dt + "d " + s;
      }
    }
    return s;
  },
  presentTiming: function () {
    var self = this;
    var right = true;
    var tag, eid
    if (right) {
      tag = "R"; eid = "kpsB";
    } else {
      tag = "L"; eid = "kpsA";
    }
    var e = document.querySelector('#' + eid);
    // e.innerHTML = "";
    var otime = self.testCurrent.kp4;
    console.log(otime.scheduled);
    var aotimes = otime.tested;
    for (var i = 0; i < aotimes.length; i++) {
      var o = aotimes[i];
      // console.log(o.lts + " " + o.good);
    }
    otime.dt = otime.scheduled - otime.t1;
    var dt_s = self.getDT(otime.dt);
    html = "<li>" + self.format_m(otime.t1) + "+" + dt_s + "=" + otime.scheduled + "(" + self.format_m(otime.scheduled) + ")</li>";
    for (var i = aotimes.length - 1; i >= 0; i--) {
      var o = aotimes[i];
      // console.log(o.lts + " " + o.good);
      var html0 = '<li id="li' + eid + '">' + self.format_m(o.lts) + " " + o.good + '</li>';
      html += html0;
    }
    e.innerHTML = html;
  },
  presentKPs: function (right, okps) {
    var self = this;
    var tag, eid
    if (right) {
      tag = "R"; eid = "kpsB";
    } else {
      tag = "L"; eid = "kpsA";
    }
    var e = document.querySelector('#' + eid);
    // e.innerHTML = "";
    html = "";
    for (var kpid in okps) {
      var kp = okps[kpid];
      console.log(kpid + ":" + kp);
      if (kp.deleted) { } else {
        var isfor = "";
        try {
          if (self.testCurrent.kp4.kpid == kpid) {
            isfor = "*";
          }
        } catch (e) {
          console.log(e);
        }
        var eid = "kp" + kpid; //+ tag. I do not need tag, since I can ensure there is only 1. the case that the same KP present on both sides will not happen.
        var html0 = '<li id="li' + eid + '"><input type="checkbox" id="' + eid + '"/><label for="' + eid + '">' + (isfor) + kpid + ":" + kp.levels[user.levelSystem] + " " + kp.desc + '</label>';
        if (right) {
          // html0 += '<button onclick="tester.addKP(' + kpid + ')">Add</button><button onclick="tester.editKP(' + kpid + ')">Edit</button>';
        } else {
          // html0 += '<button onclick="tester.deleteKP(' + kpid + ')">Remove</button><button onclick="tester.editKP(' + kpid + ')">Edit</button>';
        }
        html0 += '</li>';
        html += html0;
      }
    }
    e.innerHTML = html;
  },
  editTest: function () {
    var self = this;
    var e = document.querySelector('#info');
    // console.log(self.testCurrent);
    var html = '<input type="text" id="testinfo" value="' + self.testCurrent.info + '"/> <button onclick="tester.editTestDone()">Done</button>';
    e.innerHTML = html; //.fn; //.pathPlaying;// player.pathCurrent;
  },
  editTestDone: function () {
    var self = this;
    var e = document.querySelector("#testinfo");
    var info = e.value;
    var url = "test?act=chgInfo&idtest=" + self.testCurrent.id + "&info=" + encodeURIComponent(info);
    self.postReq(url).then(ojson => {
      self.updateTestInfo();
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  /*
  only to remove the relationship between ETest and EKP. the EKP is not removed from the system.
  */
  deleteKP: function (kpid) {
    var self = this;
    self.postToTestAndUpdate("deleteKP", kpid);
  },
  addKP: function (kpid) {
    var self = this;
    self.postToTestAndUpdate("addKP", kpid);
  },
  postToTestAndUpdate: function (act, kpid) {
    var self = this;
    var url = "test?act=" + act + "&idtest=" + self.testCurrent.id + "&idkp=" + kpid;
    self.postReq(url).then(ojson => {
      self.updateTestInfo();
    }).catch(e => {
      console.log(e);
      alert(e);
    });
    var e = document.querySelector("#likp" + kpid);
    e.parentNode.removeChild(e);
  },
  getEKPbyID: function (kpid) {
    var self = this;
    var kp = self.testCurrent.kps[kpid + ""];
    if (kp) { } else {
      kp = self.okpsR[kpid + ""];
    }
    return kp;
  },
  editKP: function (kpid) {
    var self = this;
    var kp = self.getEKPbyID(kpid);
    var e = document.querySelector("#likp" + kpid);
    var sysName = user.levelSystem;
    e.innerHTML = '<input type="text" id="kp' + kpid + '" value="' + kp.desc + '"/><input type="text" id="kplevel' + kpid + '" value="' + kp.levels[sysName] + '"/> <button onclick="tester.editKPdone(' + kpid + ')">Done</button>';
  },
  editKPdone: function (kpid) {
    var self = this;
    var e = document.querySelector("#kp" + kpid);
    var desc = e.value;
    var kp = self.getEKPbyID(kpid);
    if (desc === kp.desc) {
      p = Promise.resolve({});
    } else {
      var url = "kp?act=chgKPdesc&idkp=" + kpid + "&desc=" + encodeURIComponent(desc);
      p = self.postReq(url);
    }
    p = p.then(ojson => {
      var el = document.querySelector("#kplevel" + kpid);
      var ls = el.value;
      var sysName = user.levelSystem;
      if (ls === kp.levels[sysName]) {
        return Promise.resolve({});
      } else {
        var url = "kp?act=chgKPlevel&idkp=" + kpid + "&sys=" + sysName + "&level=" + encodeURIComponent(ls);
        return self.postReq(url);
      }
    });
    p.then(ojson => {
      self.updateTestInfo();
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
  searchKP: function () {
    var self = this;
    var e = document.querySelector("#kpnew");
    var desc = e.value;
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
  newKPdone: function () {
    //this KP might exist already, so I have to allow the user to choose one of the exists.
    var self = this;
    var e = document.querySelector("#kpnew");
    var desc = e.value;
    var e = document.querySelector("#kplevelnew");
    var level = e.value;
    var url = "test?act=newKP&idtest=" + self.testCurrent.id + "&desc=" + encodeURIComponent(desc) + "&level=" + encodeURIComponent(level);
    self.postReq(url).then(ojson => {
      self.updateTestInfo();
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  // start: function () {
  //   //get a list
  //   var self = this;

  //   self.getTests1().then(tf => { //getTests
  //     return self.prepareNextTest();
  //   }).catch(e => {
  //     alert(e);
  //   });
  // },
  loadTest: function () {
    var self = this;
    var e = document.querySelector('#testid');
    var testid = e.value;
    console.log(testid);
    self.updateTest(testid).then(tf => {
      var otest = self.testCurrent;
      //do I have to check tf? always true, otherwise rejected.
      if (otest.dataAudio) {
        otest.flagsLoad |= 256;
        return Promise.resolve(otest.dataAudio);
      } else
        return self.getDataAudio(otest);
    }).then(dataAudio => {
      var otest = self.testCurrent;
      otest.flagsLoad |= 256;
      console.log((dataAudio instanceof ArrayBuffer) + " audio data:" + dataAudio + " " + (otest.flagsLoad & 258));
      // self.testidLoading = -1;
      // return self.preload();
      return true;
    }).then(tf => {
      console.log("current:" + self.testCurrent.id);
      return player.closeSource();
    }).then(tf => {
      self.testInfoPresented = false;
      self.presentTestInfo(); //self.clearTestInfo();
      return player.decodeDataAndPlay(self.testCurrent.dataAudio);
    }).catch(ex => {
      console.log("exception 1274");
      console.log(ex);
    });

  },
};
var user = {
  name: "jack",
  password: "jack", //TODO: should from uielement
  autheticated: false,
  levelSystem: "misc",
  targetMajor: 0,
  targetMinor: 0,
  actualMajor: 0,
  actualMinor: 0,
  //TODO: login stuff
  getNonce: function () {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + "user?act=nonce&username=" + self.name;
        request.open('GET', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var ojson = request.response;
          console.log(ojson);
          if (ojson.ireason) {
            reject(url + " returns: " + JSON.stringify(ojson));
          } else {
            self.nonce = ojson.nonce;
            self.onPKs(ojson.pks);
            resolve(true);
          }
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  },
  // pNonce: null,
  nonce: null,
  pkChosen: null,
  pkIsChosen: false,
  onPKs: function (pks) {
    var self = this;
    self.pks = pks;
    self.pkChosen = null;
    self.pkIsChosen = false;
    //TODO: I should present PKs, so the user can pick one of them.
    if (pks.length == 1) { //   if we have only 1 PK, then auto chosen.
      self.pkChosen = pks[0];
      self.pkIsChosen = true;
      //TODO: on UI element
    } else {

    }
  },
  onUsernameKnown: function () {
    var self = this;
    self.pks = null;
    self.pkIsChosen = false;
    this.getNonce().then(tf => { //TODO: for temp. should be triggered by UI
      self.onPasswordKnown();
    }).catch(e => {
      console.log(e);
      alert(e);
    });
  },
  onPasswordKnown: function () {
    var self = this;
    var pass = self.password;
    if (self.pks) { //we have got pks
      if (self.pkIsChosen) {
        var pk = self.pkChosen;
        self.authenticate(pk, pass).then(tf => {
          return self.getLevelInfo();
        }).then(tf => {
          return self.updateLevelInfo();
        }).catch(t => {
          alert(t);
        });
      } else {
        alert("please choose one PK from " + self.pks.length);
      }
    }
  },
  authenticate: function (pk, pass) {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + "user?act=authenticate&pk=" + pk + "&sig=" + (pass + self.nonce);
        request.open('POST', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var ojson = request.response;
          console.log(ojson);
          if (ojson.ireason) {
            reject(url + " returns: " + JSON.stringify(ojson));
          } else {
            console.log("authenticated");
            resolve(true);
          }
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  },
  updateLevelInfo: function () { //TODO: we can present test statistics
    var self = this;
    var ojson = self.olevels;
    console.log(ojson);
    if (ojson.target) {
      self.levelSystem = ojson.target.sys;
      var e = document.querySelector("#levelsys");
      e.innerHTML = self.levelSystem;
      self.targetMajor = ojson.target.major;
      self.targetMinor = ojson.target.minor;
      e = document.querySelector("#levelt");
      e.innerHTML = self.targetMajor + "." + self.targetMinor;
      e = document.querySelector("#levela");
      if (ojson.actual) {
        self.actualMajor = ojson.actual.major;
        self.actualMinor = ojson.actual.minor;
        e.innerHTML = self.actualMajor + "." + self.actualMinor;
      } else {
        e.innerHTML = "";
        // alert("your current actual level is unknown to us, we will auto find out as you do some tests");
      }
      e = document.querySelector("#levelL");
      if (ojson.learning) {
        e.innerHTML = ojson.learning.major + "." + ojson.learning.minor;
      } else {
        e.innerHTML = "";
        // alert("your current actual level is unknown to us, we will auto find out as you do some tests");
      }

      if (ojson.tested) {
        //the test statistics
      }
    } else {
      alert("we do not know your target level");
    }
  },
  getLevelInfo: function () {
    var self = this;
    return new Promise((resolve, reject) => {
      try {
        var request = new XMLHttpRequest();
        var url = webPath + "user?act=level&t=" + new Date().getTime();
        request.open('GET', url, true);
        request.responseType = 'json';
        request.onload = function () {
          var ojson = request.response;
          if (ojson.ireason) {
            reject(url + " returns: " + JSON.stringify(ojson));
          } else {
            console.log(ojson);
            self.olevels = ojson.levels;
            //                        self.tests[0] = ojson; // request.response.tests;
            resolve(true);
          }
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  }
};

function onload() {

  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === 'visible') {
      // backgroundMusic.play();
      player.resumeCtx();

      // if (player.audioCtx.state==.isSuspended) {
      //   player.resumeCtx();
      // }
    } else { //hidden
      // backgroundMusic.pause();
      player.pauseCtx();
      // if (player.isPlaying) {
      //   player.pauseCtx();
      // }
    }
  });

  // var loadData = document.querySelector('#loadData');
  // console.log(loadData);
  // loadData.onclick = function () { //TODO: this should be moved outside?!!!
  //   if (false) {
  //     var path = 'outfoxing.mp3';
  //     console.log(this);
  //     player.testNext = { fnAudio: path };
  //     self.getDataAudio(); //path
  //   } else {
  //     tester.start();
  //   }
  // };

  player.init();
  tester.init();
  user.onUsernameKnown(); //TODO: for temp, should be triggered by UI


}
