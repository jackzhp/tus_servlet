package com.zede.ls;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
//import static com.zede.ls.EUser.user1;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
//import javax.jdo.JDOHelper;
//import javax.jdo.PersistenceManager;
//import javax.jdo.PersistenceManagerFactory;
//import javax.jdo.Transaction;

/**
 * long time ago, I had investigated persistence solutions. and I preferred one
 * of them. but now I do not remember which one it was. what I can remember is
 * that it is mainly designed by 1 person.
 *
 * I guess it is JDO. and I almost redesigned JDO for my own project with
 * annotation. it was only later I found JDO was doing the same thing as I did.
 *
 * but now, I decided not to use JDO(data nucleus). a few days ago, I thought if
 * I use this, it would be quick for me to move ahead. and I was forced to
 * choose a DB again. long time ago, I had used a list of them(marinDB, etc),
 * this time, I just go with derbyDB.
 *
 * now, I decide not to use data nucleus and derbyDB. I just use plain json
 * files. why? because of the size. or I just say they are still too
 * complicated. I do not need them.
 *
 *
 */
public class App {

    static String dirData = "/Users/yogi/jack/data/";
    static File dirTests, dirKPs, dirUsers, dirLevels,dirAudio;
    static String dirJapanese = "/Users/yogi/jack/japanese"; //TODO: from config
//    static EUser user1;// = new EUser(1);
//    static PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory("langs");
//    static PersistenceManager pm = pmf.getPersistenceManager();
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    static JsonFactory factory;

//    public static PersistenceManager getPM() {
//        throw new IllegalStateException("stop using them");//        return pm;
//    }
    private static JsonFactory getFactory() {
        if (factory == null) {
            factory = new JsonFactory();
        }
        return factory;
    }

    public static JsonGenerator getJSONgenerator(OutputStream baos) throws IOException {
        getFactory();
        JsonGenerator ret = factory.createGenerator(baos, JsonEncoding.UTF8);
        return ret;
    }

    public static JsonGenerator getJSONgenerator(File f) throws IOException {
        getFactory();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
        JsonGenerator ret = factory.createGenerator(bos, JsonEncoding.UTF8);
        return ret;
    }

    static JsonParser getJSONparser(File f) throws IOException {
        getFactory();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf8"));
//        factory.createParser(f); //what kind of encoding? autodetect by mechanism specified by json specification.
        return factory.createParser(br);
    }

    public static ScheduledExecutorService getExecutor() {
        return executor;
    }

    static void ensureExists(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory()) {
            } else {
                throw new IllegalStateException(dir + " should be directory");
            }
        } else {
            boolean tf = dir.mkdirs();
            if (tf) {
            } else {
                throw new IllegalStateException("can not create dir:" + dir.getAbsolutePath());
            }
        }
    }

    static File dirTests() {
        if (dirTests == null) {
            dirTests = new File(dirData, "tests");
            ensureExists(dirTests);
        }//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return dirTests;
    }

    static File dirUsers() {
        if (dirUsers == null) {
            dirUsers = new File(dirData, "users");
            ensureExists(dirUsers);
        }//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return dirUsers;
    }

    static File dirLevels() {
        if (dirLevels == null) {
            dirLevels = new File(dirData, "levels");
            ensureExists(dirLevels);
        }//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return dirLevels;
    }

    /**
     * be noted they are EKPbundle, not EKP.
     *
     * @return
     */
    static File dirKPs() { //
        if (dirKPs == null) {
            dirKPs = new File(dirData, "kps");
            ensureExists(dirKPs);
        }
        return dirKPs;
    }
    static File dirAudio(){
        if(dirAudio==null){
            dirAudio=new File(dirJapanese);
        }
        return dirAudio;
    }

    /**
     *
     * @param ireason ==0 means OK.
     * @param sreason
     * @param response
     * @throws IOException
     */
    static void sendFailed(int ireason, String sreason, HttpServletResponse response) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator g = getJSONgenerator(baos);
        g.writeStartObject();
        g.writeNumberField("ireason", ireason);
        if (ireason != 0) {
            g.writeStringField("sreason", sreason);
        }
        g.writeEndObject();
        g.flush();
        g.close();
        ServletOutputStream sos = response.getOutputStream();
        byte[] bytes = baos.toByteArray();// sreason.getBytes("utf8");
        response.setContentLength(bytes.length);
        response.setContentType("application/javascript;charset=UTF-8");
        sos.write(bytes);
        sos.flush();
        sos.close();
    }

    static void serve(HttpServletResponse response, File f) throws IOException {
        serve(response, f, "application/javascript;charset=UTF-8");
    }

    static void serve(HttpServletResponse response, File f, String ct) throws IOException {
        int size = (int) f.length();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        response.setContentLength(size);
        response.setContentType(ct);
        ServletOutputStream sos = response.getOutputStream();
        byte[] bytes = new byte[4096];
        while (true) {
            int len = bis.read(bytes);
            if (len == -1) {
                break;
            }
            sos.write(bytes, 0, len);
        }
        sos.flush();
        sos.close();
        bis.close();
    }

    static void serve(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
        int size = baos.size();
        response.setContentLength(size);
        response.setContentType("application/javascript;charset=UTF-8");
        ServletOutputStream sos = response.getOutputStream();
        sos.write(baos.toByteArray());
        sos.flush();
        sos.close();
    }

    static byte[] sha256(File f) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest;
        digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        byte[] bytes = new byte[4096];
        while (true) {
            int len = bis.read(bytes);
            if (len == -1) {
                break;
            }
            digest.update(bytes, 0, len);
        }
        return digest.digest();
    }

    static String sha256_s(File f) {
        try {
            byte[] hash = sha256(f);
            return Hex.toHexString(hash);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    static boolean contains(File f, byte[] target, byte[] bytes) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        int off = 0;
        boolean bret = false;
        while (true) {
            int len = bis.read(bytes, off, bytes.length - off);
            if (len == -1) {
                break;
            }
            off += len;
            int idxStart = App.indexOf(bytes, 0, off, target);
            if (idxStart != -1) {
                bret = true;
                break;
            }
        }
        bis.close();
        return bret;
    }

    static FilenameFilter ff_json = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".json");
        }
    };

    static int[] getInts(String ids_s) {
        if (ids_s == null) {
            return null;
        }
        if (ids_s.isEmpty()) {
            return new int[0];
        }
        String[] as = ids_s.split(",");
        int[] ids = new int[as.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Integer.parseInt(as[i]);
        }
        return ids;
    }

    void initTestKP() throws IOException { //TODO: just as test
        if (false) {
//            PersistenceManager pm = App.getPM();
//            Transaction tx = pm.currentTransaction();
//            try {
//                tx.begin();
//                ETest test = new ETest();
//                test.fn = "40-orewasu-pamanto.mp3";
//                test.info = "「俺はスーパマン」と言いました。";
//                String grade = null;
//                test.newKP("とas quotation", grade);
//                test.newKP("言う→言います→言いました", grade);
//                test.newKP("superman", grade);
//                pm.makePersistent(test);
//                tx.commit();
//            } finally {
//                if (tx.isActive()) {
//                    tx.rollback();
//                }
//                pm.close();
//            }
        } else {
            EUser user = null;// user1;// EUser.getByID(1);// user1;
            ETest test = new ETest();
            test.fnAudio = "40-orewasu-pamanto.mp3";
            test.info = "「俺はスーパマン」と言いました。";
            ELevel level = null;
            HashSet<EKPbundle> bs = new HashSet<>();
            int i = 0;
            EKP kp = test.newKP("とas quotation", level, user);
            bs.add(kp.bundle);
            kp = test.newKP("言う→言います→言いました", level, user);
            bs.add(kp.bundle);
            kp = test.newKP("superman", level, user);
            bs.add(kp.bundle);
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean>[] acf = new CompletableFuture[bs.size()];
            for (EKPbundle b : bs) {
                acf[i++] = b.save_cf();
            }
            CompletableFuture.allOf(acf).thenCompose(v -> {
                return test.save_cf();
            }).thenApply(tf -> {
                System.out.println("test saved");
                return true;
            }).exceptionally(t -> {
                t.printStackTrace();
                return true;
            });
        }
    }

    void changeKP() {
        int id = 0;
        String desc = "とas quotation";
        EUser user = null;
        EKP.getByID_cf(id, true).thenCompose((EKP kp) -> {
            return kp.chgDesc_cf(desc, user);
        }).thenApply(tf -> {
            System.out.println("KP desc changed, and saved");
//            return kp.getBundle_m().save_cf();
            return true;
//        }).thenCompose(tf -> {
//            return EKP.getByID_cf(id, true);
//        }).thenCompose((EKP kp) -> {
//            return kp.chgDesc_cf("a2", user);
//        }).thenApply(tf -> {
//            System.out.println("KP desc changed, and saved");
////            return kp.getBundle_m().save_cf();
//            return true;
//        }).thenCompose(tf -> {
//            return EKP.getByID_cf(id, true);
//        }).thenCompose((EKP kp) -> {
//            return kp.chgDesc_cf("a3", user);
//        }).thenApply(tf -> {
//            System.out.println("KP desc changed, and saved");
////            return kp.getBundle_m().save_cf();
//            return true;
        }).exceptionally((Throwable t) -> {
            t.printStackTrace();
            return null;
        });
    }

    //create a new user if not exists, otherwise do authentication.
    void initUser() throws InterruptedException, ExecutionException, IOException {
        String username = "jack";
        String password = "jack", pk = "pkFromRandomPrivateKey"; //pk is generated by the user(client side), not by us(server side).
        HashMap<String, String> passes = EUser.getUserNames(username).get();
        EUser user;
        if (passes.isEmpty()) {
            System.out.println("will create user");
            user = new EUser();
            user.name = username;
            user.pk = pk;
            user.password = password;
        } else {
            System.out.println("will do authentication");
            String nonce = "nonce";
            String sig = password + nonce;
            user = EUser.authenticate(passes, nonce, pk, sig);
            if (user == null) {
                throw new IllegalStateException("failed to authenticate");
            } else {
                System.out.println("authenticated");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                user.prepareToServeLevel(baos);
                System.out.write(baos.toByteArray());
            }
        }
//user.pk="";
        if (false) {
            ELevelSystem sys = ELevelSystem.getByName("misc");
            ELevel level = sys.previousLevel(2, 0);
            user.setTarget(level);
            user.save_cf().thenApply(tf -> {
                System.out.println("EUser saved:" + user.getFileName());
                return true;
            }).exceptionally(t -> {
                t.printStackTrace();
                return true;
            });
        }

        getExecutor().schedule(() -> {
            System.out.println();
            try {
                user.getTest(true).thenApply((ETest[] tests) -> {
                    if (tests.length == 0) {
                        return null;
                    }
                    ETest test = tests[0];
                    File f = test.getFile(false);
                    System.out.println(tests.length + " will serve#1: " + f.getAbsolutePath());
                    if (tests.length > 1) {
                        System.out.println("will serve#1.2: " + tests[1].id);
                    }
                    return test;
                }).thenCompose((ETest test) -> {
                    if (test == null) {
                        throw new IllegalStateException("no test returned");
                    }
                    long lts = System.currentTimeMillis();
                    int testid = test.id;
                    int[] bads = App.getInts("");
                    //test.onTested(user, lts, bads);
                    return user.onTested(lts, testid, bads);
                }).thenCompose(tf -> {
                    System.out.println("\n");
                    return user.getTest(true);
                }).thenApply((ETest[] tests) -> {
                    if (tests.length == 0) {
                        return false;
                    }
                    ETest test = tests[0];
                    File f = test.getFile(false);
                    System.out.println("will serve#2: " + f.getAbsolutePath());
                    if (tests.length > 1) {
                        System.out.println("will serve#2.2: " + tests[1].id);
                    }
                    return true;
                }).thenApply(tf -> {
                    if (tf == false) {
                        throw new IllegalStateException("no test returned");
                    }
                    return true;
                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 5, TimeUnit.SECONDS);
    }

    void initLevelSystem() {
        ELevelSystem sys = ELevelSystem.getByName("misc");
        sys.getLevel_m(1, 1);
        sys.getLevel_m(1, 2);
        sys.getLevel_m(1, 3);
        sys.getLevel_m(1, 4);
        sys.getLevel_m(2, 1);
        sys.save_cf().exceptionally(t -> {
            t.printStackTrace();
            return true;
        });
    }

    private void auditELevels() {
        String name = "misc";
        ELevelSystem sys = ELevelSystem.getByName(name);
        System.out.println("level system:" + name + "\nlevel : #ofKnowledgePoints : #ofTests");
        for (ELevel level : sys.levels) {
            System.out.println(level.levelString() + " : " + level.kps.size() + " : " + level.tests.size());
        }
        //check every EKP, there is at least one ETest for it. this is in auditEKPs

    }

    public static void main(String[] args) {
        try {
            App app = new App();

            if (false) {
                String s = null;
                int[] a = App.getInts(s);
                s = "";
                a = App.getInts(s);
                s = "1";
                a = App.getInts(s);
                s = "1,2";
                a = App.getInts(s);
                s = "1,2,3";
                a = App.getInts(s);

            }

            if (false) {
                app.initLevelSystem();
                return;
            }
            if (false) {
                app.initTestKP();
            }
            if (false) {
                app.changeKP();
                return;
            }
            if (false) {
                app.auditELevels();
                return;
            }
            if (false) {
                app.auditETests();
//                return;
            }
            if (false) {
                app.auditEKPs();
                return;
            }
            if (true) {
                app.initUser();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * find ab in the range of bytes
     *
     *
     * @param bytes
     * @param istart
     * @param off defines the range from istart to off in bytes
     * @param ab
     * @return
     */
    public static int indexOf(byte[] bytes, int istart, int off, byte[] ab) {
        int limit = off - ab.length + 1;
        startOver:
        for (int i = istart; i < limit; i++) {
            if (bytes[i] == ab[0]) {
                for (int j = 1; j < ab.length; j++) {
                    if (bytes[i + j] - ab[j] != 0) {
                        continue startOver;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    private void auditETests() throws IOException {
        ArrayList<ETest> tests;
        System.out.println("will call noEKP");
        tests = ETest.EKP_none();
        System.out.println("noKP #of tests:" + tests.size());
//        STest.serve(null, tests, "nokp");
        serve(tests, "nokp");
        System.out.println("\nserved");

        System.out.println("\nwill call halfEKP");
        tests = ETest.EKP_half();
        System.out.println("halfKP #of tests:" + tests.size());
//        STest.serve(null, tests, "nokp");
        serve(tests, "halfkp");
        System.out.println("\nserved");

        ELevelSystem sys = ELevelSystem.getByName("misc");
        if (false) {
            System.out.println("\nwill call noELevel");
            tests = ETest.ELevel_none(sys);
            System.out.println("nolevel #of tests:" + tests.size());
//        STest.serve(null, tests, "nokp");
            serve(tests, "nolevel");
            System.out.println("\nserved");
        }
        System.out.println("\nwill call halfELevel");
        tests = ETest.ELevel_half(sys);
        System.out.println("halflevel #of tests:" + tests.size());
//        STest.serve(null, tests, "nokp");
        serve(tests, "halflevel");
        System.out.println("\nserved");

    }

    static void serve(ArrayList<ETest> tests, String action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator g = App.getJSONgenerator(baos);
        g.writeStartObject();
        g.writeArrayFieldStart(action);
        for (ETest test : tests) {
            g.writeNumber(test.id);
        }
        g.writeEndArray();
        g.writeEndObject();
        g.flush();
        g.close();
        System.out.write(baos.toByteArray());
    }

    private void auditEKPs() throws IOException {
        ArrayList<EKP> kps;
        System.out.println("will call noETest");
        kps = EKP.noETest();
        System.out.println("noETest #of KPs:" + kps.size());
//        STest.serve(null, tests, "nokp");
        serveKPs(kps, "notest");
        System.out.println("\nserved");

        System.out.println("will call halfETest");
        kps = EKP.halfETest();
        System.out.println("halfETest #of KPs:" + kps.size());
//        STest.serve(null, tests, "nokp");
        serveKPs(kps, "halftest");
        System.out.println("\nserved");

        ELevelSystem sys = ELevelSystem.getByName("misc");
        System.out.println("will call halfELevel");
        kps = EKP.halfELevel(sys);
        System.out.println("halfELevel #of KPs:" + kps.size());
//        STest.serve(null, tests, "nokp");
        serveKPs(kps, "halflevel");
        System.out.println("\nserved\n");

        System.out.println("will call noELevel");
        kps = EKP.noELevel(sys);
        System.out.println("NoELevel #of KPs:" + kps.size() + "\n");
//        STest.serve(null, tests, "nokp");
        serveKPs(kps, "nolevel");
        System.out.println("\nserved");
        ELevel level = sys.getLevel_m(1, 1);
        if (kps.isEmpty()) {
        } else {
            EKP kp = kps.get(0);
            kp.set(level);
            kp.getBundle_m().save_cf().thenCompose(tf -> {
                return sys.save_cf();
            }).exceptionally(t -> {
                t.printStackTrace();
                return null;
            });
        }

        HashMap<ELevel, Integer> histogram = EKP.levels(sys);
//        Set<Map.Entry<ELevel, Integer>> s=histogram.entrySet();
        ELevel[] keys = histogram.keySet().toArray(new ELevel[0]);
        Arrays.sort(keys, sys.c);
        for (ELevel key : keys) {
            if (key == null) {
                System.out.println("null key");
                continue;
            }
            Integer n = histogram.get(key);
            System.out.println(key.levelString() + ": " + n);
        }
    }

    static void serveKPs(ArrayList<EKP> tests, String action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator g = App.getJSONgenerator(baos);
        g.writeStartObject();
        g.writeArrayFieldStart(action);
        for (EKP test : tests) {
            g.writeNumber(test.id);
        }
        g.writeEndArray();
        g.writeEndObject();
        g.flush();
        g.close();
        System.out.write(baos.toByteArray());
    }
}
