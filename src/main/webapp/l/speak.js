var o = {
    //1000 is just not enough
    dtMaxPauseAllowed: 1500, //if not allowed, then we discard the old info.
    dtDone: 2000,
    doAutoDiscard: false,
    sLast: '', //the string of input
    ltsTextChanged: 0, //the timestamp of last changed of the text input
    onInputChanged_b: null, //for the next function.
    onInputChanged: function (e, a, b) {
        var self = this;
        // log.textContent = e.target.value;
        /*
                console.log("properties of event:" + e + " " + a + " " + b);
                for (var p in e) {
                    console.log(p + ":" + e[p]);
                }
        properties of event:[object InputEvent] undefined undefined
        speak.js:6 isTrusted:true
        speak.js:6 data:a //what I input
        speak.js:6 isComposing:false
        speak.js:6 inputType:insertText
        speak.js:6 dataTransfer:null
        speak.js:6 getTargetRanges:function getTargetRanges() { [native code] }
        speak.js:6 view:null
        speak.js:6 detail:0
        speak.js:6 sourceCapabilities:null
        speak.js:6 which:0
        speak.js:6 initUIEvent:function initUIEvent() { [native code] }
        speak.js:6 type:input
        speak.js:6 target:[object HTMLInputElement]
        speak.js:6 currentTarget:[object HTMLInputElement]
        speak.js:6 eventPhase:2
        speak.js:6 bubbles:true
        speak.js:6 cancelable:false
        speak.js:6 defaultPrevented:false
        speak.js:6 composed:true
        speak.js:6 timeStamp:2896.085000014864
        speak.js:6 srcElement:[object HTMLInputElement]
        speak.js:6 returnValue:true
        speak.js:6 cancelBubble:false
        speak.js:6 path:[object HTMLInputElement],[object HTMLBodyElement],[object HTMLHtmlElement],[object HTMLDocument],[object Window]
        speak.js:6 NONE:0
        speak.js:6 CAPTURING_PHASE:1
        speak.js:6 AT_TARGET:2
        speak.js:6 BUBBLING_PHASE:3
        speak.js:6 composedPath:function composedPath() { [native code] }
        speak.js:6 initEvent:function initEvent() { [native code] }
        speak.js:6 preventDefault:function preventDefault() { [native code] }
        speak.js:6 stopImmediatePropagation:function stopImmediatePropagation() { [native code] }
        speak.js:6 stopPropagation:function stopPropagation() { [native code] }
        */
        var s = e.target.value; //e.data; is just the changed part //or e.target.value;
        console.log("changed:" + s.toString());
        var ltsnow = new Date().getTime();
        var dt = ltsnow - self.ltsTextChanged;
        self.ltsTextChanged = ltsnow;
        if (dt < self.dtMaxPauseAllowed) {
            self.sLast = s;
        } else { //not allowed, so we should discard the old one
            if (self.doAutoDiscard) {
                var sn = s;
                if (self.sLast) {
                    var n = self.sLast.length;
                    if (sn.length > n) {
                        var t = sn.substring(0, n);
                        console.log("discarded:" + t);
                        self.e_textDiscarded.innerHTML = self.e_textDiscarded.innerHTML + "<br/>" + t;
                        self.e_input.value = self.sLast = sn.substring(n);
                    }
                } else {
                    self.sLast = sn;
                    self.presentSentenceCurrent('');
                }
            }
        }
        setTimeout(() => { //check is done?
            var ltsNow = new Date().getTime();
            if (self.ltsTextChanged + self.dtDone < ltsNow) {
                self.onUtteranceDone(self.e_input.value);
            }
        }, self.dtDone + 1);
    },
    isPunctuation: function (c) {
        return c == '　' || c == ' ' || c == '「' || c == '」' || c == '、'
            || c == '，' || c == ',' || c == '。' || c == '.' || c == '？' || c == '?'
            || c == '！' || c == '!'
            || c == 'ん'; //||c=='。';
    },
    presentSentenceCurrent: function (text) {
        //TODO: this function is the target language dependent!
        // this implementation is for Japanese only.
        //if text is same as the expected text,
        // then return true and presentation is not needed(or can just present "" instead).
        //  otherwise return false.
        //if text is empty, then just present the current expected text.
        //TODO: right now, as long as difference found, I stop.
        //   in fact, I should find out the only difference for example, If I got "ac" while expecting "abc".
        //   in such a case, I should just hightlight "b", rather than highlight "bc".
        
        var self = this;
        var t0 = this.sCurrent.s, html = t0;
        if (text) {
            var i = 0, ilimit = t0.length, j = 0, jlimit = text.length;
            //this.sCurrent.s must have been normalized
            //TODO:  now we normalize text, such as remove unnecessary space etc.
            //TODO: this logic is for temp, not good. the sound equivalence should be compared with sound, rather than text.
            //and very difficult to compare 'おはよございます。' and 'おはようございます。'            
            var ired = -1, withExtra = false;
            // if (ilimit > n) {
            //     ired = ilimit = n;
            // }
            for (; ; i++, j++) {
                if (i < ilimit) {
                    var c0 = t0.charAt(i);
                    if (j < jlimit) {
                        var c = text.charAt(j);
                        if (c0 == c)
                            continue;
                        var c0_p = self.isPunctuation(c0), c_p = self.isPunctuation(c);
                        if (c0_p) {
                            if (c_p)
                                continue;
                            else {
                                //i will get to the next one
                                j--;
                                continue;
                            }
                        } else {
                            if (c_p) {
                                //j will get to the next one
                                i--; continue;
                            } else { //c0 & c are different, and neither of them is punctuation mark.
                                ired = i;
                                break;
                            }
                        }
                    } else { //i<ilimt, j>=jlimit
                        //text is too short
                        if (self.isPunctuation(c0)) {
                            j--; //i get to the next char
                            continue;
                        }
                        ired = i;
                        break;
                    }
                } else { //i>=ilimit
                    if (j < jlimit) {
                        var c = text.charAt(j);
                        if (self.isPunctuation(c)) {
                            i--; //while j will get to the next one
                            continue;
                        }
                        withExtra = true;
                        break;
                    } else {
                        html = ""; //both are done, and no difference found. this one is done.
                        return true;
                    }
                }
            }
            if (ired != -1) {
                html = t0.substring(0, ired) + '<span style="color:red">' + t0.substring(ired) + '</span>';
            } else {
                // var withExtra = i < n; // i + 1 < n;
                if (withExtra) {
                    html = t0 + '<span style="color:red">Extra</span>';
                } else {
                    alert("now this should not happen");
                    return true;
                }
            }
        } else {
            html = t0;
        }
        self.e_text2speak.innerHTML = html;
        return false;
    },
    toggleAutoDiscard: function () {
        var self = this;
        var s;
        if (self.doAutoDiscard = !self.doAutoDiscard) {
            s = "Enable auto discard";
        } else {
            s = "Disable auto discard";
        }
        self.e_autoDiscard.value = s;
    },
    clearAndStart: function () {
        self = this;
        self.e_input.value = '';
        self.e_input.focus();
        self.e_textDiscarded.innerHTML = '';
        self.presentSentenceCurrent('');
    },
    doSentencePrevious: function () {
        var self = this;
        if (self.sentencesDone.length) {
            if (self.sCurrent) {
                self.sentences.unshift(self.sCurrent);
            }
            self.sCurrent = self.sentencesDone.pop();
            self.clearAndStart();
        } else alert("I don't remember which one is the previous one");
    },
    doSentenceNext: function () {
        var self = this;
        if (self.sentences.length) {
            if (self.sCurrent) {
                self.sentencesDone.push(self.sCurrent);
                while (self.sentencesDone.length > 10) {
                    self.sentencesDone.shift();
                }
            }
            self.sCurrent = self.sentences.shift();
            self.clearAndStart();
        } else alert("no more sentence");
    },
    sentences: [],
    sentencesDone: [],
    sCurrent: null, //idxCurrent: -1, //the current sentence
    onUtteranceDone: function (text) {
        var self = this;
        // var sCurrent = sentences[self.idxCurrent];
        if (this.sCurrent) {
            //how to judge whether 2 of them are same?
            // with Japanese, the sentence can use kanji or without kanji.
            //    and some words can use hiragana or haragana.
            if (false) { //this logic might be good for english
                if (this.sCurrent.s == text) {
                    self.doSentenceNext();
                } else { //present the difference
                    self.presentSentenceCurrent(text);
                }
            } else {
                if (self.presentSentenceCurrent(text)) { //same as text
                    self.doSentenceNext();
                }
            }
        } else alert("should not happen, the current sentence is unknown");
    },
    init: function () {
        var self = this;
        self.e_input = document.getElementById('text_input'); //querySelector('input');
        self.e_text2speak = document.getElementById('text2speak');
        // self.colorOriginal=self.e_text2speak.style.color;
        this.onInputChanged_b = this.onInputChanged.bind(this);
        self.e_input.addEventListener('input', this.onInputChanged_b);
        self.e_textDiscarded = document.getElementById('textDiscarded');
    },


};
function onload() {
    o.init();
    // o.sCurrent = { s: 'aaaaa' };
    if (false)
        o.sentences = [{ s: 'aaaaa' }, { s: 'bbbbb' }, { s: 'ccccc' }];
    else if (false)
        o.sentences = [{ s: 'こんにちは。' }, { s: 'こんばんは。' }, { s: 'おはようございます。' }];
    else {
        var d = Downloader.data;
        for (var key in d) {
            o.sentences.push(d[key]);
        }
    }
    o.doSentenceNext();
}


var Downloader = {

    data: {
        '451':
            { s: "先生。" },

        "452":
            { s: "何ですか?" },

        "453":
            { s: "私達、先生に質問があります。" }
        , "454":
            { s: "言ってごらんなさい。" }
        // 言う【いう】→言って tell and let's see
        , "455":
            { s: "どうして勉強するんですか?私達。" }
        // 勉強するん？
        // different person?

        , "456":
            // 00:30:52,725 --> 00:30:54,887
            { s: "この前先生は言いましたよね" }
        // 前【まえ】
        // 言う→言いました
        , "457":
            // 00:30:55,361 --> 00:30:58,763
            { s: "「幾ら勉強して いい大学や　いい会社に入ったって そんなの何の意味もない」って。" }

        , "459":
            // 00:31:01,601 --> 00:31:04,298
            { s: "じゃあ どうして勉強しなきゃいけないんですか?" }
        // 無(な)きゃいけない absolutely necessary
        , "460":
            // 00:31:07,206 --> 00:31:09,402
            { s: "いい加減 目覚めなさい。" }
        // いい加減【いいかげん】
        // 目覚める【めざめる】to wake up

        , "461":
            // 00:31:10,109 --> 00:31:12,077
            { s: "まだそんなことも解らないの?" }
        // 解る=分かる
        , "462":
            // 00:31:13,746 --> 00:31:18,047
            { s: "勉強は しなきゃいけないものじゃありません。したいと思うものです。" }
        // し parallel to the previous one.
        // したい=思う【おもう】want to do

        , "464":
            // 00:31:22,388 --> 00:31:26,757
            { s: "これからあなた達は　知らない物や 理解できない物に たくさん出あいます。" }
        // 理解【りかい】
        // ものに？
        // 出会う【であう】to encounter
        , "466":
            // 00:31:30,563 --> 00:31:34,090
            { s: "美しいなとか楽しいなとか 不思議だなと思う物にも　たくさん出あいます。 " }
        // 不思議【ふしぎ】　不思議だな？
        // にも also, too ?close to the former part, or the latter part?
        , "468":
            // 00:31:39,772 --> 00:31:44,733
            { s: "その時 もっともっと　そのことを知りたい" }
        // もっと and more

        , "469":
            // 00:31:45,345 --> 00:31:48,975
            { s: "勉強したいと自然に思うから人間なんです。" }
        // しぜん
        // から because
        // にんげん: human  じんかん:the world
        // because human will naturally think to wanted to study.
        , "470":
            // 00:31:49,582 --> 00:31:54,884
            { s: "好奇心や探究心のない人間は人間じゃありません。" }
        // こうきしん
        // たんきゅうしん
        // の?
        , "471":
            // 00:31:55,355 --> 00:31:56,345
            { s: "猿以下です。" }
        // 猿【さる】
        // いか
        , "472":
            // 00:31:58,991 --> 00:32:02,723
            { s: "自分達の生きているこの世界のことを知ろうとしなくて 何ができると言うんですか。" }
        // 何【なに】
        , "474":
            // 00:32:05,932 --> 00:32:09,596
            { s: "いくら勉強したって生きている限り 分からないことはいっぱいあります。" }
        // いっぱいfully

        , "476":
            // 00:32:13,139 --> 00:32:14,766
            { s: "世の中には 何でも知ったような顔をした 大人がいっぱい いますが" }
        , "478":
            { s: "あんなものウソっぱちです。" }
        // あんな that kind of
        // 嘘っ八【ウソっぱち】downright lie
        , "479":
            { s: "いい大学に入ろうが いい会社に入ろうが " }
        // 大学【だいがく】
        // 入ろう？
        , "480":
            { s: "幾つになっても勉強しようと思えば 幾らでもできるんです。" }
        // 幾つ【いくつ】how many?how old?
        // how old become, also study,
        // 勉強しようと思えば?
        // 幾らでも【いくらでも】 as mucn/many as you like

        , "481":
            { s: "好奇心を失った瞬間 人間は死んだも同然です。" }
        // 好奇心 こうきしん
        // 失う【うしなう】
        // 瞬間 しゅんかん
        // 人間 にんげん
        // 死ぬ【しぬ】→死んだ
        // 同然 【どうぜん】    
        , "482":
            { s: "勉強は受験の為にするのではありません。" }
        // 受験じゅけん
        // 為に【ために】
        // の？
        , "483":
            { s: "立派な大人になる為にするんです。" }
    },
    getSentenceByID: function (id) {
        return this.data["" + id];
    }
};


