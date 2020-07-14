package com.zede.ls;

import java.util.SortedSet;
import java.util.TreeSet;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Serialized;
import javax.jdo.annotations.Unique;

/**
 *
 *
 * https://en.wikipedia.org/wiki/Forgetting_curve
 *
 * given a user, we know its target level. so we know all EKP's the user has to
 * learn, let's denote it as set A.
 *
 * given set A(of EKP's), we have to choose a minimum set of ETest's.
 *
 *
 * usual query task:
 *
 *
 *
 *
 *
 *
 *
 */
@Unique(name = "idx_user_kp", members = {"user", "kp"})
@PersistenceCapable
public class R_User_KP {

    @ForeignKey //@Persistent
    EUser user;
    @ForeignKey //@Persistent
    EKP kp;

    @Serialized
    SortedSet<ETestResult> tested; //TODO: use a array/list is good enough

    /* this should be updated as more data is collected for the user.
    at present, I update this upon the user logged in. No. do it when new data is collected.
     */
    @Persistent
    long ltsScheduled;

    //I should have 3 levels: short time level, medium time level, near future level.
    public static void onUserChosenLevel(EUser user, ELevel level) {
        ForgettingCurve fc = ForgettingCurve.getFor(user);
        SortedSet<ETestResult> tested = new TreeSet<>();
        for (EKP kp : level.kps) {
            R_User_KP r = new R_User_KP();
            r.user = user;
            r.kp = kp;
            boolean exists = false;//r exists or not?
            if (exists) {
            } else {
                r.ltsScheduled = fc.scheduleWith(tested);
            }
        }
    }

    public void onTested(long lts, ETest test, boolean good) {
        ETestResult tr = new ETestResult();
        tr.lts = (int) (lts / 1000 / 60); //to minutes
        tr.test = test;
        tr.good = good;
        tested.add(tr);
        App.getExecutor().execute(() -> {
            updateSchedule();
        });
    }

    void updateSchedule() {
        ltsScheduled = ForgettingCurve.getFor(user).scheduleWith(tested);
    }

}
