package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
//import javax.jdo.annotations.ForeignKey;
//import javax.jdo.annotations.PersistenceCapable;
//import javax.jdo.annotations.Persistent;

//@PersistenceCapable
public class ETestResult implements Serializable {

//    @ForeignKey
//    ETest test;  //this is the one I needed.
    int testid; //when EKP is tested, which test is used to do the test.
//    EKP kp;
//    @Persistent
    /* granularity to be minute. 4085 years from 1970. when save and load use minute.
    in memory, use milliseconds.
     */
    long lts; //==t0+t milliseconds from 1970
//    @Persistent
    boolean good; //to measure the 1/0 test result
    int nPlayed = 1; //to measure the hesitation. now this takes to be the times of record played. default=1. 


    /*    one stimulus(t: a time point(long), good: good at the time point(boolean), hesitate at the time point(time elapsed, or times of audio record played), 
 *                n-th stimulus ,
 *                ETest(all kinds of properties:its id?, words of ETest.info0, time length of the audio, time point of last used),
 *                the EKP(all kinds of properties: its id?, tf: last forecasted forgetting time point,t0: time point of the first stimulus)
 *                ).
     */
    long t; //minutes since t0
    long t0, //the timestamp of the first stimulus, minutes since 1970
            t1, //minutes from t0, where P(t1)=1
            tf; //previously forecasted, minutes since t0
    int n; //0: first stimulus, 1: 2nd 
    int testWords, testAudioSeconds; //properties of the ETest used
    long testlts; //the last timestamp this same ETest is used for the user. minutes since 1970

    static Comparator<ETestResult> cETestID = new Comparator<ETestResult>() { //this is the natural order for this class of objects.
        @Override
        public int compare(ETestResult o1, ETestResult o2) {
            return o1.testid - o2.testid;
        }
    };
    static Comparator<ETestResult> cTime = new Comparator<ETestResult>() {
        @Override
        public int compare(ETestResult o1, ETestResult o2) {
            long dt = o1.lts - o2.lts;
            return dt < 0 ? -1 : dt > 0 ? 1 : 0;
        }
    };

    void json(JsonGenerator g) throws IOException {
        g.writeStartObject();
        g.writeNumberField("testid", testid);
        if (lts < 0) {
            g.writeNumberField("lts", 0);
            System.out.println("ETestResult lts:" + lts);
        } else {
            g.writeNumberField("lts", lts / (1000 * 60));
        }
        g.writeBooleanField("good", good);
        g.writeEndObject();
    }

    void load(JsonParser p) throws IOException {
        JsonToken t = p.currentToken();//.nextToken(); since the caller has to check whether the array is end
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    p.nextToken();
                    if ("testid".equals(name)) {
                        testid = p.getValueAsInt();
                    } else if ("lts".equals(name)) {
                        lts = p.getValueAsLong() * 1000 * 60;
                    } else if ("good".equals(name)) {
                        good = p.getValueAsBoolean();
                    } else {
                        throw new IllegalStateException("unknown field name:" + name);
                    }
                } else {
                    if (t == JsonToken.END_OBJECT) {
                        break;
                    } else {
                        throw new IllegalStateException("expecting end object, but " + t);
                    }
                }
            }
//            this.t = lts - t0;
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof ETestResult) {
                ETestResult tr2 = (ETestResult) o;
                return this.testid == tr2.testid;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return testid;
    }

    /**
     * both are milliseconds since 1970
     *
     * @param t0
     * @param t1
     */
    void setT0T1(long t0, long t1) {
        this.t0 = t0 / 1000 / 60;
        this.t1 = (t1 - t0) / 1000 / 60;
        this.t = (this.lts - t0) / 1000 / 60;
        if (this.t1 == 0) {
            this.t1 = 1;
        }
    }

    static boolean sameTime(long lts1, long lts2) {
        return (lts1 - lts2) / 1000 / 60 == 0;
    }
}
