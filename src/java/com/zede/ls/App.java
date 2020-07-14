package com.zede.ls;

import java.util.concurrent.Executor;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

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
}
