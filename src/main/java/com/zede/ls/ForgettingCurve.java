package com.zede.ls;

import java.util.ArrayList;
import java.util.Collections;
//import java.util.Set;

/**
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
        Collections.sort(tested, ETestResult.cTime);
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
