package com.zede.ls;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 *
 * inside a webpage, there are lots of audio element, and source element. this
 * class is to parse those source for the audio.
 *
 * so we can download those audio files later.
 *
 * for example: https://www.thejapanesepage.com/japanese-grammar-100-part-ii/
 *
 *
 *
 */
public class Analyzer {

    EUser user = null;

    String sysName = "misc";
    String grade = "1.1";
    ELevel level;
    String url = "https://www.thejapanesepage.com/japanese-grammar-100-part-i/";
    File dirDst = new File("/Users/yogi/jack/japanese");

    public static void main(String[] args) {
        try {
            Analyzer a = new Analyzer();
            if (false) {
                a.run();
            }
            if (false) {
                a.modifyLevels_ETest();
            }
            if (true) {
                a.modifyLevels_EKPbundle();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
    private String mimeType;
    private String encoding;
    private int size;
    private File fdownloading;

    //found audio
    CompletableFuture<Boolean> onFoundAudio(String s) throws Exception {
//            System.out.println(s);
        try {
            int istart = s.indexOf("src=\"");
            if (istart == -1) {
//            System.out.println(s);
            } else {
                istart += 5;
                int iend = s.indexOf('\"', istart);
                if (iend == -1) {
//                System.out.println(s);
                } else {
//System.out.println(istart+":"+iend);                
                    String src = s.substring(istart, iend);
                    return onFoundAudioURL(src);
                }
            }
            throw new Exception(s);
        } catch (Throwable t) {
            CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
            cf.completeExceptionally(t);
            return cf;
        }
    }

    CompletableFuture<Boolean> onFoundAudioURL(String srcurl) throws Exception {
//        System.out.println(srcurl);
        //TODO: if it is relative, I should construct its absolute form
        try {
            String fn;
            int istart = srcurl.lastIndexOf('/');
            if (istart != -1) {
                istart++;
                if (istart < srcurl.length()) {
                    fn = srcurl.substring(istart);
                    File fdst = new File(dirDst, fn);
                    if (fdst.exists()) {
//                    fdst.delete();
                        System.out.println(fn + " exists");
                    } else {
                        download(srcurl, fdst);
                    }
                    if (fdst.exists()) {
                        ETest test;
                        String fsha = App.sha256_s(fdst);
                        test = ETest.getByFileAudio(fsha);
                        if (test == null) {
                            test = new ETest();
                            test.newID();
                            test.fnAudio = fn;
                            ELevel levelt = getLevelID(test.fnAudio);
                            if (levelt == null) {
                                levelt = level;
                            }
                            test.fsha = fsha;
                            int iloc = fn.lastIndexOf(".mp3");
                            test.info = fn.substring(0, iloc);
                            ETest ot = test;
                            return test.newKP_cf(test.info, levelt, user).thenCompose((kp) -> {
                                System.out.println(" EKP saved, now will save ETest");
                                return ot.save_cf();
                            }).thenApply((tf) -> {
                                System.out.println(" test " + fn + " saved");
                                level.tests.add(ot.id); //do this after ETest has been saved.
                                return true;
                            });
//                                    .exceptionally(t -> {
//                                t.printStackTrace();
//                                return true;
//                            });
                        } else {
                            return CompletableFuture.completedFuture(true);
                        }
                    } else {
                        throw new Exception("file does not exist:" + fn);
                    }
                }
            }
            throw new Exception(srcurl);
        } catch (Throwable t) {
            CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
            cf.completeExceptionally(t);
            return cf;
        }
    }

    void run() throws Exception {
        String username = "jack";
        String password = "jack", pk = "pkFromRandomPrivateKey"; //pk is generated by the user(client side), not by us(server side).
        HashMap<String, String> passes = EUser.getUserNames(username).get();
        System.out.println("will do authentication");
        String nonce = "nonce";
        String sig = password + nonce;
        user = EUser.authenticate(passes, nonce, pk, sig);
        ELevelSystem sys = user.target.sys;
        level = ELevel.get_m(sys, grade);

        fdownloading = new File("tmp.html");
        System.out.println("location:" + fdownloading.getAbsolutePath());
        if (fdownloading.exists()) {
        } else {
            download(url, fdownloading);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fdownloading), "utf8"));
        String line;
        StringBuilder sb = new StringBuilder();
        boolean opened = false;
        LinkedList<CompletableFuture<Boolean>> cfs = new LinkedList<>();
        nextline:
        while (true) {
            line = br.readLine();
            if (line == null) {
                break;
            }
            int istart = 0;
            currentline:
            while (true) {
                if (opened) {
                    int iend = line.indexOf("</audio>", istart);
                    if (iend == -1) {
                        sb.append(' ').append(line.substring(istart));
                        continue nextline;
                    } else {
                        opened = false;
                        sb.append(' ').append(line.substring(istart, iend)); //iend+8
                        CompletableFuture<Boolean> cf = onFoundAudio(sb.toString());
                        cfs.add(cf);
                        sb.delete(0, sb.length());
                        istart = iend + 8;
                        continue currentline;
                    }
                } else { //not open yet
                    istart = line.indexOf("<audio", istart);
                    if (istart == -1) {
                        continue nextline;
                    }
                    opened = true;
                    continue currentline;
                }
            } //end of current line //end of current line
        }
        CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).thenCompose(v -> {
            return ELevelSystem.getByName(sysName).save_cf();
        }).thenApply(tf -> {
            System.out.println("level system saved:" + tf);
            return true;
        }).exceptionally(t -> {
            t.printStackTrace();
            return true;
        });
    }

    void download(String url, File foutput) throws Exception {
        if (foutput.exists()) {
            return;
        }
        if (false) { //use pure jave to download webpage and audio file.
            downloadJava(url, foutput); //i got 403.
        } else {
            //TODO: download with curl.
            String cmd = "curl " + url + " -o " + foutput.getAbsolutePath();
            System.out.println(cmd);
            Runtime.getRuntime().exec(cmd);
        }

    }

    //I got 403, though curl can get it.
    void downloadJava(String url, File foutput) throws Exception {
        URL ourl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) ourl.openConnection();
        // just want to do an HTTP GET here
        connection.setRequestMethod("GET");
        connection.setUseCaches(true);
        //connection.setRequestProperty("Accept-Encoding", "gzip");
        /**
         * give it 10 seconds to respond. when timed out, what will happen?
         * SocketTimeoutException will be thrown
         */
        int timeout = 10 * 1000;
        connection.setReadTimeout(timeout);
        connection.connect();
        int code = connection.getResponseCode();
        //if (code / 100 == 4) { //chinese character is causing 400.
        if (code != 200) {
            throw new Exception("code:" + code);
        }
        this.mimeType = connection.getContentType();
        this.encoding = connection.getContentEncoding();
        this.size = connection.getContentLength();
        // read the output from the server
        BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
        FileOutputStream fos = new FileOutputStream(foutput, false);
        byte[] buf = new byte[4096];
        int len = -1;
        long size = 0;
        while (true) {
            len = bis.read(buf);
            if (len <= 0) {
                break;
            }
            size += len;
            fos.write(buf, 0, len);
        }
        fos.flush();
        fos.close();
        bis.close();
        System.out.println("downloaded " + size + "/" + this.size);
    }

    private void modifyLevels_ETest() throws IOException {
        ELevelSystem sys = ELevelSystem.getByName("misc");// user.target.sys;
        level = ELevel.get_m(sys, grade);
        if (level.sys != sys) {
            throw new IllegalStateException();
        }

        HashSet<EKP> changed = new HashSet<>();
        int nchanged = 0;
        File dir = App.dirTests();
        String[] afn = dir.list(App.ff_json);
        for (String fn : afn) {
            String[] as = fn.split("\\.");
            String id_s = as[0];
            Integer id = Integer.parseInt(id_s);
            ETest test = ETest.loadByID_m(id);
            ELevel levelt = getLevelID(test.fnAudio);
            if (levelt != null) {
                for (EKP kp : test.kps) {
                    if (kp.desc.equals(test.info)) {
//                        if (kp.id == 898) {
//                            System.out.println("test:" + id+" "+test.kps.size());
//                        }
                        kp = EKP.getByID_m(kp.id, true);
                        if (kp.set(levelt)) {
                            System.out.println(kp.id + ":" + levelt.levelString() + ":" + kp.desc);
                            nchanged++;
                            changed.add(kp);
                        }
                    }
                }
            }
        }
        System.out.println(nchanged + " # changed:" + changed.size());
        for (EKP kp : changed) {
//            kp.save(5);
        }

    }

    private ELevel getLevelID(String fnAudio) {
        String id_s = null;
        try {
            if (fnAudio.indexOf('-') != -1) {
                id_s = null;
            }
            int iloc = fnAudio.indexOf('-');
            if (iloc == -1) {
                return null;
            }
            id_s = fnAudio.substring(0, iloc);
            int id = Integer.parseInt(id_s);
            ELevel levelt = level.sys.getLevel_m(1, id);
            return levelt;
        } catch (Throwable t) {
            System.out.println(fnAudio);
            t.printStackTrace();
            return null;
        }
    }

    private void modifyLevels_EKPbundle() throws IOException {
        ELevelSystem sys = ELevelSystem.getByName("misc");// user.target.sys;
        level = ELevel.get_m(sys, grade);
        if (level.sys != sys) {
            throw new IllegalStateException();
        }

        HashSet<EKP> changed = new HashSet<>();
        int nchanged = 0;
        File dir = App.dirKPs();
        String[] afn = dir.list(App.ff_json);
        for (String fn : afn) {
            String[] as = fn.split("\\.");
            String id_s = as[0];
            Integer id = Integer.parseInt(id_s);
            EKPbundle bundle = EKPbundle.getByID_m(id * EKPbundle.bundleSize);
            System.out.println("bundle size:" + bundle.kps.size());
            for (EKP kp : bundle.kps) {
                StringBuilder sb = new StringBuilder();
                sb.append(kp.id).append(": ");
                ELevel lt = kp.hmLevels.get(sys);
                if (lt != null) {
                    sb.append(lt.levelString());
                } else {
                    sb.append("null");
                }
                sb.append(": ").append(kp.desc);
                System.out.println(sb.toString());
//                if (kp.id == 543) {
//                    System.out.println(kp.id + ":" + kp.desc);
//                }
                ELevel levelt = getLevelID(kp.desc);
                if (levelt != null) {
                    nchanged++;
                    if (kp.set(levelt)) {
//                        System.out.println(kp.id + ":" + levelt.levelString() + ":" + kp.desc);
                        changed.add(kp);
                    }
                }
            }
//            EKP kp = EKP.getByID_m(543, true);
//            ELevel levelt = getLevelID(kp.desc);
//            if (levelt != null) {
//                System.out.println("found 543");
//                nchanged++;
//                if (kp.set(levelt)) {
//                    System.out.println(kp.id + ":" + levelt.levelString() + ":" + kp.desc);
//                    changed.add(kp);
//                }
//            }
        }
        System.out.println(nchanged + " # changed:" + changed.size());
        for (EKP kp : changed) {
//            kp.save(5);
        }

    }

}
