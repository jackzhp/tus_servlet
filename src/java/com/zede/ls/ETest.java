package com.zede.ls;

import java.util.Set;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

/**
 *
 */
@PersistenceCapable
public class ETest {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    int id;

    //TODO: how to specify it to be unique?
    @Persistent
    @Unique(name = "idx_fn")
    String fn;

    String source; //where the material came from. TODO: make it an object

    @Persistent
    String info;

    /*
    if an ETest does not have any EKP, then 
    itself is just an EKP, i.e. the ETest contains only 1 EKP.
    
    when ETest contains more than 1 EKP, the default one could be modified.
     */
    @Persistent(table = "tests_kps")
    @Join(column = "id_test")
    @Element(column = "id_kp")
    Set<EKP> kps;

    void save() { //TODO: just as test
        PersistenceManager pm = App.getPM();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            ETest test = new ETest();
            test.fn = "a.mp3";
            test.info = "「俺はスーパマン」と言いました。";
            EKP kp = new EKP(); //"Sony Discman", "A standard discman from Sony", 49.99);
            kp.desc = "とas quotation";
            test.kps.add(kp);
            kp.tests.add(test);
            kp = new EKP();
            kp.desc = "言う→言います→言いました";
            test.kps.add(kp);
            kp.tests.add(test);
            pm.makePersistent(test);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
    }

    void onTested(EUser user, long lts, Set<EKP> bads) {
        for (EKP kp : kps) {
            boolean good = bads.contains(kp) == false;
            R_User_KP r = null; //find it by user, kp
            if (r == null) {
                r = new R_User_KP();
                r.user = user;
                r.kp = kp;
            }
            r.onTested(lts, this, good);
        }
    }

}
