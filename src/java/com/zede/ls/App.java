package com.zede.ls;

import java.util.concurrent.Executor;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

/**
 *
 */
public class App {

    static EUser user1 = new EUser(1);
    static PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("langs");
    static PersistenceManager pm = pmf.getPersistenceManager();

    public static PersistenceManager getPM() {
        return pm;
    }

    public static Executor getExecutor() {
        return null;
    }
    
    
    void testSave() { //TODO: just as test
        PersistenceManager pm = App.getPM();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            ETest test = new ETest();
            test.fn = "   a.mp3";
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
    
}
