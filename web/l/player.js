//  "use strict";
var webPath = "/Receiver/";
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
  p_reject_resolve: null,  //  this should be called if failed.
  stopRequested: false,//user wanted to stop playing the current test.

  init: function () {
    var self = this;
    self.play = document.querySelector('#pauseCtx'); //class  .play
    // var stop = document.querySelector('.stop');

    self.e_playbackControl = document.querySelector('.playback-rate-control');
    self.e_playbackValue = document.querySelector('.playback-rate-value');
    self.e_playbackControl.setAttribute('disabled', 'disabled');

    self.e_loopstartControl = document.querySelector('#loopstart-control');
    self.e_loopstartValue = document.querySelector('.loopstart-value');
    self.e_loopstartControl.setAttribute('disabled', 'disabled');

    self.e_loopendControl = document.querySelector('#loopend-control');
    self.e_loopendValue = document.querySelector('.loopend-value');
    self.e_loopendControl.setAttribute('disabled', 'disabled');
    self.play.onclick = function () {
      self.onPauseResumeClicked();
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
    //     getData(pathCurrent);
    //   } else { //suspend the context


    //   }
    // };

    self.e_playbackControl.oninput = function () {
      self.source.playbackRate.value = self.e_playbackControl.value; //TODO: this should be put off
      self.e_playbackValue.innerHTML = self.e_playbackControl.value;
    };

    self.e_loopstartControl.oninput = function () {
      // self.source.loopStart = loopStart = self.e_loopstartControl.value;
      self.e_loopstartValue.innerHTML = self.e_loopstartControl.value;
    };

    self.e_loopendControl.oninput = function () {
      // self.source.loopEnd = self.loopEnd = self.e_loopendControl.value;
      self.e_loopendValue.innerHTML = self.e_loopendControl.value;
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
  onPauseResumeClicked: function () {
    var self = this;
    if (self.dataReady) {
      if (self.isStarted) {
        if (self.audioCtx.state === 'running') {
          self.pauseCtx();
        } else if (self.audioCtx.state === 'suspended') {
          self.resumeCtx();
        }
      } else {
        self.tsStartCtx = self.audioCtx.currentTime + 1;
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
          self.source.loop = false;
          self.source.playbackRate.value = self.playSpeed; // self.e_playbackControl.value; //TODO: put this off
          var when = self.tsStartCtx, offset = self.loopStart, duration = self.loopEnd - self.loopStart;
          console.log(when + " " + offset + "+" + duration);
          self.isStopped = false;
          self.source.start(when, offset, duration); //with this, does not do loop
        }
        self.onPlayStarted();
      }
    } else {
      alert("please load data before play");
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
    if (isStopped) { } else {
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
    var length = Math.ceil(self.songLength); //floor
    self.e_playbackControl.removeAttribute('disabled'); //the rate
    self.e_loopstartControl.removeAttribute('disabled');
    self.e_loopstartControl.setAttribute('max', length);
    self.loopStart = 0;
    self.e_loopendControl.removeAttribute('disabled');
    self.e_loopendControl.setAttribute('max', length);
    self.loopEnd = self.songLength;
    document.querySelector('#songLength').innerHTML = "/" + self.songLength.toFixed(2);
    self.nTimes = 0;
    // self.startPlay();
  },
  decodeDataAndPlay: function (audioData) {
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
    console.log("played " + self.nTimes + " times");
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
    self.source = self.audioCtx.createBufferSource();
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
    self.isStarted = true;
    // play.setAttribute('disabled', 'disabled');
    self.e_playbackControl.removeAttribute('disabled'); //the rate
    self.e_loopstartControl.removeAttribute('disabled');
    self.e_loopendControl.removeAttribute('disabled');
    self.e_timeDisplay = document.querySelector('#tsCurrent'); //TODO: use id instead //tag 'p'
    self.play.textContent = 'Suspend context'; //susresBtn
    self.displayTime();
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
      console.log("state is not suspended, but " + state);
    }
  },
};

var tester = {
  /*
  at present, for each test, there is only 2 possible results: good or bad.
  */
  e_good: null,
  e_bad: null,
  tests: [],
  autoStartNext: true, //when a test is done, we just start the next one automatically.
  preloadNext: true, //false, //TODO: do preload.
  // dataPreloaded: null,  //in promise, so not needed.
  dataPreloaded_p: null,
  // pathLast: null, //the one is loading or just loaded. if paralle, then this miht be different from player.pathCurrent.
  testNext: null,
  testCurrent: null, //the test is being played.
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
      //I will have to present some info about this test.
      var e = document.querySelector('#info');
      e.innerHTML = self.testCurrent.fn; //.pathPlaying;// player.pathCurrent;
    };
    self.e_presentTest = document.querySelector('#presentTest');
    self.e_presentTest.onclick = function () {
      // self.presentTest();
      try {
        // player.pauseCtx(); //this is not good. the source should be closed
        player.closeSource().then(tf => {
          //TODO: check the test's key if possible.
          //    and other fields.

          //TODO: send the test result to server. player.nTimes played.
          self.presentTest(); //present the next test

        }).catch(e => {
          alert("close error:" + e);
        });
      } catch (e) {
        alert(e);
      }
    };

  },
  presentTest: function () {
    var e = document.querySelector('#info');
    e.innerHTML = "";//clear any info for previous test
    var self = this;
    var p;
    if (self.preloadNext) {
      p = self.dataPreloaded_p;
      self.dataPreloaded_p = null;
    } else {
      p = self.getTest();
    }
    p.then((dataAudio) => {
      self.testCurrent = self.testNext;//  self.pathPlaying = self.pathLast;
      // self.testNext=null; //this is not needed.
      return player.decodeDataAndPlay(dataAudio);
    }).then(tf => {
      if (self.preloadNext) {
        self.getTest();
      }
    }).catch(ex => {
      e.innerHTML = self.tests.length + ":" + self.testCurrent.fn + ":" + self.testNext.fn + ":" + ex;
      //document.querySelector('#songLength').innerHTML = ex;
      // alert("failed to load data:" + self.pathLast + ":" + e);
    });
  },
  getTest: function () {
    var self = this;
    var p = null;
    var tests = self.tests;
    if (tests.length > 0) { //TODO: move this check outside.
      idx = Math.floor(Math.random() * tests.length);
      self.testNext = tests[idx];
      var path = self.testNext.fn;
      // //remove the current test
      // var idx = self.tests.indexOf(self.pathPlaying);//player.pathCurrent);
      // if (idx != -1) {
      self.tests.splice(idx, 1);
      // }
      p = self.getData(path);
      if (self.tests.length == 0) {
        self.getTests().then(tf => {
          alert("all test has been preloaded, now what to reload?");
        }).catch(e => {
          alert(e);
        });
      }
    } else p = Promise.reject("no more item to test"); //TODO: move to outside.
    return self.dataPreloaded_p = p;
  },
  // use XHR to load an audio track, and
  // decodeAudioData to decode it and stick it in a buffer.
  // Then we put the buffer into the source
  /*
  path is relative, such as 'outfoxing.mp3'. can it be absolute?  
  var path = 'outfoxing.mp3';
  self.getData(path);
  */
  getData: function () { //path
    var self = this;
    // self.pathLast = path;
    return new Promise((resolve, reject) => {
      try {
        // isStarted = false;  //TODO: ensure this is right in player
        //can it be reused? or a new one is must? "AudioBufferSourceNode': cannot call start more than once." so we must create a new one.
        request = new XMLHttpRequest();
        request.open('GET', webPath + "files?fn=" + self.testNext.fn, true); //path
        request.responseType = 'arraybuffer';
        request.onload = function () {
          console.log("data loaded, will decode it:" + typeof (request.response));
          resolve(request.response);
          // if(self.preloadNext){
          //   self.dataPreloaded=request.response;
          // }
          // self.decodeData(request.response);

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
    if (false) { //as test
      //self.tests = ["assets/outfoxing.mp3","assets/257.mp3"]; very old
      // self.tests = [{ fn: "arigato.mp3", key: "" }];  //indeeded can not be decoded
      /* why shortened ?
      when it is alone, it is not shortened. 
      so when it is not alone, there is some where messed up.

      now I know, sometimes the record is shortened, but why?
      */
      self.tests = [{ fn: "25-watashiwanekogasuki.mp3", key: "" }];

      return Promise.resolve(true);
    }
    return new Promise((resolve, reject) => {
      try {
        request = new XMLHttpRequest();
        request.open('GET', webPath + "STest", true);
        request.responseType = 'json';
        request.onload = function () {
          self.tests = request.response.tests;
          resolve(true);
        };
        // request.onFailed;  //TODO: ....
        request.send();
      } catch (e) {
        reject(e);
      }
    });
  },
  start: function () {
    //get a list
    var self = this;
    self.getTests().then(tf => {
      return self.getTest();
    }).catch(e => {
      alert(e);
    });
  },

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

  var loadData = document.querySelector('#loadData');
  console.log(loadData);
  loadData.onclick = function () { //TODO: this should be moved outside?!!!
    if (false) {
      var path = 'outfoxing.mp3';
      console.log(this);
      self.getData(path);
    } else {
      tester.start();
    }
  };

  player.init();
  tester.init();
  tester.start();

}
