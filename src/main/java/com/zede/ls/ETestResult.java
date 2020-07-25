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

/**
 *
 */
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
    long lts;
//    @Persistent
    boolean good;

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
        g.writeNumberField("lts", lts / (1000 * 60));
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
                        lts = p.getValueAsInt() * 1000 * 60;
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
}
