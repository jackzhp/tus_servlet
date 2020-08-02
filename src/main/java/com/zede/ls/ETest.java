package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import static com.zede.ls.EKP.merge;
import static com.zede.ls.EKP.merge_1;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.jdo.PersistenceManager;
//import javax.jdo.Transaction;
//import javax.jdo.annotations.Element;
//import javax.jdo.annotations.IdGeneratorStrategy;
//import javax.jdo.annotations.Join;
//import javax.jdo.annotations.PersistenceCapable;
//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.PrimaryKey;
//import javax.jdo.annotations.Unique;

/**
 *
 */
//@PersistenceCapable
public class ETest implements OID {

//    @PrimaryKey
//    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    int id;

    //TODO: how to specify it to be unique?
//    @Persistent
//    @Unique(name = "idx_fn")
    String fnAudio; //for the audio file. not the json file
    String fsha; //sha256
//    @Persistent
    String source; //where the material came from. TODO: make it an object

//    @Persistent
    String info; //TODO: this is language(instruction) dependent. 
    /*
    info should be replaced with info0 and infos.
     */
    String info0; //text in learning language. at present, it is Japanese.
    HashMap<String, String> infos; //indexed by language code, value is the translation of info0.
    //flags: 1: intended to be deleted.  32: it is safe to reuse this id.
    int deleted; //boolean isDeleted; //set at last step. when this is true, this ETest does not have to be saved.
    int idReplacedBy = -1; //I want to implement staged deletion, so I need this.
    /*
    if an ETest does not have any EKP, then 
    itself is just an EKP, i.e. the ETest contains only 1 EKP.
    
    when ETest contains more than 1 EKP, the default one could be modified.
     */
//    @Persistent(table = "tests_kps")
//    @Join(column = "id_test")
//    @Element(column = "id_kp")
//    Set<EKP> kps;
    HashSet<EKP> kps;//ArrayList<EKP> kps; //used to save to json file. TODO: turn this into HashSet
//    int[] kps0; //TODO: when loaded from json file. do I need this? No!

    static int[] idsIdle; //TODO: save them, and load them.
    static int idLast = -2;
    //TODO: should be of WeakReferenc
    private final static ConcurrentHashMap<Integer, WeakReference<ETest>> cached = new ConcurrentHashMap<>();

    public ETest() {
        kps = new HashSet<>();// new ArrayList<>();// new HashSet<>();
    }

    @Override
    public int getID() {
        if (idReplacedBy != -1) {
            return idReplacedBy;
        }
        return id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof ETest) {
                ETest t2 = (ETest) o;
                return this.id == t2.id;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * TODO: unless the caller want to ensure the save, I should delay the save
     * 10 seconds. then the changes to a test entails only 1 save. this could
     * improve performance.
     *
     *
     * @return
     */
    CompletableFuture<Boolean> save_cf() {
        CompletableFuture<Boolean> cf = new CompletableFuture<>();
        try {
            if (false) {
//                PersistenceManager pm = App.getPM();
//                Transaction tx = pm.currentTransaction();
//                try {
//                    tx.begin();
//                    pm.makePersistent(this);
//                    tx.commit();
//                } finally {
//                    if (tx.isActive()) {
//                        tx.rollback();
//                    }
//                    pm.close();
//                }
            } else {
//                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
//        StringWriter sw=new StringWriter();
//            JsonGenerator g = factory.createGenerator(sw);
                this.saveRequested = System.currentTimeMillis();
                save();
            }
            cf.complete(true); //CompletableFuture.completedFuture(true); //TODO: for temp.
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

    EKP[] getKPs() {
        return kps.toArray(new EKP[0]);
    }

    CompletableFuture<EKP[]> getKPs_cf() {
        return null;
    }

//    void onTested(EUser user, long lts, Set<EKP> bads) {
//    void onTested(EUser user, long lts, int[] bads) {
//        for (EKP kp : kps) {
//            boolean good = true; //false;// = bads.contains(kp) == false;
//            for (int bpidBad : bads) {
//                if (kp.id == bpidBad) {
//                    good = false;
//                    break;
//                }
//            }
////            R_User_KP r = null; //find it by user, kp
////            if (r == null) {
////                r = new R_User_KP();
////                r.user = user;
////                r.kp = kp;
////            }
////            r.onTested(lts, this, good);
//            user.onTested(lts, kp, this.id, good);
//        }
//    }
    /**
     *
     * @param kp why not use kpid? since when this ETest is in the memory. the
     * EKP must be in memory. though it might be the one from this ETest, rather
     * than the one from EKPbundle
     * @param user
     * @return
     */
    public CompletableFuture<Boolean> deleteKP(EKP kp, EUser user) {
        //TODO: check user's role.
//        this.highestLevel()
        kps.remove(kp);
        kp.remove(this);
        //the highest level of this test might be changed for some level system.
        //TODO: if the EKP is not used by any ETest, then delete the EKP.
        return save_cf();
    }

    //do I need addKP_cf
    public void addKP(EKP kp, EUser user) {
        //TODO: check user's role.
        kps.add(kp);
        kp.add(this);
        this.updateHighestLevel();
        save(20);
        //TODO: if the EKP is not used by any ETest, then delete the EKP.
    }

    public CompletableFuture<Boolean> addKP_cf(EKP kp, EUser user) {
        //TODO: check user's role.
        addKP(kp, user);
        //TODO: if the EKP is not used by any ETest, then delete the EKP.
        return save_cf();
    }

    @Deprecated
    public CompletableFuture<EKP> newKP_cf(String desc, String grade, EUser user) throws IOException {
        ELevelSystem sys = user.target.sys;
        ELevel level = ELevel.get_m(sys, grade);
        return newKP_cf(desc, level, user);
    }

    //TODO: newKP should not be in this class. in this class add(EKP) is enough.
    public EKP newKP(String desc, ELevel level, EUser user) {
        try {
//TODO: in the future, I should check the KP should not a really new one.
            ELevelSystem sys = user.target.sys;
            EKP kp = new EKP(null);
            kp.newID();
            kp.desc = desc;
            kp.add(this);
            kp.toBeApplied = true;
            kp.hmLevels.put(sys, level);
            kp.getBundle_m();
            return kp;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * after added mistakenly, we can merge EKP's, or delete some EKP's.
     *
     *
     *
     * @param desc
     * @param level
     * @param user
     * @return
     */
    public CompletableFuture<EKP> newKP_cf(String desc, ELevel level, EUser user) {
        CompletableFuture<EKP> cf = new CompletableFuture<>();
        try {
//TODO: in the future, I should check the KP should not a really new one.
            ELevelSystem sys = user.target.sys;
            EKP kp = new EKP(null);
            kp.newID();
            kp.desc = desc;
            kp.add(this);
            kp.toBeApplied = true;
            kp.hmLevels.put(sys, level);
            return kp.getBundle_m().save_cf().
                    //               thenAccept((Boolean tf)->{
                    //       }).
                    //               handle((Boolean tf, Throwable t)->{
                    //           if(t!=null){
                    //               t.printStackTrace();
                    //               return false;
                    //           }else{
                    //               return true;
                    //           }
                    //       }).
                    thenApply((Boolean tf) -> {
                        if (tf) { //we save this side only after the otherside succeeded.
                            //TODO: I can postpone this.
                            kps.add(kp); //Yes. use kps
                            kp.apply(); //TODO: to put it off, or at least check the role of the user.
                            save(100);
                            return true; //return save_cf();
                        } else {
//            CompletableFuture cf=CompletableFuture();
//            cf.completeExceptionally(t);
//            return cf;
                            return false; //CompletableFuture.completedFuture(false);
                        }
                    }).thenCompose(tf -> {
                return save_cf();
            }).thenApply(tf -> {
                return kp;
            });
//        .exceptionally(t -> {
//            t.printStackTrace();
//            return null;
//        });
        } catch (Throwable t) {
            cf.completeExceptionally(t);
            return cf;
        }
    }

    void newID() throws IOException {
        if (idLast == -2) {
            idLast = getIDlast();
        }
        synchronized (EKP.class) {
            if (idsIdle != null) {
                id = idsIdle[0];
                if (idsIdle.length > 1) {
                    int[] ids = new int[idsIdle.length - 1];
                    System.arraycopy(idsIdle, 1, ids, 0, ids.length);
                    idsIdle = ids;
                } else {
                    idsIdle = null;
                }
            } else {
                id = ++idLast;
            }
        }
    }

    static File getFileByID(int id, boolean extnew) {
        String s = Integer.toString(id);
        StringBuilder sb = new StringBuilder();
        for (int n = 5 - s.length(); n >= 0; n--) {
            sb.append('0');
        }
        sb.append(s).append(".json");
        String fn = sb.toString();
        if (extnew) {
            fn += ".new";
        }
        File f = new File(App.dirTests(), fn);
        if (extnew) {
            if (f.exists()) {
                f.delete();
            }
        }
        return f;
    }

    File getFile(boolean extnew) {
        return getFileByID(id, extnew);
    }

    void chgInfo(String info, EUser user) {
        this.info = info;
        this.deleted = 0;
        this.idReplacedBy = -1;
        save(20);
//.exceptionally((Throwable t) -> {
//            t.printStackTrace();
//            return null;
//        });
    }
    HashMap<ELevelSystem, ELevel> levelsHighest = new HashMap<>();

    /* the level change can change up or down, hence we just recalculate it.
    if we know up or down, then we might have shortcut.
    
    we do not save levelsHeight!
     */
    void onLevelChanged(EKP kp, ELevel level) {
        try {
            ELevel hO = levelsHighest.get(level.sys);
            ELevel h = highestLevel_cal(level.sys);
            if (h != hO) {
                if (hO != null) {
                    hO.removeTest(this.id);
                }
                h.addTest(this.id);
            }
            save(30);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    ELevel highestLevel_cal(ELevelSystem sys) {
        ELevel highest = sys.getLevel_m(1, 1); //the lowest one is always 1.1
//        HashSet<EKP> kps=null;
        for (EKP kp : kps) {
            ELevel lt = kp.getLevel(sys);
            if (sys.c.compare(lt, highest) > 0) {
                highest = lt;
            }
        }
        levelsHighest.put(sys, highest);
//        save(15);  //this is not needed since we do not save levelsHighest.
        return highest;
    }

    //this should be called when a new EKP is added to this ETest
    private void updateHighestLevel() {
        Collection<ELevelSystem> c = ELevelSystem.syss.values();
        for (ELevelSystem sys : c) {
            ELevel levelO = levelsHighest.get(sys);
            ELevel level = this.highestLevel_cal(sys);
            if (level != levelO) {
                level.addTest(this.id);
                if (levelO != null) {
                    levelO.removeTest(id);
                }
            }
        }
    }

    ELevel highestLevel(ELevelSystem sys) {
        ELevel level = levelsHighest.get(sys);
        if (level == null) {
            /* if it is null, we can update it, what if it is not null, but need update.
            so this update should be done when any EKP's level is changed.
            then this make up is not needed.
            
            we always use this make up since we do not save levelsHighest.
             */
            level = highestLevel_cal(sys);
        }
        return level;
    }

    static ETest getByID(int id) {
        ETest test = null;
        WeakReference<ETest> wr = cached.get(id);
        if (wr != null) {
            test = wr.get();
        }
        return test;
    }

    static ETest loadByID_m(int id) {
        try {
            ETest test = getByID(id);// cached.get(id);
            if (test == null) {
                File f = getFileByID(id, false);
                if (f.exists()) {
                    test = new ETest();
                    test.id = id;
                    WeakReference<ETest> wr = new WeakReference<>(test);
                    boolean shouldLoad = true;
                    WeakReference<ETest> wrO = cached.putIfAbsent(id, wr);
                    if (wrO != null) {
                        ETest testO = wrO.get();
                        if (testO != null) {
                            test = testO;
                            shouldLoad = false;
                        } else {
                            cached.put(id, wr); ////retry putIfAbsent
                            //shouldLoad=true;
                        }
                    } else {
                        //shouldLoad=true;
                    }
                    if (shouldLoad) {
                        test.load(f);
                    }
                }
            }
            return test;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException(t);
        }
    }

    static CompletableFuture<ETest> getByID_cf(int id) {
        CompletableFuture<ETest> cf = new CompletableFuture<ETest>();
        try {
            cf.complete(loadByID_m(id));
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

    void load(File f) throws IOException {
//        System.out.println("load ETest from " + f.getAbsolutePath());
//        if (id == 0) {
//            source = "";
//            source = null;
//        }
        JsonParser p = App.getJSONparser(f);
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    t = p.nextToken();
                    if ("kps".equals(name)) {
                        if (false) { //use array
                            if (t == JsonToken.START_ARRAY) {
                                EKP kp = new EKP(null);
                                kp.parse(p);
                                kp.setRedundant();// = true;
                                EKP.cache(kp);
                            } else {
                                throw new IllegalStateException("expecting start object, but " + t);
                            }
                        } else { //use object
                            EKPbundle.parse(p, kps, null);
                        }

                    } else if ("id".equals(name)) {
                        id = p.getValueAsInt();
                    } else if ("deleted".equals(name)) {
                        this.deleted = p.getValueAsInt();// p.getValueAsBoolean();
                    } else if ("idReplaced".equals(name)) {
                        this.idReplacedBy = p.getValueAsInt();// p.getValueAsBoolean();
                    } else {
                        String value = p.getValueAsString();
                        if ("fnAudio".equals(name)) {
                            this.fnAudio = value;
                        } else if ("fsha".equals(name)) {
                            this.fsha = value;
                        } else if ("info".equals(name)) {
                            this.info = value;
                        } else if ("source".equals(name)) {
                            this.source = value;
                        } else {
                            throw new IllegalStateException("unexpected name:" + name);
                        }
                    }
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                } else {
                    throw new IllegalStateException("expecting end object, but " + t);
                }
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
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
                    if (id == 0) {
                        source = "";
                        source = null;
                    }
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

    /**
     *
     *
     *
     * @param sha
     * @return
     */
    static ETest getByFileAudio(String sha) {
        try {
            File dir = App.dirTests();
            File[] af = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });
            byte[] target = (",\"sha\":\"" + sha + "\",").getBytes("utf8"); //TODO: am I sure no space after/before ':'?
            byte[] bytes = new byte[4096];
            for (File f : af) {
                try {
                    if (App.contains(f, target, bytes)) {
                        String fn = f.getName();
                        String[] as = fn.split("\\.");
                        int id = Integer.parseInt(as[0]);
                        return getByID(id);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    static int getIDlast() {
        try {
            File dir = App.dirTests();
            String[] af = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".json");
                }
            });
            Arrays.sort(af);
            String fn = af[af.length - 1];
            String[] as = fn.split("\\.");
            System.out.println(fn + " -> " + as[0]);
            return Integer.parseInt(as[0]);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return -1;
    }

    /**
     */
    static void EKP_none(HashSet<ETest> tests) {
//        ELevel levelRepair = ELevelSystem.getByName("misc").getLevel_m(1, 1);
        Function<ETest, Boolean> f = new FunctionEKPNone(tests); //(test)->test.kps.isEmpty(); //
        filter(f); //, halves
    }

    @SuppressWarnings("unchecked") //HashSet<ETest> halvesTest,
    static void EKP_half(HashSet<EKP> halvesKP) { //, int repair
//        HashSet<OID> halves;
//        if (repair == App.FixHalf_Self) {
//            halves = (HashSet) halvesTest;
//        } else if (repair == App.FixHalf_Reciprocol) { //must be this one
//            halves = (HashSet) halvesKP;
//        } else {
//            throw new IllegalStateException("unknown repair:" + repair);
//        }
        Function<ETest, Boolean> f = new FunctionEKPHalf(halvesKP); //repair, 
        filter(f); //, halves
    }

    static int filter(Function<ETest, Boolean> f) { //, HashSet<ETest> halves
        File dir = App.dirTests();
        String[] af = dir.list(App.ff_json);
        int fixed = 0;
//        ArrayList<ETest> halves = new ArrayList<>(); //TODO: rename it.
        for (String fn : af) {
            String[] as = fn.split("\\.");
            int id = Integer.parseInt(as[0]);
            ETest test = loadByID_m(id);
            if (test.deleted != 0) {
                continue;
            }
//            if (f.apply(test)) {
//                halves.add(test);
//            }
            if (f.apply(test)) {
                fixed++;
            }
        }
        return fixed;//        return tests;
    }

    boolean withEKP() {
//        int[] aid = kps0;
//        if (aid != null) {
//            return aid.length > 0;
//        }
//        ArrayList<EKP> al = kps;
////        if (al != null) {
//        return al.size() > 0;
////        }
////        return false;
        return kps.size() > 0;
    }

    boolean contains(EKP kp) {
        //the list of kps will not be long, so I do not use HashSet
        for (EKP kpt : kps) {
            if (kpt.id == kp.id) { //kpt.equals(kp)
                return true;
            }
        }
        return false;
    }

    void add(EKP kp) {
        kps.add(kp);
    }

    /*
    this method should not be used since I do not save levelsHighest.
    so all with none, and the highest level should be calculated.
     */
    static void ELevel_none(ELevelSystem sys, HashSet<ETest> halves) {
        //, halves
        Function<ETest, Boolean> f = new FunctionELevelNone(sys, true);
//        return 
        filter(f);
    }

    static int //HashSet<ETest>
            ELevel_half(ELevelSystem sys, //int repair, HashSet<ETest> halvesTest,
                    HashSet<ELevel> halvesLevel) {
        //, halves
//        HashSet<OID> halves;
//        if (repair == App.FixHalf_Self) {
//            halves = (HashSet) halvesTest;
//        } else if (repair == App.FixHalf_Reciprocol) {
//            halves = (HashSet) halvesLevel;
//        } else {
//            throw new IllegalStateException("unknown repair:" + repair);
//        }
        Function<ETest, Boolean> f = new FunctionELevelHalf(sys, halvesLevel);//repair, halves);
//        return
        return filter(f);
    }

    /**
     *
     * delete this ETest.
     *
     * do I want it to be recoverable? deleted by whom, and when?
     *
     */
    void delete() {
        //no EKP should not refer to this
        //all ELevel should not refer to this

        //at last step.
        //this.isDeleted = true;
    }

    static CompletableFuture<Boolean> merge(String testids) throws IOException {
        if (testids == null) {
            throw new IllegalArgumentException();
        }
        System.out.println("ETest to be merged:" + testids);
        int[] atestid = App.getInts(testids);
        return merge(atestid);
    }

    static CompletableFuture<Boolean> merge(int[] atestid) throws IOException {
        try {
            if (atestid.length <= 1) {
                throw new IllegalArgumentException("at least 2 ids to merge");
            }
            int testid = atestid[0];
            ETest testT = ETest.loadByID_m(testid);
            if (testT == null) {
                throw new Exception("can not load ETest#" + testid);
            }
//        if (true) {
//            for (int i = 1; i < akpid.length; i++) {
//                int kpidt = akpid[i];
//                EKP kpt = EKP.getByID_m(kpidt);
//                kp.merge(kpt);
//            }
            for (int i = 1; i < atestid.length; i++) {
                try {
                    testid = atestid[i];
                    if (testid == -1) {
                        continue;
                    }
                    ETest test = ETest.loadByID_m(testid);
                    testT.merge(test);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return testT.save_cf();
//        }
//        else 
//        {
//            EKP.Merger<ETest> mergerTest = new EKP.MergerETest(kp);
//            CompletableFuture<Void> cf = merge_1(atestid, mergerTest);
//            return cf.thenCompose(v -> {
//                EKP.Merger<ELevelSystem> mergerLevel = new EKP.MergerELevel(kp);
//                return merge_1(atestid, mergerLevel);
//            }).thenCompose(v -> {
//                EKP.Merger<EUser> mergerUser = new EKP.MergerEUser(kp);
//                return merge_1(atestid, mergerUser);
//            }).thenCompose(v -> {
//                ETest test = ETest.loadByID_m(testid);
//                for (int i = 1; i < atestid.length; i++) {
//                    try {
//                        int kpidt = atestid[i];
//                        if (kpidt == -1) {
//                            continue;
//                        }
//                        EKP o = EKP.getByID_m(kpidt, true);
//                        test.kps.remove(o);
//                        if (o.delete()) {
//                        } else {
//                            throw new IllegalStateException("EKP#" + kpidt + " is still being used");
//                        }
//                    } catch (Throwable t) {
//                        t.printStackTrace();
//                    }
//                }
//                return test.save_cf();
//            });
//        }
        } catch (Throwable t) {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            cf.completeExceptionally(t);
            return cf;
        }
    }

    /**
     * TODO: merge test into this Object.
     *
     * whatever referred by test, should be referred by this ETest. EKP
     *
     * whatever is referring to test, should refer to this ETest. EKP, ELevel
     *
     *
     * no rush to active this method.
     *
     * @param test
     */
    void merge(ETest test) {
        if (this.fnAudio == null) {
            this.fnAudio = test.fnAudio;
        } else {
            if (this.fnAudio.equals(test.fnAudio)) {
            } else { //TODO: can be disabled
                throw new IllegalStateException(this.fnAudio + ":" + test.fnAudio);
            }
        }
        if (this.info.equals(test.info)) {
        } else { //TODO: keep the longer one.  I do not need this. I have chosen the target for the merge.
            int len1 = this.info.length();
            int len2 = test.info.length();
            if (len2 > len1) {
                this.info = test.info;
            }
        }
        test.idReplacedBy = this.id;
        test.deleted = 1; //isDeleted = true;
        ELevelSystem[] asys = ELevelSystem.syss.values().toArray(new ELevelSystem[0]);
        for (ELevelSystem sys : asys) {
            ELevel level = test.highestLevel(sys);
            level.removeTest(test.id);
            level = this.highestLevel(sys);
            level.removeTest(this.id); //we remoe them first, later add them back
        }
        kps.addAll(test.kps);
        for (ELevelSystem sys : asys) {
            ELevel level = this.highestLevel(sys);
            level.addTest(this.id); //now add them back.
        }
        EKP[] akp = test.kps.toArray(new EKP[0]);
        for (EKP kp : akp) {
            kp.replace(test, this);
        }
        test.kps.clear();
        this.save(30);
        test.save(29);
    }

    static void distinctAudioFile() {
        HashMap<String, ETest> tests = new HashMap<>();
        File dir = App.dirTests();
        String[] af = dir.list(App.ff_json);
        int merged = 0;
        for (String fn : af) {
            String[] as = fn.split("\\.");
            int id = Integer.parseInt(as[0]);
            ETest test = loadByID_m(id);
//            if (test.deleted > 0) {
//                continue;
//            }
            if (test.fnAudio == null) {
                System.out.println("fnAudio is null for " + test.id + " " + fn);
                continue;
            }
            ETest testO = tests.get(test.fnAudio);
            if (test.idReplacedBy != -1) {
                boolean shouldSet = true;
                if (testO == null) {
                } else {
                    if (testO.id == test.idReplacedBy) {
                        shouldSet = false;
                    } else {
                    }
                }
                if (shouldSet) {
                    testO = loadByID_m(test.idReplacedBy);
                    tests.put(test.fnAudio, testO);
                }
            } else {
                if (testO != null) {
                } else {
                    testO = test;
                    tests.put(test.fnAudio, test);
                }
            }
            if (test == testO) {
            } else {
                System.out.println("will merge:" + test.fnAudio);
                if (testO.deleted > 0) {
                    throw new IllegalStateException();
                }
                testO.merge(test);
                merged++;
            }
        }
        System.out.println("merged:" + merged);
    }

    /**
     * to merge remove into keep.
     *
     * this is not the memory object change.
     *
     * @param remove
     * @param keep
     */
    void replace(EKP remove, EKP keep) {
        if (remove == keep) {
            throw new IllegalArgumentException();
        }
        kps.remove(remove);
        kps.add(keep);
        save(20);
    }

    void json(JsonGenerator g) throws IOException {
        g.writeStartObject();
        g.writeNumberField("id", id);
        g.writeNumberField("deleted", deleted); //g.writeBooleanField("deleted", isDeleted);
        g.writeNumberField("idReplaced", this.idReplacedBy); //g.writeBooleanField("deleted", isDeleted);
        g.writeStringField("fnAudio", fnAudio);
        g.writeStringField("fsha", fsha);
        g.writeStringField("info", info); //the key word of the record.
        g.writeStringField("source", source); //the key word of the record.
//        g.writeNumberField("ireason", 5);
//generator.writeStringField("brand", "Mercedes");
        if (false) {
            g.writeArrayFieldStart("kps");
            for (EKP kp : kps) {
                //Be noted: EKP does saved here, but this is only for convenience, EKP's storage is the one reliable.
                kp.json(g);
            }
            g.writeEndArray();
        } else {
            g.writeObjectFieldStart("kps"); //g.writeArrayFieldStart("kps");
            EKPbundle.json(g, kps);
            g.writeEndObject(); //g.writeEndArray();
        }
        g.writeEndObject();
    }

    static void search(App.ConditionSearch cs, HashSet<ETest> tests) {
        File dir = App.dirTests();
        String[] af = dir.list(App.ff_json);
        for (String fn : af) {
            String[] as = fn.split("\\.");
            int id = Integer.parseInt(as[0]);
            ETest test = loadByID_m(id);
            if (cs.apply(test.info)) {
                tests.add(test);
            }
        }
    }

    /* why did I got result: {"category":"search","tests":[136,341,341,136]}. Be noted I am using HashSet.                    
    because even though test.id, is different, but test.getID() are same.
     */
    static HashSet<ETest> distinct(HashSet<ETest> tests) {
        HashSet<ETest> results = new HashSet<>();
        ETest[] a = tests.toArray(new ETest[0]);
        tests.clear();
        for (ETest test : a) {
            int id = test.getID();
//            set.add(id); //test.id might be different, but test.getID() might be same
            results.add(ETest.loadByID_m(id));
        }
        return results;
    }

    //TODO: this is not needed at all.
    static class FunctionELevelNone implements Function<ETest, Boolean> {

        private final ELevelSystem sys;
        private final boolean repair;

        FunctionELevelNone(ELevelSystem sys, boolean repair) {
            this.sys = sys;
            this.repair = repair;
        }

        @Override
        public Boolean apply(ETest test) {
            ELevel level = test.levelsHighest.get(sys);
            if (level == null) {
                if (repair) {
                    test.highestLevel_cal(sys);
                }
                return true;
            }
            return false;
        }

    }

    static class FunctionELevelHalf implements Function<ETest, Boolean> {

        private final ELevelSystem sys;
        private int repair = App.FixHalf_Reciprocol;
        HashSet<ELevel> halves;

        FunctionELevelHalf(ELevelSystem sys, //int repair,
                HashSet<ELevel> halves) {
            this.sys = sys;
//            if (repair != 0) {
//                this.repair = repair;
//            }
            this.halves = halves;
        }

        @Override
        public Boolean apply(ETest test) {
            if (test.id == 1) {
                test.id = 1;
            }
            ELevel level = test.highestLevel_cal(sys); //.levelsHighest.get(sys);
            if (level != null) {
                if (level.tests.contains(test.id)) {
                    return false;
                } else {
                    if (repair == App.FixHalf_Reciprocol) {
                        level.addTest(test.id);
                        halves.add(level);
                    } else {
                        throw new IllegalStateException("repair should be fixing ELevel");
                    }
                    return true;
                }
            } else {
                return false;
            }
        }

    }

    static class FunctionEKPNone implements Function<ETest, Boolean> {

//        ELevel level;        //        private boolean repair;
        HashSet<ETest> tests;

        FunctionEKPNone(HashSet<ETest> tests) { //boolean repair,ELevel level
//            this.level = level;;//this.repair = repair;
            this.tests = tests;
        }

        @Override
        public Boolean apply(ETest test) {
            if (test.kps.isEmpty()) {
//                if (level != null) { //do repair. use ETest.info as EKP.desc
//                    test.newKP(test.info, level, null);
//                }
                tests.add(test);
                return true;
            }
            return false;
        }

    }

    static class FunctionEKPHalf implements Function<ETest, Boolean> {

        private int repair = App.FixHalf_Reciprocol; //change EKP, do not change ETest
        HashSet<EKP> changed;// = new HashSet<>();

        FunctionEKPHalf(//int repair,
                HashSet<EKP> changed) {
//            if (repair != 0) {
//                this.repair = repair;
//            }
            this.changed = changed;
        }

        @Override
        public Boolean apply(ETest test) {
            if (test.kps.isEmpty()) {
                return false;
            }
            int nhalf = 0, nchanged = 0;
            EKP[] a = test.kps.toArray(new EKP[0]);
            for (EKP kp : a) {
                if (kp.withETest(test)) {
                } else {
                    nhalf++;
                    if (repair == App.FixHalf_Reciprocol) {
                        nchanged++;
                        try {
                            //default
                            kp = EKP.getByID_m(kp.id, true);
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                        kp.add(test);
                        kp.save(20);
                        changed.add(kp);
                    } else {
                        throw new IllegalStateException();
//                        test.kps.remove(kp);
//                        test.save(20);
//                        changed.add(test);
                    }
                }
            }
            if (nchanged > 0) {
                test.save(20);
            }
            return nhalf > 0;
        }

    }
}
