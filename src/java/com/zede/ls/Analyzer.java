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

    String url = "https://www.thejapanesepage.com/japanese-grammar-100-part-ii/";
    File dirDst = new File("/Users/yogi/jack/japanese");

    public static void main(String[] args) {
        try {
            new Analyzer().run();
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
    private String mimeType;
    private String encoding;
    private int size;
    private File fdownloading;

    //found audio
    void onFoundAudio(String s) throws Exception {
//            System.out.println(s);
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
                onFoundAudioURL(src);
                return;
            }
        }
        throw new Exception(s);
    }

    void onFoundAudioURL(String srcurl) throws Exception {
//        System.out.println(srcurl);
        //TODO: if it is relative, I should construct its absolute form
        String fn;
        int istart = srcurl.lastIndexOf('/');
        if (istart != -1) {
            istart++;
            if (istart < srcurl.length()) {
                fn = srcurl.substring(istart + 1);
                File fdst = new File(dirDst, fn);
                if (fdst.exists()) {
//                    fdst.delete();
                    System.out.println(fn + " exists");
                } else {
                    download(srcurl, fdst);
                    if (fdst.exists()) {
                        ETest test = new ETest();
                        test.fn =fn;
                        test.save();
                    }
                }
                return;
            }
        }
        throw new Exception(srcurl);
    }

    void run() throws Exception {

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
                        onFoundAudio(sb.toString());
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
    }

    void download(String url, File foutput) throws Exception {
        if (false) {
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

}
