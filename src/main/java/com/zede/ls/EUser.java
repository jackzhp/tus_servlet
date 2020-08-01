package com.zede.ls;

//import javax.jdo.annotations.PersistenceCapable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.jdo.annotations.Persistent;

//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.PrimaryKey;
/**
 *
 */
//@PersistenceCapable
public class EUser {

//    @PrimaryKey
//    int id;
    String name, pk, password;
    String device, lastIP; //TODO: etc to help to identify the user.
    String fn;
    /*  but could be 5.1 to 5.20.  the 20 classes for grade 5, and each class's EKP's.
between the actual and the target, there might be a huge gap.
    the actual level & target level should be on UI, so the user knows it.
    and its level system name.
    the actual level is auto detected with tests. the target level is what the user can choose.
     */
//    @Persistent
    ELevel actual, target; //like grade 5
//    @Persistent
//    ELevel actual;
    HashMap<ELevel, ELevelTested> levelTested = new HashMap<>();

    /* why do I need this? 
    for example I should try to avoid returning a test which is just used within 1 minute.
     */
    int intervalNotReUse = 1000 * 60 * 5; //5 minutes.  this could be different for different user.
    /* if no concurrent issue, we can use TreeMap, or we have to use ConcurrentSkipListMap
      sorted by timestamp. No. we do not have to sort them before we do removing.
    
    it is wrong to set its Comparator with ETestResult.cTime.
     */
//    TreeMap<ETestResult, ETestResult> testsUsed = new TreeMap<>(); //HashSet<ETestResult> testsUsed = new HashSet<>();
    HashMap<Integer, ETestResult> testsUsed = new HashMap<>(); //HashSet<ETestResult> testsUsed = new HashSet<>();
    static int testsUsed_sizeMin = 100, testsUsed_sizeMax = 120;
    static float LevelThreshold = 0.9f; // 0.75f;

    /*
the KP's level is too low compared to the actual level, then its test result could be ommitted, unless
    the test result is not good.
    
    its keyset is the EKP's which the user is learning.
     */
//    ArrayList<ETestResult> tested; //this field will be with huge data!!!!
//    HashMap<EKP,ArrayList<ETestResult>> results;
    HashMap<Integer, EKPscheduled> results = new HashMap<>(); //indexed by EKP.id

    /* scheduled on EKP, and when the user query, we find out tests right away.
    originally on ETest, then new ETest can not be utilized immediately.
    
    from a list of EKPscheduled to a minimum list of ETest, this is NP hard problem.
     */
//    ArrayList<ETest> scheduled=new ArrayList<>();
//    LinkedList<EKPscheduled> scheduled = new LinkedList<>();
//    EKPscheduled[] scheduled;// = new LinkedList<>();
    EKPscheduled[] scheduled;// ArrayList<EKPscheduled> scheduled = new ArrayList<>();
    boolean shouldSortScheduled;
    Scheduler scheduler;

    //we do not use WeakReference since we have authentication and deauthentication.
    static ConcurrentHashMap<String, EUser> users = new ConcurrentHashMap<>();

    EUser() {
//        id = i;
    }

    /* I should have 3 levels: short time level, medium time level, near future level.
    No. we do not need these. auto detect actual level, and auto advance to the target level.
     */
//    public void onUserChosenLevel(ELevel level) {
//        EUser user = this;
//        ForgettingCurve fc = ForgettingCurve.getFor(user);
//        ArrayList<ETestResult> tested = new ArrayList<>();
//        for (EKP kp : level.kps) {
//            R_User_KP r = new R_User_KP();
//            r.user = user;
//            r.kp = kp;
//            boolean exists = false;//r exists or not?
//            if (exists) {
//            } else {
//                r.ltsScheduled = fc.scheduleWith(tested);
//            }
//        }
//    }
    private void updateActualLevel() {
        try {
            Set<ELevel> s = levelTested.keySet();
            ArrayList<ELevel> al = target.sys.levels;
//            new ArrayList<>();
//            for (ELevel level : s) {
//                if (level.sys == target.sys) {
//                    al.add(level);
//                }
//            }
            ELevel[] a = al.toArray(new ELevel[0]);
            Arrays.sort(a, target.sys.c);
            boolean set = false;
            if (false) {
                for (int i = a.length - 1; i > 0; i--) {
                    ELevel level = a[i];
                    ELevelTested lt = levelTested.get(level);
                    if (lt != null) {
                        //level threshold: t=75% good
                        int total = lt.good + lt.bad;
                        if (total * LevelThreshold >= lt.good) {//  good/(good+bad) < t
                            //do not have enough good, so the user's level is lower than this
                        } else { //the current level is the actual level.
                            actual = level;
                            set = true;
                            break;
                        }
                    }
                }
            }
            ELevel lO = null;
            for (int i = 0; i < a.length; i++) {
                ELevel level = a[i];
                ELevelTested lt = levelTested.get(level);
                if (lt != null) {
                    //level threshold: t=75% good
                    int total = lt.good + lt.bad;
                    if (total * LevelThreshold >= lt.good) {//  good/(good+bad) < t
                        //do not have enough good, so the user's level is lower than this
                        actual = lO != null ? lO : level;
                        set = true;
                        break;
                    } else { //the current level is the actual level.
                        lO = level;
                    }
                }
            }
            if (set) {
//                System.out.println("actual level:" + actual.levelString());
            } else {
                actual = target.sys.getLevel_m(1, 1);// a[0];
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * when this is called, the test must be just accessed not long ago.
     *
     *
     *
     * @param lts
     * @param kp
     * @param testid
     * @param good
     */
//    public void onTested(long lts, ETest test, boolean good) {
//    public void onTested(long lts, EKP kp, ETest test, boolean good) {
    private EKPscheduled onTested(long lts, EKP kp, int testid, boolean good) {
        int kpid = kp.id;
        ELevel level = kp.getLevel(target.sys);
        ELevelTested lt = levelTested.get(level);
        if (lt == null) {
            lt = new ELevelTested(level);
            levelTested.put(level, lt);
        }
        if (good) {
            lt.good++;
        } else {
            lt.bad++;
        }
        updateActualLevel();
//        onTested(lts,kp.id,good);
//    }
//    public void onTested(long lts, int kpid, boolean good) {
        /* this check we do not do it here.
        no, we should do it here.
        otherwise, for a high level user, we will keep too much low level info!
         */
        if (good == false || shouldKeepTestResult(kp)) {
            ETestResult tr = testsUsed.get(testid);
            if (tr != null && tr.testid != -1) {
                tr = null;
            }
            if (tr == null) {
                tr = new ETestResult();
                tr.testid = testid; //tr.kp = kp;//  tr.test = test;
            }
            tr.lts = lts; // (int) (lts / 1000 / 60); //to minutes
            tr.good = good;
            addToUsed(tr);
//            results.get(kpid);
//            ArrayList<ETestResult> tested = results.get(kpid);
            EKPscheduled kps = results.get(kpid);
            if (kps == null) {
//                tested = new ArrayList<>();
                kps = new EKPscheduled();
                kps.set(kp); //kps.kp = kp; //kps.id = kpid;
                results.put(kpid, kps); //tested
            }
            kps.updateSchedule().thenAccept(tf -> {
            }).exceptionally(t -> {
                t.printStackTrace();
                return null;
            });
            kps.tested.add(tr);
            save(100); //since we are the server, continuously running, so 100 seconds should be good.
            return kps;
        }
        return null;
    }

    public CompletableFuture<Void> onTested(long lts, int testid, int[] bads) {
//                ETest test = ETest.loadByID_m(testid);      
        return ETest.getByID_cf(testid).thenApply((ETest test) -> { //Compose

            return test.getKPs(); //test.getKPs_cf();
        }).thenCompose((EKP[] kps) -> {
            CompletableFuture[] cfs = new CompletableFuture[kps.length];
            for (int i = 0; i < kps.length; i++) {
                EKP kp = kps[i];
                boolean good = true; //false;// = bads.contains(kp) == false;
                for (int bpidBad : bads) {
                    if (kp.id == bpidBad) {
                        good = false;
                        break;
                    }
                }
                /* why it can return null?
                the user's current level is high, but the kp is of very low level.
                in that case, the return should be null.
                 */
                EKPscheduled kpst = onTested(lts, kp, testid, good);
                cfs[i] = kpst != null ? kpst.updateSchedule() : CompletableFuture.completedFuture(true);
            }
            return CompletableFuture.allOf(cfs);
//        }).thenApply((Void tf) -> {
//            return true;
//        }).thenAccept(v -> {
//            Collections.sort(scheduled); //as long as shouldSortScheduled is set, we do not have to do this now
        });
//        .exceptionally((Throwable t) -> {
//            t.printStackTrace();
//            return null;
//        });
    }

    /*
    
    TODO: if the user's level is high, then for those low level KP's in a test, we do not have to keep them.
    unless the test result is not good.
     */
    private boolean shouldKeepTestResult(EKP kp) {
//        return true; //TODO: for temp, all should be kept.   
        if (actual == null) {
            return true;
        }
        ELevel lt = kp.getLevel(target.sys);
        return lt.idMajor + 1 >= actual.idMajor;
    }

    /*
    when the user's actual level is far from her target level, 
    we should suggest the user to adjust target level. No! not needed, we will auto detect her actual level,
    and we will advance levels gradually.
     */
    void setTarget(ELevel level) {
        if (level == null) {
            throw new IllegalArgumentException();
        }
        if (target != null) {
            if (level.sys != target.sys) {
                levelTested.clear(); //different level system,so clear the old one.
                actual = null;
            }
        }
        target = level;
        System.out.println("target:" + level.levelString());
//        updateSchedule0(); //anyway, upon authenticated, this will be called again.
//        getScheduler().set(level, true);
//        App.getExecutor().submit(scheduler);
    }

    void updateSchedule0() {
        updateSchedule(target, true);
    }

    /**
     *
     *
     *
     * @param level
     * @param must always true? seems no point to be false.
     * @return
     */
    CompletableFuture<Boolean> updateSchedule(ELevel level, boolean must) {
        if (scheduler == null) {
            scheduler = new Scheduler(results.values());
        }
        CompletableFuture<Boolean> cf = scheduler.set(level, must);
        return cf;
    }

    Fetcher fetcher;

    /**
     * the user wants a test. return the test id.
     *
     *
     * @param reviewOnly when this is true, a test should not include any new
     * EKP's. usually, the user is encouraged not to set it. then review can be
     * done through new learning.
     *
     * @return
     */
    CompletableFuture<ETest[]> getTest(boolean reviewOnly) {
        //now we choose a test from ETests covering kps
        //at present, I do not try to find the minimum covering set of ETest. I serve the user one by one.
        ELevel limit;
        if (actual != null) {
            if (reviewOnly) {
                limit = actual;
            } else {
                limit = actual.sys.nextLevel(actual);
            }
        } else {
            limit = target;
        }
        if (fetcher == null) {
            fetcher = new Fetcher();
        }
        return fetcher.fetch(2, limit);
    }

    Comparator<ETest> cETests = new Comparator<ETest>() {
        @Override
        public int compare(ETest o1, ETest o2) {
            ELevelSystem sys = target.sys;
            ELevel l1 = o1.highestLevel(sys);
            ELevel l2 = o2.highestLevel(sys);
            return sys.c.compare(l1, l2);
        }
    };

    private void jsonLevelTested(JsonGenerator g) throws IOException {
        ELevelSystem sys = target.sys;
        Set<Map.Entry<ELevel, ELevelTested>> s = levelTested.entrySet();
        g.writeStartObject();
        g.writeStringField("sys", sys.name);
        for (Map.Entry<ELevel, ELevelTested> me : s) {
            ELevel level = me.getKey();
            if (level.sys != sys) {
                continue;
            }
            ELevelTested lt = me.getValue();
            String key = level.levelString();
            g.writeFieldName(key);
            lt.json(g);
        }
        g.writeEndObject();
    }

    private void parseLevelTested(JsonParser p) throws IOException {
        String sysName = null, sysNameT = null;
        if (target != null) {
            sysNameT = target.sys.name;
        }
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            boolean shouldDiscard = false;
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();//.currentName();
                    if ("sys".equals(name)) {
                        sysName = p.nextTextValue();
                        if (sysName.equals(sysNameT)) {
                        } else {
                            shouldDiscard = true;
                        }
                    } else { //name is levelString such as "1.2"
                        ELevel level = ELevel.get_m(sysName, name);
                        ELevelTested lt = new ELevelTested(level);
                        lt.parse(p);
                        levelTested.put(level, lt);
                    }
                    continue;
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                }
            }
            if (shouldDiscard) {
                levelTested.clear();
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    EUser set(String fn) {
        this.fn = fn;
        return this;
    }

    private static EUser getByFileName(String fn) throws IOException {
        EUser user = users.get(fn);
        if (user == null) {
            user = new EUser();
            EUser userO = users.putIfAbsent(fn, user);
            if (userO != null) {
                user = userO;
            } else {
                user.set(fn);
                user.loadFrom();
            }
        }
        return user;
    }

    private static String searchPK(File f) throws FileNotFoundException, IOException {
        byte[] abPK = "\"pk\":\"".getBytes("utf8"); //TODO: am I sure no space after/before ':'?
        byte[] abPass = "\"password\":\"".getBytes("utf8"); //TODO: am I sure no space after/before ':'?
        byte[] abEnd = "\"".getBytes("utf8");
        byte[] bytes = new byte[4096];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        int iStart = 0, off = 0;
        byte[][] targets = new byte[][]{abPK, abPass}; //the sequence matters.
        String[] results = new String[targets.length];
        int i = 0;
        byte[] target = targets[i];
        moreData:
        while (true) {
            int len = bis.read(bytes, off, bytes.length - off);
            if (len == -1) {
                break;
            }
            off += len;
            while (true) {
                int idxStart = App.indexOf(bytes, iStart, off, target);
                if (idxStart != -1) {
                    int idx = idxStart + target.length;
                    int idxEnd = App.indexOf(bytes, idx, off, abEnd);
                    if (idxEnd != -1) {
                        len = idxEnd - idx;
                        results[i] = new String(bytes, idx, len, "utf8");
                        iStart = idxEnd;
                        i++;
                        if (i < targets.length) {
                            target = targets[i];
                        } else {
                            break moreData;
                        }
                    } else {
                        off -= idxStart;
                        System.arraycopy(bytes, idxStart, bytes, 0, off);
                        continue moreData;
                    }
                }
            }
        }
        return results[0] + ":" + results[1];
    }

    void loadFrom() throws IOException {
        File f = new File(App.dirUsers(), getFileName());
        JsonParser p = App.getJSONparser(f);
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    p.nextToken();
                    if ("results".equals(name)) {
//                        results = new HashMap<>();
//                        testsUsed = new HashSet<>();
//                        scheduled = new ArrayList<>();
                        while (true) {
                            t = p.nextToken();
                            if (t == JsonToken.FIELD_NAME) {
                                name = p.getCurrentName();
                                int kpid = Integer.parseInt(name);
                                EKPscheduled kps = new EKPscheduled();
                                kps.kpid = kpid;
                                kps.load(p);
                                if (kps.kpid != kpid) {
                                    throw new IllegalStateException("kpid " + kps.kpid + " unexpected " + kpid);
                                }
                                results.put(kpid, kps);
                            } else if (t == JsonToken.END_OBJECT) {
                                break;
                            } else {
                                throw new IllegalStateException("unexpected " + t);
                            }
                        }
//                        scheduled = new ArrayList<>(results.values());
                    } else if ("levelt".equals(name)) {
                        target = ELevel.parseSimple(p);
                    } else if ("levela".equals(name)) {
                        actual = ELevel.parseSimple(p);
                    } else if ("levelTested".equals(name)) {
                        parseLevelTested(p);
                    } else {
                        String value = p.getValueAsString();
                        if ("name".equals(name)) {
                            this.name = value;
                        } else if ("password".equals(name)) {
                            password = value;
                        } else if ("pk".equals(name)) {
                            pk = value;
                        } else if ("device".equals(name)) {
                            device = value;
                        } else if ("lastIP".equals(name)) {
                            lastIP = value;
                        } else {
                            throw new IllegalStateException("unknown field name:" + name);
                        }
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
    long saveRequested, saveLast, saveNext; //the next means the next saving must be after that.
    boolean nextSaveScheduled;
    AtomicBoolean saving = new AtomicBoolean();
    private static long saveDelayDefault = 100; //seconds

    void save(long delay) { //this method is same for all 
        if (pk == null) {
            throw new IllegalStateException("pk should not be null");
        }
        long ltsnow = System.currentTimeMillis();
        saveRequested = ltsnow;
        if (nextSaveScheduled) {
        } else {
            long delay2 = saveNext - ltsnow;
            delay2 /= 1000;
            delay2++;
            if (delay < delay2) {
                delay = delay2;
            }
            App.getExecutor().schedule(() -> {
                try {
                    nextSaveScheduled = false;
                    save();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, delay, TimeUnit.SECONDS);
            nextSaveScheduled = true;
        }
    }

    CompletableFuture<Boolean> save_cf() {
        CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
        try {
            if (pk == null) {
                throw new IllegalStateException("pk should not be null");
            }
            this.saveRequested = System.currentTimeMillis();
            save();
            cf.complete(true);
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

    void save() throws IOException {
        if (saving.compareAndSet(false, true)) {
            try {
                long ltsnow = System.currentTimeMillis();
                if (saveLast <= saveRequested && ltsnow > saveNext) {
                    File fnew = getFile(true);
                    JsonGenerator g = App.getJSONgenerator(fnew);
                    g.writeStartObject();
                    saveLast = ltsnow;
                    saveNext = saveLast + saveDelayDefault;
                    g.writeStringField("name", name);
                    g.writeStringField("pk", pk); //TODO: use pk, stop using password
                    g.writeStringField("password", password);
                    g.writeStringField("device", device);
                    g.writeStringField("lastIP", lastIP);
                    if (target != null) {
                        g.writeFieldName("levelt");
                        target.jsonSimple(g);
                    }
                    if (actual != null) {
                        g.writeFieldName("levela");
                        actual.jsonSimple(g);
                    }
                    g.writeFieldName("levelTested");
                    jsonLevelTested(g);
                    //testsUsed we do not save them, instead when loading the results, we construct it.
                    //we do not need a field named scheduled, either. we construct when we load results.
//        g.writeArrayFieldStart("scheduled");
//        for (EKPscheduled kps : scheduled) {
//
//        }
//        g.writeEndArray();
                    g.writeFieldName("results");
                    Set<Integer> s = results.keySet();
                    g.writeStartObject(); //results
                    for (Integer kpid : s) {
                        g.writeFieldName(kpid.toString());
                        results.get(kpid).json(g);
                    }
                    g.writeEndObject(); //results
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    File f = getFile(false);
                    if (f.exists()) {
                        f.delete();
                    }
                    boolean tf = fnew.renameTo(f);
                    if (tf) {
                        System.out.println("ok to rename " + fnew.getName() + " -> " + f.getName());
                    } else {
                        System.out.println("failed to rename " + fnew.getName() + " -> " + f.getName());
                    }
                }
            } finally {
                saving.set(false);
            }
        }
    }

    //use username to identify people.
//    static HashMap<Integer, EUser> users = new HashMap<>(); //put it here once authenticated, remove it once deauthenticated.
//    public static EUser getByID(int id) {
//
//        return user1;
//    }
    static CompletableFuture<HashMap<String, String>> getUserNames(String username) {
        CompletableFuture<HashMap<String, String>> cf = new CompletableFuture<>();
        App.getExecutor().submit(() -> {
            try {
                HashMap<String, String> hm = new HashMap<>(); //key: fn, value: password
                //load those User's file, with same username. and put their password as value
                File dir = App.dirUsers();
                String[] fns = dir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(username) && name.endsWith(".json");
                    }
                });  //filename starts with username
                for (String fn : fns) {
                    File f = new File(dir, fn);
                    if (f.length() == 0) {
                        f.delete();
                        continue;
                    }
                    String password = searchPK(f);
                    hm.put(fn, password);
                }
                cf.complete(hm);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
        });
        return cf;
    }
    private boolean authenticated;

    static void deauthenticate(EUser user) {
        if (user == null) {
            return;
        }
        user.authenticated = false;
        user.save_cf().thenAccept(tf -> {
            if (user.authenticated) {
            } else {
                users.remove(user.fn);
            }
        }).exceptionally(t -> {
            t.printStackTrace();
            return null;
        });
    }

    /*
   since BTC PK is a little bit too complicated, so use username and password only.
    sig is the password merged with nonce.
    TODO: but eventually, I will use BTC PK
     */
    static EUser authenticate(HashMap<String, String> passes, String nonce, String pk, String sig) {
        try {
            Set<Map.Entry<String, String>> s = passes.entrySet();
            for (Map.Entry<String, String> me : s) {
                String[] as = me.getValue().split(":"); //0: pk, 1: password
                String pkLocal = as[0];
                if (pkLocal.equals(pk)) {
                    String passLocal = as[1];
                    String sigCalculated = merge(passLocal, nonce);
                    if (sigCalculated.equals(sig)) {
                        String fn = me.getKey();
                        EUser user = EUser.getByFileName(fn);
                        user.authenticated = true;
                        StringBuilder sb = new StringBuilder();
                        sb.append("after loaded, #of test results for 0:");
                        EKPscheduled kps = user.results.get(0);
                        if (kps != null) {
                            sb.append(kps.tested.size());
                        } else {
                            sb.append("null");
                        }
                        System.out.println(sb.toString());
                        user.updateSchedule0();
                        return user;
                    } else {
                        System.out.println("sig:" + sigCalculated + ":" + sig);
                    }
                } else {
                    System.out.println("pk:" + pkLocal + ":" + pk);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    //TODO: use sha256
    //but for now.
    private static String merge(String pass, String nonce) {
        return pass + nonce;
    }

    //the user is request level info.
    void prepareToServeLevel(ByteArrayOutputStream baos) throws IOException {
        EUser user = this;
        JsonGenerator g = App.getJSONgenerator(baos);
        g.writeStartObject();
        g.writeFieldName("target");
        user.target.jsonSimple(g);
//        g.writeEndObject();
        g.writeFieldName("actual");
        if (user.actual != null) {
            user.actual.jsonSimple(g);
        } else {
            g.writeNull();
        }
        g.writeFieldName("tested");
        jsonLevelTested(g);
        g.writeEndObject();
        g.flush();
        g.close();
    }

    String getFileName() {
        if (fn == null) {
            fn = name + pk + ".json";
        }
        return fn;
    }

    private File getFile(boolean extnew) {
        File dir = App.dirUsers();
        String fn = getFileName();
        if (extnew) {
            fn += ".new";
        }
        File f = new File(dir, fn);
        if (extnew) {
            if (f.exists()) {
                f.delete();
            }
        }
        return f;
    }

    static EUser[] replace(EKP remove, EKP keep) {
        File dir = App.dirUsers();
        ArrayList<EUser> al = new ArrayList<>();
        String[] afn = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        for (String fn : afn) {
            EUser user;
            try {
                user = EUser.getByFileName(fn);
            } catch (Throwable t) {
                System.out.println("failed for:" + fn);
                t.printStackTrace();
                continue;
            }
            EKPscheduled kpRemove = user.results.get(remove.id);
            if (kpRemove == null) {
                continue;
            } else {
                EKPscheduled kpKeep = user.results.get(keep.id);
                if (kpKeep == null) {
                    kpKeep = kpRemove;
                    kpKeep.kp = keep;
                    kpKeep.kpid = keep.id;
                    user.results.put(keep.id, kpKeep);
                } else {
                    kpKeep.tested.addAll(kpRemove.tested);
                }
                user.results.remove(remove.id);
                al.add(user);
            }
        }
        return al.toArray(new EUser[0]);
    }

    /**
     * it does not matter how is testsUsed sorted. when we want to constraint
     * its size, we have to use comparator based on time. while the map should
     * be based on testid.
     *
     * @param tr
     */
    private void addToUsed(ETestResult tr) {
        testsUsed.put(tr.testid, tr);
        int n = testsUsed.size();
        if (n > testsUsed_sizeMax) {
            ETestResult[] atr = testsUsed.values().toArray(new ETestResult[0]);
            Arrays.sort(atr, ETestResult.cTime);
            n = testsUsed_sizeMax - testsUsed_sizeMin;
            for (int i = 0; i < n; i++) {
                testsUsed.remove(atr[i].testid);
            }
        }
    }

    class ELevelTested {

        ELevel level;
        int good, bad;

        ELevelTested(ELevel level) {
            this.level = level;
        }

        void good() {
            good++;
        }

        void bad() {
            bad++;
        }

        /**
         * seems not needed! these data are in EKPscheduled. but if use that to
         * reconstruct the good & bad data, then we will have to query the level
         * for every EKP which is expensive.
         *
         * so I just save these data.
         *
         *
         * @param p
         * @throws IOException
         */
        void parse(JsonParser p) throws IOException {
            JsonToken t = p.nextToken();
            if (t == JsonToken.START_OBJECT) {
                t = p.nextToken(); //field name
                t = p.nextToken(); //field value
                good = p.getValueAsInt();
                t = p.nextToken(); //field name
                t = p.nextToken(); //field value
                bad = p.getValueAsInt();
                t = p.nextToken();
                if (t == JsonToken.END_OBJECT) {
                } else {
                    throw new IllegalStateException("expecting end object, but " + t);
                }
            } else {
                throw new IllegalStateException("expecting start object, but " + t);
            }
        }

        void json(JsonGenerator g) throws IOException {
            g.writeStartObject();
            g.writeNumberField("good", good);
            g.writeNumberField("bad", bad);
            g.writeEndObject();
        }
    }

    class Scheduler implements Runnable {

        //TODO: should shoulde be triggered only when a test results is saved.
        private boolean must;
        private ELevel level;
        AtomicBoolean running = new AtomicBoolean();
        ConcurrentLinkedQueue<CompletableFuture<Boolean>> request = new ConcurrentLinkedQueue<>();
        /*
        those EKP of low level, and mastered should be removed(not to be rescheduled unncecessarily).
         */
        private final HashSet<EKPscheduled> kpsRelevant = new HashSet<>();

        Scheduler(Collection<EKPscheduled> values) {
            kpsRelevant.addAll(values);
            System.out.println("scheduler inited with " + kpsRelevant.size());
            if (false) { //I have scheduled field saved, so I do not have to redo it at this time.
                App.getExecutor().submit(() -> {
                    for (EKPscheduled kps : values) {
                        kps.updateSchedule();
                    }
                });
            }
        }

        CompletableFuture<Boolean> set(ELevel level, boolean must) {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            try {
                if (level == null) {
                    throw new IllegalArgumentException();
                }
                this.level = level;
                this.must = must;
                request.add(cf);
                /* TODO: running is not reset, then scheduler will not be executed anymore.
            reset it after 10 minutes.
                 */
                if (running.compareAndSet(false, true)) {
                    App.getExecutor().submit(this);
                }
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
            return cf;
        }

        /**
         * TODO: results are not used yet!!!
         *
         *
         */
        @Override
        public void run() {
//            if (running.compareAndSet(false, true)) {
            CompletableFuture<Boolean> cf;
            try {
                if (level != null) {
                    if (level.sys == null) {
                        throw new IllegalStateException("this should not happen");
                    }
                } else {
                    throw new IllegalStateException("this should not happen");
                }
                schedule().thenApply(tf -> {
                    System.out.println(level.levelString() + " is scheduled:" + kpsRelevant.size());
                    if (level.sys == null) {
                        throw new IllegalStateException("this should not happen");
                    }
                    level = level.sys.previousLevel(level.idMajor, level.idMinor);
                    if (level != null) {
                        if (level.sys == null) {
                            throw new IllegalStateException("this should not happen");
                        }
                        App.getExecutor().submit(this);
                    } else {
                        onSucceeded();
                        System.out.println("no more level");
                    }
                    return true;
                }).exceptionally(t -> {
                    onFailed(t);
                    return true;
                });
//                } finally {
//                    running.set(false);
            } catch (Throwable t) {
                onFailed(t);
            }
//            } else {
//                System.out.println("shceduler is running, so skip");
//            }
        }

//ELevel level
        CompletableFuture<Boolean> schedule() {
            System.out.println("schedule level:" + level.levelString());
            Integer[] akpid = level.kps.toArray(new Integer[0]);
            EKPscheduled kpst = new EKPscheduled();
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean>[] cfs = new CompletableFuture[akpid.length];
            for (int i = 0; i < cfs.length; i++) {
                Integer kpid = akpid[i];
                kpst.kpid = kpid;
                if (kpsRelevant.contains(kpst)) {
                    cfs[i] = CompletableFuture.completedFuture(true);
                    continue;
                }
                cfs[i] = EKP.getByID_cf(kpid).thenCompose((EKP kp) -> {
                    EKPscheduled kps = new EKPscheduled();
                    kps.set(kp);
                    kpsRelevant.add(kps);
                    return kps.updateSchedule();
                }).exceptionally(t -> {
                    if (t instanceof FileNotFoundException) {
                        level.kps.remove(kpid);
                    }
                    return true;
                });
            }
            return CompletableFuture.allOf(cfs).thenApply((Void v) -> {
//                EKPscheduled[] akps = kpsRelevant.toArray(new EKPscheduled[0]);
//                Arrays.sort(akps, cTime);
//                kpsRelevant.clear();
//                kpsRelevant.addAll(Arrays.asList(akps));
                int size = kpsRelevant.size(); //akps.length;
                System.out.println(level.levelString() + " #ofKP scheduled:" + size); //java.lang.NullPointerException
                return true;
            });
//                .exceptionally((Throwable t) -> {
//            t.printStackTrace();
//            return null;
//        });
        }

        private void onSucceeded() {
            scheduled = kpsRelevant.toArray(new EKPscheduled[0]);
            System.out.println("eventually # scheduled:" + scheduled.length);
            //TODO: do we have to sort them now?
            shouldSortScheduled = true;
            running.set(false); //succeeded
            for (CompletableFuture<Boolean> cf : request) {
                cf.complete(true);
            }
        }

        private void onFailed(Throwable t) {
            t.printStackTrace();
            running.set(false); //failed
            for (CompletableFuture<Boolean> cf : request) {
                cf.completeExceptionally(t);
            }
        }
    }
    static Comparator<EKPscheduled> cTime = new Comparator<EKPscheduled>() {
        @Override
        public int compare(EKPscheduled o1, EKPscheduled o2) {
            long dt = o1.ltsScheduled - o2.ltsScheduled;
            return dt < 0 ? -1 : dt > 0 ? 1 : 0;
        }
    };

    class EKPscheduled { //in this class, we do not keep the description of the EKP, we keep only schedule related info.

        EKP kp;
        int kpid; //same as EKP.id
//        int levelMajor, levelMinor; //why do I keep them here? if I don't, then I will have to load EKP frequently.

        /* I have to keep them here. same reason as above, but this one in EKP might be update frequently.
        when I keep this redundant copy, the update will not be reflected.
         */
//        ArrayList<Integer> tests0 = new ArrayList<>();
//        ArrayList<ETest> tests = new ArrayList<>();
        CompletableFuture<ETest[]> getTests() {
            return getEKP().thenApply((EKP kp) -> {
                return kp.getTests();
            }); //null;
        }

        CompletableFuture<EKP> getEKP() {
            if (kp == null) {
                return EKP.getByID_cf(kpid).thenApply((EKP kp0) -> {
                    return kp = kp0;
                });
            }
            return CompletableFuture.completedFuture(kp);
        }

        /* this should be updated as more data is collected for the user.
    at present, I update this upon the user logged in. No. do it when new data is collected.
         */
        long ltsScheduled;
        /* this EKP are tested with many different ETest
        are they sorted? yes, since they are added sequentially over time.
         */
        ArrayList<ETestResult> tested = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (o != null) {
                if (o instanceof EKPscheduled) {
                    EKPscheduled kps2 = (EKPscheduled) o;
                    return kpid == kps2.kpid;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return kpid;
        }

        CompletableFuture<Boolean> updateSchedule() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            try {
                if (kpid == 0 || kpid == 97) {
                    System.out.println(kpid + " # of tested:" + tested.size());
                }

                ltsScheduled = ForgettingCurve.getFor(EUser.this).scheduleWith(tested);
//                System.out.println("dt:" + (ltsScheduled - System.currentTimeMillis()));
                shouldSortScheduled = true;
                cf.complete(true);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
            return cf;
        }

        /* TODO: for performance, tests2 should be with only testid.
        is this possible?
         */
        /**
         *
         *
         * @param limit
         * @param tests2
         * @return
         */
        private CompletableFuture<Boolean> filterTests(ELevel limit, ArrayList<ETest> tests2) {
            ELevelSystem sys = limit.sys;
            return getTests().thenApply((ETest[] tests) -> {
//                System.out.println("#ofETests:" + tests.length);
                Arrays.sort(tests, cETests);
                for (int i = tests.length - 1; i >= 0; i--) {
                    ETest test = tests[i];
                    if (sys.c.compare(test.highestLevel(sys), limit) <= 0) {
                        for (; i >= 0; i--) {
                            test = tests[i];
                            tests2.add(test);
                        }
                    }
                }
//                System.out.println("after filtered with level" + sys.name + "-" + limit.levelString() + ":" + tests.length);
//                if (tests2.isEmpty()) {
//                    tests2.add(tests[0]); //TODO: java.lang.ArrayIndexOutOfBoundsException: 0
//                }
                return true;
            });
        }

        private void json(JsonGenerator g) throws IOException {
            g.writeStartObject();
            g.writeNumberField("scheduled", ltsScheduled / 1000 / 60); //minutes is good enough
            g.writeArrayFieldStart("tested");
            for (ETestResult tr : tested) {
                tr.json(g);
            }
            g.writeEndArray();
            g.writeEndObject();
        }

        void load(JsonParser p) throws IOException {
            JsonToken t = p.nextToken();
            if (t == JsonToken.START_OBJECT) {
                while (true) {
                    t = p.nextToken();
                    if (t == JsonToken.FIELD_NAME) {
                        String name = p.getCurrentName();
                        t = p.nextToken();
                        if ("scheduled".equals(name)) {
                            ltsScheduled = p.getValueAsLong() * 1000 * 60;
                        } else if ("tested".equals(name)) {
                            //TODO: better to use object? No!
//                            t = p.nextToken();
                            if (t == JsonToken.START_ARRAY) {
                                while (true) {
                                    t = p.nextToken();
                                    if (t == JsonToken.END_ARRAY) {
                                        break;
                                    }
                                    ETestResult tr = new ETestResult();
                                    tr.load(p);
                                    tested.add(tr);
                                    addToUsed(tr);
                                }
                            } else {
                                throw new IllegalStateException("expecting start array, but " + t);
                            }
                        } else {
                            throw new IllegalStateException("unknow field name:" + name);
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

        private void set(EKP kp) {
            this.kp = kp;
            this.kpid = kp.id;
        }

    }

    class Fetcher implements Runnable {

        int idx, n;
        ELevel limitCurrent, limit;
        ConcurrentLinkedQueue<CompletableFuture<ETest[]>> request = new ConcurrentLinkedQueue<>();
        AtomicBoolean running = new AtomicBoolean();
        private ArrayList<ETest> testsT = new ArrayList<>();
        private HashSet<ETest> results = new HashSet<>();

        ;//ArrayList<ETest> results = new ArrayList<>();

        CompletableFuture<ETest[]> fetch(int n, ELevel limit) {
            CompletableFuture<ETest[]> cf = new CompletableFuture<>();
            this.n = n;
            this.limit = limit;
            if (running.compareAndSet(false, true)) {
                idx = 0;
                results.clear();
                App.getExecutor().submit(this);
            }
            request.add(cf);
            return cf;
        }

        CompletableFuture<Void> checkSchedule() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            if (limitCurrent != limit) {
                CompletableFuture<Boolean> cf0;
                if (scheduled == null || scheduled.length == 0) { //.isEmpty()
                    cf0 = updateSchedule(limit, true); //false
                } else {
                    cf0 = CompletableFuture.completedFuture(true);
                }
                cf0.thenAccept(tf -> {
                    limitCurrent = limit;
                    cf.complete(null);
                }).exceptionally(t -> {
                    cf.completeExceptionally(t);
                    return null;
                });
            } else {
                cf.complete(null);
            }
            return cf;
        }

        @Override
        public void run() {
            checkSchedule().thenCompose(tf -> {
                int size = scheduled.length; //.size();
                if (idx == 0) {
                    if (shouldSortScheduled) {
                        //sort by scheduled timestamp
                        Arrays.sort(scheduled, cTime); // Collections.sort(scheduled);//Arrays.sort(scheduled);
                    }
                    System.out.println("#of KP scheduled:" + size + " testsUsed:" + testsUsed.size());
                }
                if (idx < scheduled.length) { //.size()
                    EKPscheduled kps = scheduled[idx];//.get(idx);//[0];//.removeFirst();//.get(0);
//                    System.out.println(idx + "-th/" + size + " KP:" + kps.kpid);
                    if (false) { //before test results arrived, we should serve the same test all the time.
                        kps.ltsScheduled = System.currentTimeMillis() + 1000 * 60 * 5; //at least 5 minutes later.
                        shouldSortScheduled = true;
                    }
                    testsT.clear();
                    return kps.filterTests(limit, testsT);
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            }).thenAccept((Boolean tf) -> {
                if (tf) {
//                    System.out.println(idx + "-th KP gives ETests:" + testsT.size());
//                ETest testT = null;
//            ETestResult tr = new ETestResult(); //temp
                    int i0 = testsT.size() - 1;
                    for (int i = i0; i >= 0; i--) {
                        ETest test = testsT.get(i);
//                tr.testid = test.id;
                        if (results.contains(test) //what does this mean? already in the results!
                                || testsUsed.get(test.id) != null) { //is just tested?
                            testsT.remove(test);
                        } else {
//                        testT = test;
                            break;
                        }
                    }
//            if (testT == null) {
//                testT = tests2.get(i0);
//            }
//                return testT;//testT.id;
                    boolean done = false;
                    if (testsT.size() > 0) {
                        results.addAll(testsT);
                        if (results.size() >= n) {
                            onSucceeded();
                            done = true;
                        }
                    }
                    if (done) {
                    } else {
                        idx++;
                        testsT.clear();
                        App.getExecutor().submit(this);
                    }
                } else {
                    onSucceeded();
                }
            }).exceptionally(t -> {
                if (results.isEmpty()) {
                    onFailed(t);
                } else {
                    onSucceeded();
                }
                return null;
            });
        }

        private void onSucceeded() {
            System.out.println("succeeded:" + results.size() + " n:" + n);
            if (n < 1) {
                throw new IllegalStateException();
            }
            running.set(false);
            int i = results.size() - 1;
            for (; i >= n; i--) {
                results.remove(i);
            }
            ETest[] tests = results.toArray(new ETest[0]);
            for (ETest test : tests) { //why do I need this? once I serve them, I do not serve them again immediately.
                ETestResult tr = new ETestResult();
                tr.testid = test.id;
                tr.lts = -1;
                addToUsed(tr);
            }
            for (CompletableFuture<ETest[]> q : request) {
                q.complete(tests);
            }
        }

        private void onFailed(Throwable t) {
            running.set(false);
            for (CompletableFuture<ETest[]> q : request) {
                q.completeExceptionally(t);
            }
        }

    }
}
