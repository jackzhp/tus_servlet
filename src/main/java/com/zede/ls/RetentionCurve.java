package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import static com.zede.ls.ETest.getFileByID;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.Set;

/**
 * I should use AI, for example learn from
 * http://neuralnetworksanddeeplearning.com/
 *
 * The goal of this class is for a given EKP and a series of ETestResult, to
 * calculate the time point at which this EKP should be reviewed.
 *
 * the memory retention curve should be a function of #1 time since the last
 * intensification, #2 the number of intensification, and it should be
 * monotonically decreasing over time. and should be monotonically increasing
 * over the number of intensifications.
 *
 *
 * the evaluated object is the memory retention of EKP, the test
 *
 * dependent variables: ETestResult.good, ETestResult.n
 *
 * independent variables: dt[i]=t[i]-t[i-1], dT[i]=t[i]-t[0], the test. t[i] is
 * the time of i-th intensification it is hard for the test to get into the
 * equation, so instead, some of its properties are used.
 *
 * meta parameters: the complexity of ETest: words of ETest.info, length of the
 * audio record(seconds) other meta parameters.
 *
 * Given a test, its EKP's are related! (Be noted that this is important). but
 * right now, I treat them as independent.
 *
 *
 * I need 2 output: #1 forecast the timestamp the use would forget the EKP. #2
 * given a specific, determine whether the user has forgot the EKP. #2 can be
 * calculated from #1 easily.
 *
 * when neural network is used:
 *
 * the user, the EkP(what properties should I use?), last forecasted forgetting
 * time point,
 *
 * with a neural network for each (user,EKP), the historical data is not needed
 * any more.
 *
 * how would I treat EKP? if I use a neural network for each (user,EKP) then
 * each EKP are independent from each other. I had better use one neural network
 * for each user. then all EKP's are interdependent.
 *
 * phase #1. I would let the neural network to be independent from the user and
 * EKP.
 *
 * the input to the network: the user(determine the network),
 *
 *
 *
 * the output from the network: #1. the next forcasted forgetting time point,
 * #2. relevant EKPs(desired but not needed). and for the #1, I need 3(or 8) of
 * them before the output layer: one for short term, one for intermediate term,
 * one for long term. of course, at the output layer, output only 1 of the 3.
 *
 *
 *
 *
 * is oputna helpful in determine the model?
 *
 *
 * is allen NLP helpful in recognizing the sentence?
 *
 */
public class RetentionCurve {
    //https://www.ane.pl/pdf/5535.pdf

    //different user can have different curve.
    //but for now, I just use the same one.
    static RetentionCurve one = new RetentionCurve();
    static long tfMax = 60 * 24 * 365 * 2; //in minutes, so 2 years
//    public static ForgettingCurve getFor(EUser user) {
//        return one;
//    }

    void init() {
        segments = new Segment[9];
        long[] as = {0, 60 * 4, 60 * 24 * 2, 60 * 24 * 7, 60 * 24 * 30, 60 * 24 * 90};
        /**
         * TODO: when do init, I will ensure the slop is getting flatter. i.e.
         * ensure monotonicity.
         *
         * headache, so give up.
         */
        Random r = new Random();
        for (int i = 1; i < as.length; i++) {
            int k = (i - 1) << 1; //1: 0, 2: 2, 3: 4, 4: 6, 5:8
            long s0 = 0, s1 = 0;
            for (int j = 0; j < 2; j++) {
                if (j == 0) {
                    s0 = as[i - 1];
                    s1 = as[i];
                } else {
                    if (i + 1 < as.length) {
                    } else {
                        continue;
                    }
                    s0 = (s0 + s1) / 2;
                    s1 = (as[i] + as[i + 1]) / 2;
                }
                double d = r.nextDouble(); //between 0 & 1.
                while (d >= Ptarget) {
                    d /= 2;
                }
                double b = r.nextDouble() * (s1 - s0);
                if (r.nextDouble() > 0.5) {
                    b = 0 - b;
                }
                int idx = k + j;
                Segment s = new Segment(idx, s0, s1, b, d);
                segments[idx] = s;
            }
        }
        save(20);
    }

    void json(JsonGenerator g) throws IOException {
        g.writeStartObject();
        g.writeNumberField("Ptarget", Ptarget);
        g.writeNumberField("Pdelta", Pdelta);
        g.writeArrayFieldStart("segments");
        for (Segment s : segments) {
            s.json(g);
        }
        g.writeEndArray();
        g.writeEndObject();
    }

    void parse(JsonParser p) throws IOException {
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
            if (t == JsonToken.FIELD_NAME) {
            } else {
                throw new IllegalStateException("expecting field name, but " + t);
            }
            t = p.nextToken();
            Ptarget = p.getValueAsDouble();
            t = p.nextToken();
            t = p.nextToken();
//            Pdelta = p.getValueAsDouble();  //this is server's private stuff, user does not have to know this.
            t = p.nextToken();
            t = p.nextToken();
            ArrayList<Segment> al = new ArrayList<>();
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.START_OBJECT) {
                    al.add(parseSegment(p));
                    continue;
                }
                if (t == JsonToken.END_ARRAY) {
                    break;
                }
            }
            segments = al.toArray(new Segment[0]);
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    /* TODO: use neural network to do scheduling
    EKP might also be relevant too.
     */
//    public long scheduleWith(Set<ETestResult> tested) {
    public long scheduleWith(ArrayList<ETestResult> tested) {
        Collections.sort(tested, ETestResult.cTime);  //TODO: I should utilize the good & bad info.
        int n = tested.size();
        long dt;
        if (n <= 1) {
            dt = 1000 * 60 * 5; //5 minutes
        } else if (n <= 2) {
            dt = 1000 * 60 * 60 * 2;//2 hours
        } else if (n <= 3) {
            dt = 1000 * 60 * 60 * 24 * 1; //1 days
        } else if (n <= 4) {
            dt = 1000 * 60 * 60 * 24 * 2; //2 days
        } else if (n <= 5) {
            dt = 1000 * 60 * 60 * 24 * 7; //7 days
        } else if (n <= 6) {
            dt = 1000 * 60 * 60 * 24 * 10; //10 days
        } else {
            dt = (n - 6) * 1000 * 60 * 60 * 24 * 30; //30 days
        }
        long ltsBase;
        if (tested.isEmpty()) {
            ltsBase = System.currentTimeMillis();
        } else {
            ltsBase = tested.get(n - 1).lts;
        }
        return ltsBase + dt;
    }
    Segment[] segments;

    /**
     * each Segment is a cell. we use tr.t1 & [s0,s1] to find out which Segment
     * should be simulated.
     *
     * @param tr
     */
    void stimulate(ETestResult tr) {
//        double tf = 0;
//        boolean overSegment = true; // from tr.t1 to t, there is no segment can process it.
//        boolean[] stimulated = new boolean[segments.length];
        for (int i = 0; i < segments.length; i++) {
//            if (stimulated[i]) {
//                continue;
//            }
            Segment s = segments[i];
            if (s.s0 <= tr.t1 && tr.t1 <= s.s1) { //at least one matches.
                if (s.stimulate(tr)) {
                    for (int j = i + 1; j < segments.length; j++) {
                        Segment s2 = segments[j];
                        if (tr.t < s2.s0) {
                            break;
                        }
                        if (tr.t <= s2.s1) {
//                            stimulated[j] = true; //TODO: do I need this?
                            s2.transmitFrom(s, tr); //TODO: how do I impose the later segments should be more flatter than former segments.
                        }
                    }
                }
//                overSegment = false;
            }
        }
    }
    Selector selector;

    /**
     *
     * @param t minutes relative to t0 of EKP.
     * @return minutes relative to tr.t0. not 1970
     */
    long forecast(double t) {
        if (selector == null) {
            selector = new Selector();
        }
        long ltf = (long) selector.getForecast(t);
//        System.out.println(tr.tf + ":" + tr.t + "  " + tr.good + " " + tf);
        return ltf; // (tf + tr.t0);
    }

    long saveRequested, saveLast, saveNext; //the next means the next saving must be after that.
    boolean nextScheduled;
    AtomicBoolean saving = new AtomicBoolean();
    private static long saveDelayDefault = 30; //seconds

    void save(long delay) { //this method is same for all 
        long ltsnow = System.currentTimeMillis();
        saveRequested = ltsnow;
        if (nextScheduled) {
        } else {
            long delay2 = saveNext - ltsnow;
            delay2 /= 1000; //now it is seconds
            delay2++;
            if (delay < delay2) {
                delay = delay2;
            }
            App.getExecutor().schedule(() -> {
                try {
                    nextScheduled = false;
                    save();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, delay, TimeUnit.SECONDS);
            nextScheduled = true;
        }
    }

    private void save() throws IOException {
        if (saving.compareAndSet(false, true)) {
            try {
                if (saveRequested >= saveLast) {
                    File fnew = getFile(true);
                    JsonGenerator g = App.getJSONgenerator(fnew);
                    saveLast = System.currentTimeMillis();
                    saveNext = saveLast + saveDelayDefault;
                    json(g);
                    g.flush();
                    g.close();
                    File f = getFile(false);
                    if (f.exists()) {
                        f.delete();
                    }
                    fnew.renameTo(f);
                }
            } finally {
                saving.set(false);
            }
        }
    }

    File getFile(boolean extnew) {
        String fn = "rc.json";
        if (extnew) {
            fn += ".new";
        }
        return new File(App.dirData, fn);
    }
    AtomicBoolean loading = new AtomicBoolean();
    boolean loaded;

    void load() {
        if (loading.compareAndSet(false, true)) {
            try {
                if (loaded) {
                    return;
                }
                File f = getFile(false);
                if (f.exists()) {
                    JsonParser p = App.getJSONparser(f);
                    parse(p);
                    loaded = true;
                    p.close();
                } else {
                    init();
                }
                Arrays.sort(segments, new Comparator<Segment>() {
                    @Override
                    public int compare(Segment o1, Segment o2) {
                        return (int) (o1.s0 - o2.s0);
                    }
                });
                for (int i = 0; i < segments.length; i++) {
                    Segment s = segments[i];
                    s.idx = i;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                loading.set(false);
            }
        }
    }

//    private double Ptarget = 0.8, Pdelta = 0.1;
    private double Ptarget = 0.9, Pdelta = 0.05; //0.9 +0.1 should not be used.

    class Segment {

        /* retention probability = (c*e)^(b-a*t)+d  while e is the Euler's number, the base of the natural logrithm function.
        this might be too complicated, so take c=1.
        
        P(t)=e^(b-a*t)+d
        for the first Segment(0 <= t < s1), P(t1)=1, this gives P(t)=e^(-a*t)
        for each Segment(s0 <= t < s1), 1>P(s0) > P(s1). Let's fix d with this constraint and take 1==P(t1)
        while s0 <= t1 < s1
        so d=1-e^(b-a*t1).
        No. it is better to use that to detemine a=(b-ln(1-d))/t1.
        
        P(tf)=alpha we take alpha=0.9.
        tf=(b-ln(alpha-d))/a   = ts* (b-ln(alpha-d))/(b-ln(1-d))
        so tf is a function of b and d, while s and alpha are fixed.
        
        the derivative of P(t): P'(t)=-a*e^(b-a*t) + d
        
         */
//        final 
        int idx;
        final long s0, s1; //the time interval since the first stimulus.
        double //a, 
                b, d;  //d < alpha
        long dtE; //the expected time interval from. TODO: when not using exponential function, this should not be used.
        /**
         * do not utilize this, I have not think this through.
         *
         */
        int nShouldExtend; //increase this by 1 when we have a good with time after the forecasted time point, and the good time is after s1
        Segment sFormer;
//        Segment(double s0, double s1) {
//            if (s0 != 0 || s1 >= s0) {
//                throw new IllegalArgumentException();
//            }
//            this.s0 = s0;
//            this.s1 = s1;
//        }

        Segment(int idx, long s0, long s1, //double a, 
                double b, double d) {
            if (s0 < 0 || s0 >= s1 || d >= Ptarget) {  // || a == 0
                throw new IllegalArgumentException(s0 + ":" + s1 + " " + d + " target:" + Ptarget);
            }
            this.idx = idx;
            this.s0 = s0 == 0 ? 1 : s0;
            this.s1 = s1;
//            this.a = a;
            this.b = b;
            this.d = d;
        }

        /**
         *
         * @param s
         * @return true if the forecast is good, and is over the segment
         */
        boolean stimulate(ETestResult s) {
            if (s0 <= s.t1 && s.t1 <= s1) {
            } else { //if (s.t < s0 || s.t >= s1)  //should be s.t1 as s.t chould be over s1.
                throw new IllegalStateException();
            }
            double dt = tf(Ptarget, s.t1);
            double tf = s.t1 + dt;
            boolean shouldAdjust = false,
                    toLarger = false, shouldDeliver = false;
            if (s.good) {
                if (tf > s.t) {
                    //tested too early
                    //we do not have to adjust nothing
                } else { // s.tf < s.t
                    /* s.tf <=s.t, we should have forecasted a larger tf.
                    so we do something so as if we redo the forecast, we would have produced a larger tf.
                     */
//                    if (s.t > s1) { //this will never happen.
//                        nShouldExtend++;
//                    }
                    shouldDeliver = s.t > s1; //true; //deliver to segments that can be located with s.t
                    shouldAdjust = true;
                    toLarger = true;
                }
            } else { //bad
                if (tf >= s.t) {
                    /*
                    the lost time point must be less than t, and tf is even larger than t, so we should have forecasted a smaller tf.
                     */
                    shouldAdjust = true;
                    toLarger = false;
                } else { //s.tf < s.t, we do not have to adjust it, since we do not know whether our forecast is good.
                }
            }
            if (shouldAdjust) {
                if (toLarger) {
                    adjustTowardLarger(s);
                } else {
                    adjustTowardSmaller(s);
                }
                save(20);
            }
            return shouldDeliver;//(long) tf(Ptarget, s.t); //let's separate stimulation and forecast
        }

        /**
         * each segment like a neural cell.
         *
         * @param s
         */
        void transmitFrom(Segment s, ETestResult tr) {
            if (s0 <= tr.t && tr.t < s1 && tr.good) {
            } else { // if (tr.t < s0 || tr.t >= s1 || tr.good == false)
                throw new IllegalStateException();
            }
            //TODO: this.dtE  should >  s.dtE
            double dtO = tf(Ptarget, tr.t); //=== tr.t + this.dtE
            double tfO = tr.t + dtO;
            double dtT = s.dtE;
            if (dtO < dtT) {
                double tfTarget = tr.t + dtT;
                if (tfTarget < tfMax) {
                    double tfLearned = tfTarget; //if learning rate is 100%. or we can deem Pdelta as the learning rate.
                    tfLearned = tfO + (tfTarget - tfO) / 2; //50%
                    findParameters(1, tfLearned, tr.t);
                    if (true) { //test
                        double dt = tf(Ptarget, tr.t);
                        System.out.println("\ttransmitted in, old:" + dtO + " targeted: " + dtT + " achieved:" + dt);
                    }
                }
            }
        }

        /**
         * P(t)=a*t^2-b*t+c
         *
         * P(t1)=1, 1 root(i.e. b^2=4*a*c)
         *
         * do not use this constraint: P(tMax)=0, tMax=b/2/a
         *
         * P(t)=a^2*t^2-b*t + c^2 with a>0, b>0. c's sign does not matter.
         * P(t1)=1, 1 root(i.e. b^2=4*a^2*c^2, i.e. b=2*a*c or b=-2*a*c. which
         * one?) P(t)=(a*t)^2-2*c*(a*t)+c^2=(a*t-c)^2
         *
         * P'(t)=2*a^2*t-b = 2*a^2*t - 2*a*c=2*a(a*t - c) should always be less
         * than 0, i.e. t less than c/a so c >0.
         *
         * P(t1)=1 gives (a*t1-c)^2=1, i.e. a*t1-c=1 or a*t1-c=-1. we can only
         * choose the latter. so c=a*t1 +1, so b=2*a*c=2*a^2*t1+2*a.
         * P'(t)=2*a^2*t-b= 2*a^2*t - 2*a^2*t1-2*a =2*a^2*(t -t1)-2*a
         *
         * in order to become flatter, 4*(t-t1)*a -2 &gt; 0, i.e. a &gt;
         * 1/(t-t1)/2. in order to become steeper, 4*(t-t1)*a -2 &lt; 0, i.e. a
         * &lt; 1/(t-t1)/2.
         *
         * s0 is dynamically determined, s0 is c/a.
         *
         *
         * P(t)=a*t^2-b*t + c with a>0, b>0. c's sign does not matter. P(t1)=1
         * P(t)=a*(t^2-t1^2)-b(t-t1)+1 P(t) is positive for the range[t1,s1],
         * b(t-t1) &lt; a*(t^2-t1^2) + 1. i.e. b &gt; (a*(t^2-t1^2) + 1)/(t-t1)
         * = a*(t+t1) + 1/(t-t1)
         *
         * P'(t)=2*a*t-b should be negative for the range[s0,s1], b &lt; 2*a*s1
         *
         * a is the this.d
         *
         * @param t
         * @param t1
         * @return
         */
        double P(double t1, double t) {
            if (t < t1) {
                throw new IllegalStateException(t + " : " + t1);
            }
            if (true) {
                /* P(t)=b-a*t
                P(t1)=1, i.e. b-a*t1=1
                P(t1;t)=1-a(t-t1)
                t=t1+(1-P)/a
                 */
                return 1 - d * (t - t1);

            }
            return d * (t * t - t1 * t1) - b * (t - t1) + 1;
        }

        /* originally, it is 
        P(t) = e^(b-a*t)+d which is too complicated, 
        
        so I changed it to
//        P(t) = e^(b-a*(t-t1))+d
//        
//        still too complicated, so I make d=0
        
        P(t) = e^(b-a*t)
        with P(t1)=1 so a*t1 = b
        P'(t) = -a*e^(b-a*t)
        
        if I use exponential function, with same parameter, changing only t1,
        then with t1 increase, the curve becomes steeper. this is not what we want, we hope it can become faltter.
        for any function, if just shift to the right, they will become steeper.
        
         */
        double slope(double t, double t1) {
            if (true) {
                /* P(t)=b-a*t
                P(t1)=1, i.e. b-a*t1=1
                P(t1;t)=1-a(t-t1)
                t=t1+(1-P)/a
                 */
                return -d;
            }
//            double a = (b - Math.log(1 - d)) / t1;
//            double slope = 0 - a * Math.exp(b - a * t);
//            return slope;
            return 2 * d * t - b;
        }

        /**
         *
         * @param P
         * @param t1 P(t1)=1 with t1>=1
         * @return dt where t1+dt = tf in P(tf)=P
         */
        double tf(double P, double t1) {
            if (s0 <= t1 && t1 <= s1) {
            } else {
                throw new IllegalArgumentException();
            }
            if (P > 1) {
                P = 1;
            }
            if (true) {
                /* P(t)=b-a*t
                P(t1)=1, i.e. b-a*t1=1
                P(t1;t)=1-a(t-t1)
                t=t1+(1-P)/a
                 */
                double dt = (1 - P) / d;
                if (Double.isInfinite(dt)) {
                    dt = tfMax - t1;
                }
                return dt;
            }
            if (true) {
                //P(t)=a*(t^2-t1^2)-b(t-t1)+1
                double delta = b * b - 4 * d * (1 - P + (b - d * t1) * t1);
                /**
                 * it is negative!!!
                 *
                 * delta=b^2 -4*a(1-P)-4*a*b*t1 + 4*a^2*t1^2 ----(30) its first
                 * derivative over t1: -4*a*b +8*a^2*t1 its 2nd derivative over
                 * t1: 8*a^2 &gt; 0, so there exists a minimum at
                 * t1=4*a*b/8/a^2=b/(2*a) s2=(4*a*b)^2 - 4*(4*a^2)*(b^2
                 * -4*a(1-P)) = 4^3*a^3*(1-P) &gt; 0. when t1= (4*a*b-s)/8*a^2
                 * or t1=(4*a*b+s)/8*a^2, delta=0 for delta>=0, we need t1 &le;
                 * (4*a*b-s)/8*a^2=b/(2*a) - 8*a(1-P) ----(35)
                 *
                 * the maximum t1 is s1, so we require s1 &le; b/(2*a) -
                 * 8*a(1-P) ----(36) i.e. b &ge; 2*s1*a +16*(1-P)*a^2 ----(37)
                 * i.e. a &le; 0 or a &ge; s1/8/(1-P) ----(38) anything wrong?
                 *
                 *
                 */
                double delta2 = Math.sqrt(delta);
                double tf = (b - delta2) / (d * 2);
                return tf - t1;
            }
////            if (t1 == 0) {
////                t1 = 1;
////            }
//            double dt = dtE;
//            if (dt == 0) {
//                double tmp = s0 * (b - Math.log(P - d)) / (b - Math.log(1 - d)); //s0 is right!!
//                dt = tmp - s0; //s0 is right!!
//                dtE = (long) dt;
//                dt = dtE;
//            }
////            double dt = t1 - s0;
//            return //t1 + 
//                    dt; //dt + tmp;

//P(t)=a*(t^2-t1^2)-b(t-t1)+1
            double delta = b * 2 - 4 * d * (1 - P + (b - d * t1) * t1);
            if (delta < 0) {
                throw new IllegalStateException("delta:" + delta);
            }
            double delta2 = Math.sqrt(delta);
            double tf = (b - delta2) / d / 2;
            if (true) { //test verification
                System.out.println("is 0:" + (P(t1, tf) - P));
            }
            return tf - t1;
        }

        /*
        tfLearned/s0 = (b - Math.log(P - d)) / (b - Math.log(1 - d))
            we are looking for the (b,d) which is the closed to its current value.
        
        I do not know how to solve this problem.
        but I take 10 points in [d-0.1, d+0.1], calculate their b.
        then I choose the one that is closest to the old (b,d). 
        this choose criteria is not right!!!!
        which point to choose? I should choose the one whose slope is closest to that of the old with the old (b,d).
        
         */
        /**
         * to find b,d such that P(tf)=Ptarget while P(t1)=1
         *
         *
         * @param flatter 1: flatter, -1: steeper, 0: init
         * @param tf
         * @param t1 at least 1.
         */
        void findParameters(int flatter, double tf, double t1) {
            if (t1 < 1) {
                throw new IllegalArgumentException("t1:" + t1);
            }
            double step = 0.05;
            if (true) {
                /* P(t)=b-a*t
                P(t1)=1, i.e. b-a*t1=1
                P(t1;t)=1-a(t-t1)
                
                its 1st derivative over t: -a
                
                
                t=t1+(1-P)/a
                 */
                if (flatter > 0) { //since slope is negative, so I should increase slope, i.e. decrease a.
                    d *= (1 - step);
                } else if (flatter < 0) {
                    d *= (1 + step);
                } else {
                    d = (1 - Ptarget) / (tf - t1);
                }
                return;
            }
            if (true) {
                /**
                 * P(t)=a*t^2-b*t+c ----(1)
                 *
                 * P'(t)=2*a*t-b ----(2)
                 *
                 * P"(t)=2*a ----(3)
                 *
                 * P"(t) &gt; 0 ----(4) this is desirable. over time, its
                 * meaning is that .....
                 *
                 * (4) gives a &gt; 0 ----(4-2)
                 *
                 * somewhere wrote a &lt; b ----(?) why do I have this?
                 *
                 * s0 &lt; t1 &lt; s1 ----- this is obvious; t1 &lt; tf ---this
                 * is also obvious; but tf can be larger than s1 or smaller than
                 * s1.
                 *
                 * P(t1)=1 ----(9)
                 *
                 * From (1) & (9), we have P(t;t1)=a*(t^2-t1^2)-b(t-t1)+1
                 * ----(10) and its derivative over t: P'(t;t1)=2*a*t-b still
                 * same as (2) over t1: b-2*a*t1
                 *
                 * 2nd derivative over t: P"(t;t1)=2*a this is (3), over t1:
                 * -2*a &lt; 0. but be noted the slope over t does not change
                 * with t1.
                 *
                 *
                 *
                 * Ptarget=P(tf;t1) ----(11) has at least 1 root, i.e.
                 * b^2-4*a*(1-Ptarget+b*t1-a*t1^2) &ge; 0 ----(12) , where 0
                 * &lt; Ptarget &lt; 1.
                 *
                 * this is an ellipse of (a,b) and (a,b) should not be inside
                 * the ellipse.
                 *
                 * (11) gives b=a*(tf+t1) + (1-Ptarget)/(tf-t1) ---(13)
                 *
                 * combine (11) & (12), we have 0 &le;
                 * b^2-4*a*(1-Ptarget+b*t1-a*t1^2) = b^2-4*a*t1*b + 4*a^2*t1^2
                 * -4*a*(1-Ptarget) = (b-2*a*t1)^2 - 4*a*(1-Ptarget) =
                 * a^2*(tf-t1)^2 - 4*a*(1-Ptarget) = a*(a*(tf-t1)^2 -
                 * 4*(1-Ptarget)) I had made the next conclusion wrong, and
                 * reversed, and then reversed it again. so 0 &le; a &le;
                 * 4*(1-Ptarget)/(tf-t1)^2 ----(22)
                 *
                 *
                 *
                 * (10) is positive for t in [t1,tf] and t1 in [s0,s1], i.e.
                 * b(t-t1) &lt; a*(t^2-t1^2) + 1. i.e. b &lt; (a*(t^2-t1^2) +
                 * 1)/(t-t1) = a*(t+t1) + 1/(t-t1) ----(14).
                 *
                 * (14) does not contain (12). That (10) is positive for t in
                 * [t1,tf] and t1 in [s0,s1] does not entail (10)=Ptarget has at
                 * least 1 root. since (10) can just be greater than Ptarget.
                 *
                 * I need a minimum for (14)' RHS. how?
                 *
                 * the first derivative of (14)'s RHS over t: a - 1/(t-t1)^2,
                 * over t1: a + 1/(t-t1)^2 &gt; 0
                 *
                 * the 2nd derivative over t: 2/(t-t1)^3 &gt; 0, over t1:
                 * 2/(t-t1)^3 &gt; 0
                 *
                 * so t1 should take its minimum s0.
                 *
                 *
                 * (2) should be negative for the range[t1,tf], i.e. b &gt;
                 * 2*a*t ----(15). the maximum of its RHS is 2*a*tf;
                 *
                 * (13) & (15) gives a cross point
                 *
                 * 2*a*tf=a*(tf+t1) + (1-Ptarget)/(tf-t1) ---(25) i.e.
                 * a*(tf-t1)=(1-Ptarget)/(tf-t1), i.e. a =(1-Ptarget)/(tf-t1)^2
                 * ----(26) (13) & (15) entails a &lt; (1-Ptarget)/(tf-t1)^2
                 * ----(27)
                 *
                 * (22) & (27) are consistent.
                 *
                 */
                //why the following equation?
                // a cross point of 2 lines! which two?
                //Ptarget = a*s1^2 - a*t1^2 - 2*a*s1^2 + 2*a*s1*t1 + 1
                //        = -a*( s1-t1)^2 + 1
                // a < (1 - Ptarget)/(s1-t1)^2
                //  b > 2*a*s1 i.e.  a*(s1+t1) + (1-Ptarget)/(s1-t1) > 2*a*s1
                //                               (1-Ptarget)/(s1-t1) > a*(2*s1-s1-t1) = a*(s1-t1)
                //                               (1-Ptarget)/(s1-t1)^2 <  a
                double dtft1 = tf - t1;
                dtft1 = dtft1 * dtft1;
                double dMax = (1 - Ptarget) / dtft1;
                double dT2 = 0, bT2 = 0;
                if (flatter > 0) { //P'(tf)=a*(tf-t1) - (1-Ptarget)/(tf-t1) should increase(it is negative) on change of (a,b)
                    dT2 = d;
                    while (true) {
                        dT2 -= dMax * step;
                        if (dT2 <= 0) {
                            dT2 = step;
                        }
//                        if (dT2 > dMax) {
////                        throw new IllegalStateException("can not add it any more");
////                            dT2 += 10;
//                        } else {
//                            dT2 = (dT2 + dMax) / 2;
//                        }
                        bT2 = dT2 * (tf + t1) + (1 - Ptarget) / (tf - t1);
                        if (2 * dT2 * tf - bT2 < 0) { //P(tf;t1)
                            break;
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                } else if (flatter < 0) { //steep. since it is negative, so I should decrease a(d)
                    dT2 = d;
                    while (true) {
                        dT2 -= dMax * step;
                        if (dT2 > dMax) {
                            dT2 = dMax;
                        }
//                        if (dT2 < dMax) {
//                            dT2-=10; //throw new IllegalStateException("can not decrease it any more");
//                        } else {
////                            dT2 = (dT2 + dMax) / 2;
//                        }
                        bT2 = dT2 * (tf + t1) + (1 - Ptarget) / (tf - t1);
                        if (2 * dT2 * tf - bT2 < 0) { //P(tf;t1)
                            break;
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                } else { //flatter ==0, i.e. init
                    dT2 = dMax / 2;
                    while (true) {
                        bT2 = dT2 * (tf + t1) + (1 - Ptarget) / (tf - t1);
                        if (2 * dT2 * tf - bT2 < 0) { //P(tf;t1)
                            break;
                        } else {
                            throw new IllegalStateException();
                        }
//                        dT2 *= 2;
                    }
                }
                d = dT2;
                b = bT2;
                System.out.println("found parameters for P(" + t1 + "," + (tf - t1) + ")=" + Ptarget);
                System.out.println("\tP(" + t1 + "," + tf + ")=" + Ptarget + "  verify P:" + (P(t1, tf) - Ptarget) + " verify tf:" + (t1 + tf(Ptarget, t1) - tf));
                try {
                    double fT = s1 + tf(Ptarget, s1);
                    if (Double.isNaN(fT)) {
                        tf(Ptarget, s1);
                    }
                    System.out.println("farthest forecast:" + fT);
                    fT = s0 + tf(Ptarget, s0);
                    System.out.println(" closest forecast:" + fT);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return;
            }
            double b = this.b, d = this.d;
            double a = (b - Math.log(1 - d)) / t1;
            double Pt1 = Math.exp(b - a * t1) + d;
            double P = Math.exp(b - a * tf) + d;
            double ratio = tf / t1;
            double dN = 0, bN = 0, slopeC = flatter > 0 ? 0 : Double.NEGATIVE_INFINITY;
            int points2 = 5;
            step = 0.1 / points2;
            double slopeO = slope(tf, t1);
            System.out.println("1? " + (Pt1 - 1) + " slope:" + slopeO + " P " + P + " -> " + Ptarget); //must be.
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < points2; i++) {
                    double dt = d + step * i;
                    double bt = (ratio * Math.log(1 - dt) - Math.log(Ptarget - dt)) / (ratio - 1);
                    a = (bt - Math.log(1 - dt)) / t1;
                    double slope = 0 - a * Math.exp(bt - a * tf);
                    System.out.println(slope + " close?: " + (slope - slopeO) + " P:" + (Math.exp(bt - a * tf) + dt));
                    if (flatter > 0) {
                        if (slope > slopeO) { //become flatter since both are negative
                            //I pick the one closest to slopeO, so looking for the minimum
                            if (slope < slopeC) {
                                dN = dt;
                                bN = bt;
                                slopeC = slope;
                            }
                        }
                    } else {
                        if (slope < slopeO) { //become steep since both are negative.
                            //I pick the one closest to slopeO, so looking for the maximum
                            if (slope > slopeC) {
                                dN = dt;
                                bN = bt;
                                slopeC = slope;
                            }
                        }
                    }
                }
                step *= -1;
            }
            dtE = 0;
            this.d = dN;
            this.b = bN;
        }

        /*
        when we do adjustment, how much to adjust. we target at a specific probability change.
         */
        void adjustTowardLarger(ETestResult s) {
            double dtTarget = tf(Ptarget - Pdelta, s.t1);
            double tfTarget = s.t1 + dtTarget;
//            if (s.t1 >= s0) {
//                if (tfTarget < s.tf) {
//                    //what to do????
//                    throw new IllegalStateException(tfTarget + " < " + s.tf);
//                }
//            }
            if (tfTarget > tfMax) {
                tfTarget = tfMax;
                dtTarget = tfTarget - s.t1;
                adjustTowardSmaller(s);
                return;
            }
            double tf0 = s.t1 + tf(Ptarget, s.t1);
            double tfLearned = tfTarget; //if learning rate is 100%. or we can deem Pdelta as the learning rate.
            while (true) {
                tfLearned = tf0 + (tfTarget - tf0) / 2; //50%.  "learing" as in "machine learning"
                findParameters(1, tfLearned, s.t1);
                if (true) { //test
                    double dt = tf(Ptarget, s.t1);
                    if (Double.isInfinite(dt)) {
                        tf(Ptarget, s.t1);
                    }
                    System.out.println("\ttargeted: " + dtTarget + " achieved:" + dt);
                }
                break;
            }
        }

        void adjustTowardSmaller(ETestResult s) {
            double dtTarget = tf(Ptarget + Pdelta, s.t1);
            double tfTarget = s.t1 + dtTarget;
            if (tfTarget > tfMax) {
                tfTarget = tfMax;
                dtTarget = tfTarget - s.t1;
            }
//            if (s.t1 >= s0) {
//                if (tfTarget > s.tf) {
//                    throw new IllegalStateException(tfTarget + " < " + s.tf);
//                }
//            }
            double dt0 = tf(Ptarget, s.t1);
            double tf0 = s.t1 + dt0;
            double tfLearned = tfTarget; //if learning rate is 100%. or we can deem Pdelta as the learning rate.
            while (true) {
                tfLearned = tf0 + (dtTarget - dt0) / 2; // s.tf - (s.tf - tfTarget) / 2; //tfTarget + (s.tf - tfTarget) / 2; //50%
                findParameters(0, tfLearned, s.t1);
                double dt = tf(Ptarget, s.t1);
                if (Double.isInfinite(dt)) {
                    tf(Ptarget, s.t1);
                }
                if (true) { //test
                    System.out.println("\ttargeted: " + dtTarget + " achieved:" + dt);
                }
                double tf = s.t1 + dt;
                if (tf < tfMax) {
                    break;
                }
                tf0 = tf;
            }
        }

        private void json(JsonGenerator g) throws IOException {
            g.writeStartObject();
            g.writeNumberField("s0", s0);
            g.writeNumberField("s1", s1);
            g.writeNumberField("b", b);
            g.writeNumberField("d", d);
            g.writeEndObject();
        }
    }

    Segment parseSegment(JsonParser p) throws IOException {
        double[] bd = new double[2];
        long[] as = new long[2];
        JsonToken t;
        int idx = 0;
        Segment s = null;
        while (true) {
            t = p.nextToken();
            if (t == JsonToken.FIELD_NAME) {
                String name = p.getCurrentName();
                t = p.nextToken();
                if ("idx".equals(name)) {
                    idx = p.getValueAsInt();
                } else if ("s0".equals(name)) {
                    as[0] = p.getValueAsLong();
                } else if ("s1".equals(name)) {
                    as[1] = p.getValueAsLong();
                } else if ("b".equals(name)) {
                    bd[0] = p.getValueAsDouble();
                } else if ("d".equals(name)) {
                    bd[1] = p.getValueAsDouble();
                } else {
                    throw new IllegalStateException("unknown field name:" + name);
                }
            } else if (t == JsonToken.END_OBJECT) {
                break;
            } else {
                throw new IllegalStateException("parsing logic is not right");
            }
        }
        s = new Segment(idx, as[0], as[1], bd[0], bd[1]);
        if (true) {
            s.findParameters(0, (s.s0 + s.s1) / 2, s.s0);
        }
        return s;
    }

//    class Segment0 extends Segment {
//
//        /*
//        P(t)=e^(-a*t)   P(tf)=alpha, so tf=-ln(alpha)/a
//         */
//        double a;
//
//        Segment0(double s1, double a) {
//            super(0, s1);
////            super(0, s1, 0, 0); //, a
//            if (a == 0) {
//                throw new IllegalArgumentException();
//            }
//            this.a = a;
//        }
//
//        @Override
//        double tf(double P) {
//            if (P > 1) {
//                P = 1;
//            }
//            return -Math.log(P) / a; //s0 * (b - Math.log(P - d)) / (b - Math.log(1 - d));
//        }
//
//        @Override
//        void findParameters(double tfLearned) {
//            a = -Math.log(Ptarget) / tfLearned;
//        }
//
//    }
    class Selector {

        long getForecast(double t) {
            double tf = 0;
            ArrayList<Segment> al = new ArrayList<>();
            for (int i = 0; i < segments.length; i++) {
                Segment s = segments[i];
                if (s.s0 < t && t <= s.s1) {  //TODO: separate them
                    al.add(s);
                }
            }
            int n = al.size();
            if (n == 0) {
//                try {
//                    throw new IllegalStateException(t + " not in " + segments[0].s0 + " & " + segments[segments.length - 1].s1);
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
                return tfMax;
            } else //if (n == 1) 
            {
                double dt = Double.POSITIVE_INFINITY; //choose the minimum one.
                for (int i = 0; i < n; i++) {
                    Segment s = al.get(i);
                    double dT = 0, tfT = 0;
                    while (true) {
                        dT = s.tf(Ptarget, t);
                        tfT = t + dT;
                        if (tfT > tfMax) {
                            s.findParameters(0, tfMax, t);
                        } else {
                            break;
                        }
                    }
                    if (dT < dt) {
                        dt = dT;
                    }
                    System.out.println(s.idx + "/" + n + " " + t + " + " + dT + " -> " + dt);
                }
                tf = t + dt;
            }
//            else {
//                /*TODO:  select one, select criteria: which one is the stablest?
//                at present, I just take average of them.
//                weighted average is better. how to choose those weights?
//                
//                 */
//                double[] atf = new double[n];
//                double total = 0;
//                for (int i = 0; i < n; i++) {
//                    atf[i] = al.get(i).tf(Ptarget, t);
//                    total += atf[i];
//                }
//                tf = total / n;
//            }
            long ltf = (long) tf;
            return ltf;
        }
    }

}
