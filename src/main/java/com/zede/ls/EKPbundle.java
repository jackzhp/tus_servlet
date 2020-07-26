package com.zede.ls;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class EKPbundle {

    static int bundleSize = 100;

    static int getIDlast() {
        try {
            EKPbundle b = EKPbundle.getLast().get();
//            if (false) {
//                int size = b.kps.size();
//                if (size > 0) {
//                    EKP kp = b.kps.get(size - 1);
//                    return kp.id;
//                }
//                return -1;
//            }
            int id = -1;
            for (EKP kp : b.kps) {
                if (kp.id > id) {
                    id = kp.id;
                }
            }
            return id;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
//at present 100 EKP consists of 1 bundle
//    static HashMap<Integer, EKPbundle> hms = new HashMap<>(); //concurrent issue?
    static ConcurrentHashMap<Integer, EKPbundle> hms = new ConcurrentHashMap<>(); //concurrent issue?

    static EKPbundle getByID(int id) {
        int idFirst = id / bundleSize;
        idFirst *= bundleSize;
        return hms.get(idFirst);
    }

    static EKPbundle getByID_m(int id) throws IOException {
        int idFirst = id / bundleSize;
        idFirst *= bundleSize;
        EKPbundle b = hms.get(idFirst);
        if (b == null) {
            b = new EKPbundle(idFirst);
            EKPbundle bO = hms.putIfAbsent(idFirst, b);
            if (bO != null) {
                b = bO;
            }
            b.load();
        }
        return b;
    }

    static CompletableFuture<EKPbundle> getByID_cf(int id) {
        CompletableFuture<EKPbundle> cf = new CompletableFuture<>();
        try {
            cf.complete(getByID_m(id));
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

    int idFirst;
    String fn;
    HashSet<EKP> kps = new HashSet<>(); //ArrayList<EKP> kps = new ArrayList<>();
    static EKPbundle last;

    EKPbundle(int idFirst) {
        this.idFirst = idFirst;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof EKPbundle) {
                EKPbundle b2 = (EKPbundle) o;
                return this.idFirst == b2.idFirst;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return idFirst;
    }

    static CompletableFuture<EKPbundle> getLast() {
        CompletableFuture<EKPbundle> cf = new CompletableFuture<>();
        if (last != null) {
            cf.complete(last);
        } else {
            App.getExecutor().submit(() -> {
                try {
                    File dir = App.dirKPs();
                    String[] fns = dir.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".json");
                        }
                    }); //if no filter, hidden files will be picked up.
                    String fn;
                    if (fns.length > 0) {
                        Arrays.sort(fns);
                        fn = fns[fns.length - 1];
                        File f = new File(dir, fn);
                        if (f.length() > 0) {
                            String[] as = fn.split("\\.");
                            if (as.length != 2) {
                                //TODO:....
                                throw new Exception(as.length + " not good file name:" + fn);
                            } else {
                                int id = Integer.parseInt(as[0]);
                                last = new EKPbundle(id * bundleSize);
                                last.fn = fn;
                                hms.put(last.idFirst, last);
                                last.load();
                            }
                        }
                    } else {
                    }
                    if (last == null) {
                        last = new EKPbundle(0);
                    }
                    cf.complete(last);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            });
        }
        return cf;
    }

    public void set(EKP kp) {
        long limit = idFirst + bundleSize;
        if (kp.id >= idFirst && kp.id < limit) {
//            int idx = kp.id - idFirst;
//            int size = kps.size();
//            for (; size <= idx; size++) {
//                EKP kpt = new EKP();
//                kpt.id = idFirst + size;
//                kpt.isIdle = true;
//                kps.add(kpt);
//            }
//            kps.set(idx, kp);
            kps.add(kp);
        } else {
            throw new IllegalStateException(idFirst + " " + kp.id + " " + limit);
        }
    }

    //if we have it here, then EKP.kps must have it since when every EKP is loaded, I cache them.
//    EKP getEKPbyID(int id) {
//        long limit = idFirst + bundleSize;
//        if (id >= idFirst && id < limit) {
//            int idx = id - idFirst;
//            int size = kps.size();
//            if (idx < size) {
//                return kps.get(idx);
//            }
//            return null;
//        } else {
//            throw new IllegalStateException(idFirst + " " + id + " " + limit);
//        }
//    }
    String getFileName() {
        if (fn == null) {
            String s = Integer.toString(idFirst / bundleSize);
            StringBuilder sb = new StringBuilder();
            for (int n = 5 - s.length(); n >= 0; n--) {
                sb.append('0');
            }
            sb.append(s).append(".json");
            fn = sb.toString();
        }
        return fn;
    }

    File getFile(boolean extnew) {
        String fn = getFileName();
        if (extnew) {
            fn += ".new";
        }
        return new File(App.dirKPs(), fn);
    }

    /**
     *
     * @throws FileNotFoundException Yes. this might happen.
     * @throws IOException
     */
    void load() throws FileNotFoundException, IOException {
        File f = getFile(false);
//        if (f.exists()) {
//        } else {
//            return;
//        }
//        System.out.println("is loading from " + f.getAbsolutePath());
//TODO: java.io.FileNotFoundException:    because of concurrency.
//    this a big problem. not just for efficiency.
        JsonParser p = App.getJSONparser(f);
        JsonToken t = p.nextToken(); //.getCurrentToken(); will give null
        String sreason = null;
        if (t != JsonToken.START_OBJECT) {
            sreason = "expecting start_object, but " + t;
        } else {
            nextname:
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    t = p.nextToken();
                    if ("idFirst".equals(name)) {
                        if (t != JsonToken.VALUE_NUMBER_INT) {
                            sreason = "expecting int value, but " + t;
                        } else {
                            long id = p.getLongValue();
                            if (id != idFirst) {
                                sreason = "expecting idFirst:" + idFirst + ", but " + id;
                            } else {

                            }
                        }
                    } else if ("kps".equals(name)) {
                        if (false) {
                            if (t != JsonToken.START_ARRAY) {
                                sreason = "expecting start array, but " + t;
                            } else {
                                while (true) {
                                    t = p.nextToken();
                                    if (t == JsonToken.START_OBJECT) {
                                        EKP kp = new EKP(this);
                                        kp.parse(p);
                                        kps.add(kp);
                                        EKP.cache(kp);
                                    } else if (t == JsonToken.END_ARRAY) {
                                        break;
                                    } else {
                                        sreason = "unknown token in array:" + t;
                                        break nextname;
                                    }
                                }
                            }
                        } else { //now use object
                            //t = p.nextToken();
                            EKPbundle.parse(p, kps, this);
                        }
                    }
                } else {
                    if (t != JsonToken.END_OBJECT) {
                        sreason = "unknown token:" + t;
                    }
                    break;
                }
            }
        }
        if (sreason != null) {
            throw new IllegalStateException(sreason);
        }
    }

    // save for this class of objects can be immediately.
    void save() throws IOException, InterruptedException, ExecutionException {
        if (saving.compareAndSet(false, true)) {
            try {
                if (saveRequested >= saveLast) {
                    saveLast = System.currentTimeMillis();
                    saveNext = saveLast + saveDelayDefault;
                    File fnew = getFile(true);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fnew));
//        StringWriter sw=new StringWriter();
//            JsonGenerator g = factory.createGenerator(sw);
                    JsonGenerator g = App.getJSONgenerator(bos);
                    g.writeStartObject();
                    g.writeNumberField("idFirst", idFirst);
                    g.writeObjectFieldStart("kps"); //g.writeArrayFieldStart("kps");
                    json(g, kps);
                    g.writeEndObject(); //g.writeEndArray();
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    File f = getFile(false);
                    if (f.exists()) {
                        f.delete();
                    }
                    fnew.renameTo(f);
                    System.out.println("EKPbundle saved:" + idFirst);
                }
            } finally {
                saving.set(false);
            }
        }
    }

//    @Deprecated
//    static void parse(JsonParser p, ArrayList<EKP> kps) throws IOException {
//        JsonToken t = p.currentToken();
//        if (t == JsonToken.START_OBJECT) {
//            while (true) {
//                t = p.nextToken();
//                if (t == JsonToken.FIELD_NAME) {
//                    String name = p.getCurrentName();
//                    int kpid = Integer.parseInt(name);
//                    if (kpid == 299) {
//                        kpid++;
//                        kpid--;
//                    }
//                    EKP kp = EKP.getByID(kpid);
//                    t = p.nextToken();
//                    if (kp == null || kp.isRedundant) {
//                        kp = new EKP();
//                        kp.id = kpid;
//                        kp.parse(p);
//                        EKP.cache(kp);
//                    } else {
//                        if (t == JsonToken.START_OBJECT) {
//                            p.skipChildren();
//                        } else {
//                            throw new IllegalStateException("expectingh start object, but " + t);
//                        }
//                        t = p.getCurrentToken(); //.nextToken();
//                        if (t != JsonToken.END_OBJECT) {
//                            throw new IllegalStateException("expectingh end object, but " + t);
//                        }
//                    }
//                    kps.add(kp);
//                    continue;
//                } else if (t == JsonToken.END_OBJECT) {
//                    break;
//                } else {
//                    throw new IllegalStateException("expectingh start object, but " + t);
//                }
//            } //loop over EKP
//        } else {
//            throw new IllegalStateException("expectingh start object, but " + t);
//        }
//    }
//
//    @Deprecated
//    static void json(JsonGenerator g, ArrayList<EKP> kps) throws IOException {
//        if (kps.isEmpty()) {
//        } else {
//            EKP[] kpst = kps.toArray(new EKP[0]);
//            Arrays.sort(kpst);
////            if (kpst[0].id != idFirst) {
////                throw new IllegalStateException("idFirst is not right:" + kpst[0].id + " not " + idFirst);
////            }
//            for (EKP kp : kpst) {
//
//                if (kp.isRedundant) {
//                    kp = EKP.getByID(kp.id);
////    throw new IllegalStateException();
//                }
//                g.writeFieldName(Integer.toString(kp.id));
//                //Be noted: EKP does saved here,this storage is the one reliable.
//                //   this one take priority over the one in ETest
//                kp.json(g);
//            }
//        }
//    }
    static void parse(JsonParser p, HashSet<EKP> kps, EKPbundle b) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    int kpid = Integer.parseInt(name);
                    if (kpid == 299||kpid==543) {
                        kpid++;
                        kpid--;
                    }
                    EKP kp = EKP.getByID(kpid);
                    t = p.nextToken();
                    if (kp == null || kp.isRedundant) {
                        kp = new EKP(b);
                        kp.id = kpid;
                        kp.parse(p);
                        EKP.cache(kp);
                    } else {
                        if (t == JsonToken.START_OBJECT) {
                            p.skipChildren();
                        } else {
                            throw new IllegalStateException("expectingh start object, but " + t);
                        }
                        t = p.getCurrentToken(); //.nextToken();
                        if (t != JsonToken.END_OBJECT) {
                            throw new IllegalStateException("expectingh end object, but " + t);
                        }
                    }
                    kps.add(kp);
                    continue;
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                } else {
                    throw new IllegalStateException("expectingh start object, but " + t);
                }
            } //loop over EKP
        } else {
            throw new IllegalStateException("expectingh start object, but " + t);
        }
    }

    static void json(JsonGenerator g, HashSet<EKP> kps) throws IOException {
        if (kps.isEmpty()) {
        } else {
            EKP[] kpst = kps.toArray(new EKP[0]);
            Arrays.sort(kpst);
//            if (kpst[0].id != idFirst) {
//                throw new IllegalStateException("idFirst is not right:" + kpst[0].id + " not " + idFirst);
//            }
            for (EKP kp : kpst) {
                if (kp.isRedundant) {
                    kp = EKP.getByID(kp.id);
//    throw new IllegalStateException();
                }
                g.writeFieldName(Integer.toString(kp.id));
                //Be noted: EKP does saved here,this storage is the one reliable.
                //   this one take priority over the one in ETest
                kp.json(g);
            }
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

    CompletableFuture<Boolean> save_cf() {
        CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
        try {
            this.saveRequested = System.currentTimeMillis();
            save();
            cf.complete(true);
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

}
