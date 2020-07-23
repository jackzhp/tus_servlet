package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
//import static com.zede.ls.ETest.getByID_m;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
//import javax.jdo.annotations.IdGeneratorStrategy;
//import javax.jdo.annotations.PersistenceCapable;
//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.PrimaryKey;

/**
 *
 * KnowledgePoint
 *
 * 100 EKP's saved in one file. each file contains 100EKP. why? suppose every
 * EKP takes only 40 byes, I can not allow 40 bytes to take a block on the disk.
 * though blocksize+1 bytes is allowed to take 2 blocks. usually blocksize is
 * 4096.
 *
 *
 *
 */
//@PersistenceCapable
public class EKP implements Comparable<EKP> {

//    @PrimaryKey
//    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    int id;
    /* this EKP is merged with another EKP. this EKP should be deleted.
    -1 means this EKP has not been replaced by any other EKP.
every referring to EKP, when do saving, should check this one.
if this is not -1, then this variable should replace this.id.    
     */
    int replacedBy = -1;
    boolean isDeleted, //the intention to be deleted, but not deleted only because other objects still referring to this EKP.
            isIdle;//=true;  when an EKP is deleted, or merged with another one, it becomes idle.
    /* the ones load from ETest should set this to true.
    any modification should be make to the object with this==false.
     */
    boolean isRedundant;

    /*
    TODO: this should be searchable with full text.
    kodo has this capability: 
    Example of full-text searching in JDO
The files for this sample are located in the samples/textindex directory of the Kodo installation. This sample demonstrates how full-text indexing might be implemented in JDO. Most relational databases cannot optimize contains queries for large text fields, meaning that any substring query will result in a full table scan (which can be extremely slow for tables with many rows).

The AbstractIndexable class implements javax.jdo.InstanceCallbacks which will cause the textual content of the implementing persistent class to be split into individual "word" tokens, and stored in a related table. Since this happens whenever an instance of the class is stored, the index is always up to date. The Indexer class is a utility that assists in building queries that act on the indexed field.

The TextIndexMain class is a driver to demonstrate a simple text indexing application.
     */
//    @Persistent
    String desc;

//    @Persistent //TODO: this should be a map. different sets of material.
//    ELevel level; //this depends on ELevelSystem.
    HashMap<ELevelSystem, ELevel> hmLevels = new HashMap<>();

//    @Persistent(mappedBy = "kps")
//    Set<ETest> tests;
    HashSet<ETest> tests; //ETest[] tests; //when this is not null, this should be used. use tests0 only if this is null.
    ArrayList<Integer> tests0 = new ArrayList<>(); //ETest
    /* why do I need this?
    we want to avoid other objects referring to a notsaved EKP,
    or this EKP might not be saved, and then another EKP is using the id of this EKP.
    then other objects will refer to the wrong EKP.
    so we allow other objects to refer to this EKP only after this EKP has been saved.
     */
    boolean toBeApplied;

    EKPbundle bundle;

    static int[] idsIdle; //TODO: save them, and load them.
    static int idLast = -2;
    //cache is not really needed, unless we are making modification to EKP, otherwise, it does not have to be in memory at all.
//    static LinkedHashMap<Integer,WeakReference<EKP>
//            LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder)    
    private final static ConcurrentHashMap<Integer, WeakReference<EKP>> cached = new ConcurrentHashMap<>();

    public EKP(EKPbundle b) {
        this.bundle = b;
//        tests = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof EKP) {
                EKP kp2 = (EKP) o;
                return this.id == kp2.id;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    //TODO: ....
    CompletableFuture<Boolean> init_s() {
        //idsIdle
//        idLast
        return null;
    }

    void save(long delay) { //this method is same for all 
        getBundle_m().save(delay);
    }
//    //TODO: move these variables to EKPbundle
//    long saveRequested, saveLast, saveNext; //the next means the next saving must be after that.
//    boolean nextScheduled;
//    AtomicBoolean saving = new AtomicBoolean();
//    private static long saveDelayDefault = 30; //seconds
//
//    void save(long delay) { //this method is same for all 
//        long ltsnow = System.currentTimeMillis();
//        saveRequested = ltsnow;
//        if (nextScheduled) {
//        } else {
//            long delay2 = saveNext - ltsnow;
//            delay2 /= 1000; //now it is seconds
//            delay2++;
//            if (delay < delay2) {
//                delay = delay2;
//            }
//            App.getExecutor().schedule(() -> {
//                try {
//                    nextScheduled = false;
//                    save();
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }
//            }, delay, TimeUnit.SECONDS);
//            nextScheduled = true;
//        }
//    }
//
//    CompletableFuture<Boolean> save_cf() {
//        CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
//        try {
//            save();
//            cf.complete(true);
//        } catch (Throwable t) {
//            cf.completeExceptionally(t);
//        }
//        return cf;
//    }
//
//    // save for this class of objects can be immediately.
//    void save() throws IOException, InterruptedException, ExecutionException {
//        if (saving.compareAndSet(false, true)) {
//            try {
//                if (saveRequested >= saveLast) {
//                    saveLast = System.currentTimeMillis();
//                    saveNext = saveLast + saveDelayDefault;
//                    EKPbundle b = getBundle_m();
//                    b.save();
//                    System.out.println("EKP saved:" + id);
//                }
//            } finally {
//                saving.set(false);
//            }
//        }
//    }

    void newID() throws IOException {
        if (idLast == -2) {
            idLast = EKPbundle.getIDlast();
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
        cache(this);
        this.bundle = EKPbundle.getByID_m(id);
        this.bundle.set(this);
    }

    void in(ETest test) {
//        tests.add(test);
        tests0.add(test.id);
    }

    void json(JsonGenerator g) throws IOException {
        g.writeStartObject();
        //many other fields.
        g.writeNumberField("id", id);
        g.writeNumberField("replacedBy", replacedBy);
        g.writeBooleanField("idle", isIdle);
        g.writeBooleanField("deleted", isDeleted);
        g.writeStringField("desc", desc);
        g.writeArrayFieldStart("tests");
        if (false) {
//            for (ETest test : tests) {
//                g.writeNumber(test.id);
//            }
        } else {
            Integer[] ai;
            ArrayList<Integer> t0 = tests0;
            if (t0 != null) {
                ai = t0.toArray(new Integer[0]);
            } else {
                ai = new Integer[tests.size()]; //tests.length
                int i = 0;
                for (ETest test : tests) {
                    ai[i++] = test.id;
                }
            }
            Arrays.sort(ai);
            Integer iLast = null;
//            tests0.clear();
            for (Integer i : ai) {
                if (i.equals(iLast)) {
                    continue;
                }
                g.writeNumber(i.intValue());
                iLast = i;
//                tests0.add(i);
            }
        }
        g.writeEndArray();
        g.writeFieldName("levels");
        jsonLevels(g);
        g.writeEndObject();
    }

    void parse(JsonParser p) throws IOException { //TODO: rename it to parse
        JsonToken t = p.currentToken();//.nextToken();
        String sreason = null;
        if (t != JsonToken.START_OBJECT) {
            sreason = "expecting start object, but " + t;
        } else {
            nextname:
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    t = p.nextToken();
                    if ("id".equals(name)) {
                        if (t != JsonToken.VALUE_NUMBER_INT) {
                            sreason = "expecting int value, but " + t;
                        } else {
                            int idt = (int) p.getValueAsLong();
                            if (idt != id) {
                                throw new IllegalStateException(id + ":" + idt);
                            }
                        }
                    } else if ("replacedBy".equals(name)) {
                        if (t != JsonToken.VALUE_NUMBER_INT) {
                            sreason = "expecting int value, but " + t;
                        } else {
                            replacedBy = (int) p.getValueAsLong();
                        }
                    } else if ("desc".equals(name)) {
                        if (t == JsonToken.VALUE_STRING) {
                            desc = p.getValueAsString();
                        } else if (t == JsonToken.VALUE_NULL) {
                        } else {
                            sreason = "expecting string value, but " + t;
                        }
                    } else if ("idle".equals(name)) {
                        isIdle = p.getBooleanValue();
//                        if (t == JsonToken.VALUE_TRUE) {
//                            isIdle = true;
//                        } else if (t == JsonToken.VALUE_FALSE) {
//                            isIdle = false;
//                        } else {
//                            sreason = "expecting boolean value, but " + t;
//                        }
                    } else if ("deleted".equals(name)) {
                        isDeleted = p.getBooleanValue();
                    } else if ("tests".equals(name)) {
                        if (t != JsonToken.START_ARRAY) {
                            sreason = "expecting array value, but " + t;
                        } else {
//                            tests0 = new ArrayList<>();
//                            int idlast = -1;
                            while (true) {
                                t = p.nextToken();
                                if (t == JsonToken.VALUE_NUMBER_INT) {
                                    int idtest = p.getValueAsInt();
//                                    if (idtest != idlast) { //when doing saving, they are sorted.
                                    tests0.add(idtest);
//                                        idlast = idtest;
//                                    } else {
////just ignore.
//                                    }
                                } else if (t == JsonToken.END_ARRAY) {
                                    break;
                                } else {
                                    sreason = "expecting int value in array, but " + t;
                                    break nextname;
                                }
                            }
                        }
                    } else if ("levels".equals(name)) {
                        parseLevels(p);
                    } else {
                        sreason = "unknown field name:" + name;
                    }
                } else {
                    if (t != JsonToken.END_OBJECT) {
                        sreason = "expecting field name,but " + t;
                    }
                    break;
                }
            }
        }
        if (sreason != null) {
            throw new IllegalStateException(sreason);
        }
    }

    @Override
    public int compareTo(EKP o) { //move null backward.
        return id - o.id;
    }

    EKPbundle getBundle_m() {
        try {
            if (bundle == null) {
                bundle = EKPbundle.getByID_m(id);
            }
            return bundle;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    static EKP getByID_m(int id) throws IOException {
        return getByID_m(id, false);
    }

    /**
     *
     *
     * @param id
     * @param change the caller wants to change this EKP, so this object should
     * not be the one loaded from ETest. instead, should be loaded with
     * EKPbundle.
     * @return
     * @throws IOException
     */
    static EKP getByID_m(int id, boolean change) throws IOException {
        EKP kp = null;
        WeakReference<EKP> wr = cached.get(id);
        if (wr != null) {
            kp = wr.get();
            if (change) {
                if (kp.isRedundant) {
                    kp = null;
                } else {
                    if (kp.bundle == null) {
                        throw new IllegalStateException("should not happen");
                    }
                }
            }
        }
        if (kp == null) {
            EKPbundle b = EKPbundle.getByID_m(id);
            wr = cached.get(id);
            if (wr == null) { //happened.
                throw new IllegalStateException("this should not happen:" + id);
            }
            kp = wr.get();
        }
        return kp;
    }

    static CompletableFuture<EKP> getByID_cf(int id) {
        return getByID_cf(id, false);
//        CompletableFuture<EKP> cf = new CompletableFuture<EKP>();
//        try {
//            cf.complete(getByID_m(id));
//        } catch (Throwable t) {
//            cf.completeExceptionally(t);
//        }
//        EKP kp = null;
//        WeakReference<EKP> wr = cached.get(id);
//        if (wr != null) {
//            kp = wr.get();
//        }
//        if (kp == null) {
//            return EKPbundle.getByID_cf(id).thenApply((EKPbundle b) -> {
////                return b.getEKPbyID(id);
//                WeakReference<EKP> wr2 = cached.get(id);
//                if (wr2 == null) { //this happened.
//                    throw new IllegalStateException("this should not happen");
//                }
//                return wr2.get();
//            });
//        }
//        return CompletableFuture.completedFuture(kp);
//        return cf;
    }

    static CompletableFuture<EKP> getByID_cf(int id, boolean change) {
        CompletableFuture<EKP> cf = new CompletableFuture<EKP>();
        try {
            cf.complete(getByID_m(id, change));
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
//        EKP kp = null;
//        WeakReference<EKP> wr = cached.get(id);
//        if (wr != null) {
//            kp = wr.get();
//        }
//        if (kp == null) {
//            return EKPbundle.getByID_cf(id).thenApply((EKPbundle b) -> {
////                return b.getEKPbyID(id);
//                WeakReference<EKP> wr2 = cached.get(id);
//                if (wr2 == null) { //this happened.
//                    throw new IllegalStateException("this should not happen");
//                }
//                return wr2.get();
//            });
//        }
//        return CompletableFuture.completedFuture(kp);
        return cf;
    }

    /**
     * cache in this class is needed, since not EKP in its bundle is needed in
     * the memory.
     *
     * the bundle does not have to keep all its EKP's unless modification is
     * intended.
     *
     *
     * @param kp
     */
    static void cache(EKP kp) {
        WeakReference<EKP> wr = new WeakReference<>(kp);
        WeakReference<EKP> wrO = cached.putIfAbsent(kp.id, wr);
        if (wrO != null) {
            EKP kpO = wrO.get();
            if (kpO != null) {
                if (kpO.isRedundant && kp.isRedundant == false) {
                    cached.put(kp.id, wr);
                    App.getExecutor().submit(() -> {
                        //now, we have 2 objects for the same EKP. the one with isRedundant==true, and the other one ==false.
                        kpO.replaceWith(kp);
                    });
                }
            } else {
                cached.put(kp.id, wr); //retry putIfAbsent
            }
        } else {
        }
    }

    private void replaceWith(EKP kp) {
        try {
            //TODO: anyone who refer to this object, now should refer to kp.
            //in fact, at present, only ETest
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    //this one just check memory, so might return null.
    static EKP getByID(int id) {
        EKP kp = null;
        if (false) {
//            EKPbundle b = EKPbundle.getByID(id);
//            if (b != null) {
//                kp = b.getEKPbyID(id);
//            }
        } else {
            WeakReference<EKP> wr = cached.get(id);
            if (wr != null) {
                kp = wr.get();
            }
            if (kp == null) {
                wr = cached.remove(id);
                if (wr != null) {
                    kp = wr.get();
                    if (kp != null) { //because of concurrency, it might be not null at this time point.
                        cached.put(id, wr);
                    }
                }
            }
        }
        return kp;
    }

    CompletableFuture<Void> chgDesc_cf(String desc, EUser user) {
        //TODO: I better put it in other places
        //   merge then after review
        //but for now I just change directly
        this.desc = desc;
        //for those ETest referring to this EKP, I should notify them to change.
        return this.bundle.save_cf().thenCompose(tf -> {
            ETest[] atest = getTests();
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean>[] acf = new CompletableFuture[atest.length];
            for (int i = 0; i < atest.length; i++) {
                ETest test = atest[i];
                acf[i] = test.save_cf();
            }
            return CompletableFuture.allOf(acf);
        });
    }

    ETest[] getTests() {
        if (tests == null) {
            boolean shouldSave = false;
//            synchronized (this) { //the loading sequences ensure when we get here, tests0 is ready.
            HashSet<ETest> al = new HashSet<>();
            while (true) {
                Integer[] atestid = tests0.toArray(new Integer[0]);
                for (int i = 0; i < atestid.length; i++) {
                    ETest test = ETest.loadByID_m(atestid[i]);
                    if (test == null) {
                        shouldSave = true;
//                        tests0.remove(atestid[i]); //not needed, it will be set with null.
                    } else {
                        al.add(test);
                    }
                }
                tests = al;// al.toArray(new ETest[0]);
                if (atestid.length == tests0.size()) {
                    tests0 = null;
                    break;
                }
            }
//            }
            if (shouldSave) {
                save(10);
            }
        }
        return tests.toArray(new ETest[0]);
    }

    ELevel getLevel(ELevelSystem sys) {
        ELevel level = hmLevels.get(sys);
        if (level == null) {
            level = sys.getLevel_m(1, 1); //1.1 is always the lowest level
            hmLevels.put(sys, level);
        }
        return level;
    }

    /**
     * not committed, to commit, save the EKPbundle! I do not return a
     * CompletableFuture in order to achieve the flexibility.
     *
     * the caller should take care of the commitment if immediate save is
     * needed.
     *
     */
    void set(ELevel level) {
        ELevel lO = hmLevels.get(level.sys);
        if (lO != null) {
            if (lO.idMajor == level.idMajor && lO.idMinor == level.idMinor) {
                return;
            }
            lO.kps.remove(id);
        }
        hmLevels.put(level.sys, level);
        save(10); //by default, will save in 10 seconds
        App.executor.schedule(() -> {
            level.kps.add(id);
            ETest[] tests = getTests();
            for (ETest test : tests) {
                test.onLevelChanged(this, level);
            }
        }, 11, TimeUnit.SECONDS);
    }

    /**
     * another way, before applied, I can just save raw data, rather than parse
     * it.
     *
     *
     */
    void apply() {
//        Set<Map.Entry<ELevelSystem, ELevel>> s=hmLevels.entrySet();
//        for(Map.Entry<ELevelSystem, ELevel> me:s){
//            
//        }
        Collection<ELevel> c = hmLevels.values();
        for (ELevel level : c) {
            level.kps.add(id);
        }
        this.toBeApplied = false;
    }

    /**
     *
     * there is no point to return this list of EKP's without ETest.!
     */
    static ArrayList<EKP> halfETest() throws IOException {
        Function<EKP, Boolean> f = new FunctionHalfETest(true);
        return filter(f);
    }

    /**
     *
     * there is no point to return this list of EKP's without ETest.!
     */
    static ArrayList<EKP> noETest() throws IOException {
        Function<EKP, Boolean> f = (EKP kp) -> kp.withETest() == false;
        return filter(f);
    }

    /*
    this EKP does refer to ELevel, but which does not refer to this EKP.
     */
    static ArrayList<EKP> halfELevel(ELevelSystem sys) throws IOException { //TODO: ....
        Function<EKP, Boolean> f = new FunctionHalfELevel(sys, true);
        return filter(f);
    }

    static ArrayList<EKP> noELevel(ELevelSystem sys) throws IOException { //TODO: ....
        Function<EKP, Boolean> f = new FunctionNoELevel(sys);
        return filter(f);
    }

    /**
     * given a level system, for each level, how many EKPs?
     *
     *
     * @param sys
     * @return
     * @throws IOException
     */
    static HashMap<ELevel, Integer> levels(ELevelSystem sys) throws IOException {
        File dir = App.dirKPs();
        String[] af = dir.list(App.ff_json);
        System.out.println("total # of KPbundles:" + af.length); //not EKP, but EKPbundle
        HashMap<ELevel, Integer> histogram = new HashMap<>();
        ELevel level0 = new ELevel(sys);
        for (String fn : af) {
            String[] as = fn.split("\\.");
            int id = Integer.parseInt(as[0]);
            id *= EKPbundle.bundleSize;
//                EKP kp = getByID_m(id);
            EKPbundle b = EKPbundle.getByID_m(id);
            for (EKP kp : b.kps) {
                ELevel level = kp.getLevel(sys);
                if (level == null) {
                    level = level0;
                }
                Integer n = histogram.get(level);
                if (n != null) {
                    n = Integer.valueOf(n + 1);
                } else {
                    n = Integer.valueOf(1);
                }
                histogram.put(level, n);
            }
        }
        return histogram;
    }

    /**
     * merge kp into this object.
     *
     * @param kp
     */
    void merge(EKP kp) {
        //all referring to kp, now should referring to this EKP.
        //who are referring to the kp? ELevel, ETest, EUser.
        //the trouble is with EUser. when this EKP is referred by EUser, this EKP does not know it.
        //the relation between ELevel & EKP is reciprocal, so is between ETest & EKP

        try {
            kp.isDeleted = true;
            kp.replacedBy = this.id;
//        kp.delete(); //and then delete kp
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    ETest[] mergeForETest(EKP kp) {
        ETest[] atest = kp.getTests();
        for (ETest test : atest) {
            test.replace(kp, this);
        }
        kp.tests.clear();
        return atest;
    }

    ELevelSystem[] mergeForELevel(EKP kp) {
        ELevelSystem[] asys = hmLevels.keySet().toArray(new ELevelSystem[0]);
        Set<Map.Entry<ELevelSystem, ELevel>> s = hmLevels.entrySet();
        for (Map.Entry<ELevelSystem, ELevel> me : s) {
            ELevelSystem sys = me.getKey();
            ELevel level = me.getValue();
            level.replace(kp, this);
        }
        hmLevels.clear();
        return asys;
    }

    EUser[] mergeForEUser(EKP kp) {
//         = kp.getTests();
//        for (EUser user : auser) {
//        }
        EUser[] auser = EUser.replace(kp, this);
        return auser;
    }

    /* TODO: I should first deal with ETest for all EKPs, then ETest can be saved, and memory released.
           then deal with ELevel for all EKPs
           and then deal with EUser for all EKPs
    if go around, for example, for each EKP,  go with ETest, ELevel, EUser.
       this way, we might load same ETest for more than once.
     */
    static CompletableFuture<Void> merge(String kpids) throws IOException {
        if (kpids == null) {
            throw new IllegalArgumentException();
        }
        System.out.println("EKP to be merged:" + kpids);
        int[] akpid = App.getInts(kpids);
        int kpid = akpid[0];
        EKP kp = EKP.getByID_m(kpid);
//        if (false) {
//            for (int i = 1; i < akpid.length; i++) {
//                int kpidt = akpid[i];
//                EKP kpt = EKP.getByID_m(kpidt);
//                kp.merge(kpt);
//            }
//        } else 
        {
            Merger<ETest> mergerTest = new MergerETest(kp);
            CompletableFuture<Void> cf = merge_1(akpid, mergerTest);
            return cf.thenCompose(v -> {
                Merger<ELevelSystem> mergerLevel = new MergerELevel(kp);
                return merge_1(akpid, mergerLevel);
            }).thenCompose(v -> {
                Merger<EUser> mergerUser = new MergerEUser(kp);
                return merge_1(akpid, mergerUser);
            });
        }
    }

    static class Merger<T> {

        final EKP kp;

        Function<EKP, T[]> f1;
        Function<T, CompletableFuture<Boolean>> f2;

        Merger(EKP kp) {
            this.kp = kp;
        }
    }

    static class MergerETest extends Merger<ETest> {

        MergerETest(EKP kp0) {
            super(kp0);
            f1 = (kpt) -> kp.mergeForETest(kpt);
            f2 = (test) -> test.save_cf();
        }
    }

    static class MergerELevel extends Merger<ELevelSystem> {

        MergerELevel(EKP kp0) {
            super(kp0);
            f1 = (kpt) -> kp.mergeForELevel(kpt);
            f2 = (sys) -> sys.save_cf();
        }
    }

    static class MergerEUser extends Merger<EUser> {

        MergerEUser(EKP kp0) {
            super(kp0);
            f1 = (kpt) -> kp.mergeForEUser(kpt);
            f2 = (sys) -> sys.save_cf();
        }
    }

    static <T> CompletableFuture<Void> merge_1(int[] akpid, Merger<T> merger) {
        try {
            HashSet<T> tests = new HashSet<>();
            for (int i = 1; i < akpid.length; i++) {
                int kpidt = akpid[i];
                EKP kpt = EKP.getByID_m(kpidt);
                T[] a = merger.f1.apply(kpt); //ETest[] a = mergeForETest(kpt);
                tests.addAll(Arrays.asList(a));
            }
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean>[] acf = new CompletableFuture[tests.size()];
            int i = 0;//i<cf.length;i++
            for (T test : tests) {
//            acf[i] = test.save_cf();
                acf[i] = merger.f2.apply(test);// test.save_cf();
            }
            return CompletableFuture.allOf(acf);
        } catch (Throwable t) {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            cf.completeExceptionally(t);
            return cf;
        }
    }

    void delete() {
        boolean referredByTest = false;
//        ETest[] ots = tests;
        if (tests != null) {
            if (tests.size() > 0) { //.length
                referredByTest = true;
            }
        } else {
            ArrayList<Integer> al = tests0;
            if (al != null) {
                if (al.isEmpty()) {
                } else {
                    referredByTest = true;
                }
            }
        }
        if (referredByTest) {
            throw new IllegalStateException();
        }
//        Collection<ELevel> c=hmLevels.values();
//        for(ELevel level:c){
//            level.kps.remove(id);
//        }
        boolean used = false;
        Set<Map.Entry<ELevelSystem, ELevel>> s = hmLevels.entrySet();
        for (Map.Entry<ELevelSystem, ELevel> me : s) {
            ELevel level = me.getValue();
            used = used || level.kps.remove(id);
        }
        this.isDeleted = true;
        if (used) {
        } else {
            //now it is really safe to be deleted.
            // now we can set idle flag, so this id can be reused since this id is indeed not used at present.
            this.isIdle = true;
        }
    }

    static ArrayList<EKP> filter(Function<EKP, Boolean> f) throws IOException {
        File dir = App.dirKPs();
        String[] af = dir.list(App.ff_json);
        ArrayList<EKP> kps = new ArrayList<>();
        for (String fn : af) {
            String[] as = fn.split("\\.");
            int id = Integer.parseInt(as[0]);
            id *= EKPbundle.bundleSize;
//            EKP kp = getByID_m(id);
            EKPbundle b = EKPbundle.getByID_m(id);
            for (EKP kp : b.kps) {
                if (f.apply(kp)) {
                    kps.add(kp);
                }
            }
        }
        return kps;
    }

    boolean withETest() {
        //ETest[] ots = tests;
        if (tests != null) {
            return tests.size() > 0; //ots.length > 0;
        } else {
            ArrayList<Integer> t0 = tests0;
            if (t0 != null) {
                return t0.size() > 0;
            }
        }
        return false;
    }

    boolean withETest(ETest test) {
        if (tests != null) {
            return tests.contains(test);
        } else {
            ArrayList<Integer> t0 = tests0;
            if (t0 != null) {
                return t0.contains(test.id);
            }
        }
        return false;
    }

    void add(ETest test) {
        if (tests != null) {
            tests.add(test);
        } else {
            ArrayList<Integer> t0 = tests0;
            if (t0 != null) {
                t0.add(test.id);
            } else {
                tests.add(test);
            }
        }
    }

    private void jsonLevels(JsonGenerator g) throws IOException {
        g.writeStartObject();
        Set<Map.Entry<ELevelSystem, ELevel>> s = hmLevels.entrySet();
        for (Map.Entry<ELevelSystem, ELevel> me : s) {
            g.writeFieldName(me.getKey().name);
            g.writeString(me.getValue().levelString());
        }
        g.writeEndObject();
    }

    private void parseLevels(JsonParser p) throws IOException {
        JsonToken t = p.getCurrentToken();//.nextToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    t = p.nextToken();
                    String level_s = p.getValueAsString();
                    ELevelSystem sys = ELevelSystem.getByName(name);
                    ELevel level = ELevel.get_m(sys, level_s);
                    hmLevels.put(sys, level);
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                }
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    static class FunctionNoELevel implements Function<EKP, Boolean> {

        private final ELevelSystem sys;

        FunctionNoELevel(ELevelSystem sys) {
            this.sys = sys;
        }

        @Override
        public Boolean apply(EKP kp) {
            return kp.getLevel(sys) == null;
        }

    }

    static class FunctionHalfELevel implements Function<EKP, Boolean> {

        private final ELevelSystem sys;
        boolean repair;

        FunctionHalfELevel(ELevelSystem sys, boolean repair) {
            this.sys = sys;
            this.repair = repair;
        }

        @Override
        public Boolean apply(EKP kp) {
            ELevel level = kp.getLevel(sys);
            if (level != null) {
                if (level.kps.contains(kp.id)) {
                    return false; //full relationship
                } else {
                    if (repair) {
                        level.kps.add(kp.id); //do repair
                    }
                    return true;
                }
            } else {
                return false; //NOELevel
            }
        }

    }

    static class FunctionHalfETest implements Function<EKP, Boolean> {

        boolean repair;

        FunctionHalfETest(boolean repair) {
            this.repair = repair;
        }

        @Override
        public Boolean apply(EKP kp) {
            ETest[] tests = kp.getTests();
            if (tests.length > 0) {
                int halftest = 0;
                for (ETest test : tests) {
                    if (kp.id == 299 & test.id == 0) {
                        halftest++;
                        halftest--;
                    }
                    if (test.contains(kp)) {
                    } else {
                        halftest++;
                        if (repair) {
                            System.out.println("repair " + test.id + " for " + kp.id);
                            test.add(kp);
                            test.save(20);
                        }
                    }
                }
                return halftest > 0;
            } else {
                return false; //NoTest
            }
        }

    }

}
