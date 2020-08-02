package com.zede.ls;

import java.util.ArrayList;
import java.util.Collections;
//import java.util.Set;

/**
 * I should use AI.
 * 
 * the evaluated object is the memory retention of EKP, the test  
 * 
 * dependent variables: ETestResult.good, ETestResult.n
 * 
 * independent variables: dt[i]=t[i]-t[i-1], dT[i]=t[i]-t[0]
 * 
 * meta parameters: the complexity of ETest: words of ETest.info, length of the audio record(seconds)
 *      other meta parameters.
 * 
 * 
 *
 */

public class ForgettingCurve {
    //https://www.ane.pl/pdf/5535.pdf

    //different user can have different curve.
    //but for now, I just use the same one.
    static ForgettingCurve one = new ForgettingCurve();

    public static ForgettingCurve getFor(EUser user) {
        return one;
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

}
