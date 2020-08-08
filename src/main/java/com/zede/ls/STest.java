package com.zede.ls;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import static com.zede.ls.App.OIDtoPrimitive;
import static com.zede.ls.App.serve;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static com.zede.ls.App.jsonArrayIDs;
import static com.zede.ls.App.serveKPs;
import static com.zede.ls.EKPbundle.json;

/**
 * the user tells either all good. or failed on some EKP's.
 *
 *
 *
 */
public class STest extends HttpServlet {

    String dir = "/Users/yogi/jack/japanese"; //TODO: from config

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet STest</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet STest at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
//        try {
//            new App().testSave();
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
        int ireason = 0;
        String sreason = null;
        try {
            HttpSession session = request.getSession();
            //TODO:check whether we should serve the user.
            EUser user = (EUser) session.getAttribute("user");
            if (user == null) {
                ireason = -1;
                sreason = "not authenticated";
            } else {
                String action = request.getParameter("act");
                if ("test".equals(action)) {
                    int id = getInt(request, "testid");
//        ArrayList<ETest> tests = null;
//
//        File fdir = new File(dir);
//        String[] afn = fdir.list(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith(".mp3");
//            }
//        });
//        System.out.println("# of files:" + afn.length);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
////        StringWriter sw=new StringWriter();
////            JsonGenerator g = factory.createGenerator(sw);
//        JsonGenerator g = App.getJSONgenerator(baos);
//        g.writeStartObject();
//        g.writeNumberField("ireason", 5);
////generator.writeStringField("brand", "Mercedes");
//        g.writeArrayFieldStart("tests");
//        for (String fn : afn) {
//            g.writeStartObject();
//            g.writeStringField("fn", fn);
//            g.writeStringField("key", ""); //the key word of the record.
//            //many other fields.
//            g.writeEndObject();
//        }
//        g.writeEndArray();
//        g.writeEndObject();
//        g.flush();
//        g.close();
//        int size = baos.size();
                    File f = ETest.getFileByID(id, false);
                    App.serve(response, f);
//            } else if ("nolevel".equals(action)) {
//                String sysName = request.getParameter("sys");
//                ELevelSystem sys = ELevelSystem.getByName(sysName);
//                ArrayList<ETest> tests = ETest.ELevel_none(sys);
//                serve(response, tests, action);
                } else if ("tests".equals(action)) {
                    HashSet<Integer> tests = new HashSet<>();
                    String category = request.getParameter("c");
                    if ("4level".equals(category)) {
                        String sysName = request.getParameter("sys");
                        ELevelSystem sys = ELevelSystem.getByName(sysName);
                        String level_s = request.getParameter("level");
                        if ("0.0".equals(level_s)) { //nolevel. this will not happen!
//                            ETest.noELevel(sys, tests);
                        } else { //TODO: this is not needed at all. since the client can get the whole level system file.
//                            ELevel level = ELevel.get_m(sys, level_s);
//                            if (level != null) {
////                                kps = new ArrayList<>(level.kps.size());
//                                for (Integer kpid : level.tests) {
////TODO: level.kps might contain an EKP, but whose level is not ELevel. let's call them "missing" or "conflicts"
////    how to deal with this?
////  we list and present them(shows their levels)
////   and allow the user to confirm their levels.
//
//                                    try {
//                                        EKP kp = EKP.getByID_m(kpid, true);
//                                        tests.add(kp); //true to ensure it is loaded from EKPbundle, rather than the one from ETest.
//                                    } catch (Throwable t) {
//                                        t.printStackTrace();
//                                    }
//                                }
//                            } else {
//                                throw new Exception("level is null for " + sysName + ":" + level_s);
//                            }
                        }
                    } else if ("nokp".equals(category) || "nolevel".equals(category)) {
                        HashSet<ETest> otests = new HashSet<>();
                        ETest.EKP_none(otests); //use ETest.id, not ETest.getID();
                        int[] ids = App.OIDtoPrimitive(otests);
                        serve(response, ids, category);//serve(response, otests, action);
                        return;
                    } else {
                        throw new Exception("unknow category:" + category);
                    }
                    serve(response, category, tests);
//                } else if ("nokp".equals(action)) {
//                    HashSet<ETest> tests = new HashSet<>();
//                    ETest.EKP_none(tests);
//                    serve(response, tests, action);
                } else if ("halfkp".equals(action)) {
//                    HashSet<ETest> tests = new HashSet<>();
                    HashSet<EKP> kps = new HashSet<>();
//                    ETest.EKP_half(kps);//tests, App.FixHalf_Reciprocol);
                    //for a ELevel refers to a ETest, but the ETest does not refer to the ELevel.
                    // how should I fix the relationship?
                    //   present them in test.html, let the user choose.
                    // we list the EKP's referred by the ETest, and list EKP's that refers the ETest.
                    // if autofix is indeed needed, then we changes only EkP's side. 
                    HashSet<EKP> halvesKP = new HashSet<>();
                    HashSet<ETest> halvesTest = new HashSet<>();
                    System.out.println("\nwill fix relation between EKP & ETest");
                    App.fix_EKP_ETest(halvesKP, halvesTest);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    JsonGenerator g = App.getJSONgenerator(baos);
                    g.writeStartObject();
                    App.jsonArrayIDs(g, halvesTest, "tests");
                    App.jsonArrayIDs(g, halvesKP, "kps");
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    App.serve(response, baos);
                } else {
                    ireason = -1;
                    sreason = "unknown action:" + action;
                }
            }
        } catch (Throwable t) {
            ireason = -1;
            t.printStackTrace();
            sreason = "uncaught exception:" + t.getMessage();
        }
        if (ireason != 0) {
            App.sendFailed(ireason, sreason, response);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
        int ireason = 0;
        String sreason = "";
        try {
            HttpSession session = request.getSession();
            //TODO: do not need to getByID, user is a session object once authenticated.
            EUser user = (EUser) session.getAttribute("user");
            if (user == null) {
                ireason = -1;
                sreason = "not authenticated";
            } else {
                String action = request.getParameter("act");// "result";
//        if ("result".equals(action)) { //this moved to SUser
////            EUser user = App.user1;//TODO: for temp  null; 
//            Set<EKP> kps = null;
//            long lts = System.currentTimeMillis();
//            test.onTested(user, lts, kps);
//        } else 
                if ("chgInfo".equals(action)) {
                    String info = request.getParameter("info");
                    ETest test = getETest(request);
                    test.chgInfo(info, user);
                    App.sendFailed(ireason, sreason, response);
                } else if ("newKP".equals(action)) { //TODO: stop using this one, instead use SKP.addKP and then STest.associate.
                    //TODO: turn into async mode.
                    String desc = request.getParameter("desc");// "description of KP"; //TODO: for temp
                    ELevel level = ELevel.get_m(user.target.sys, request.getParameter("level"));
                    ETest test = getETest(request);
                    test.newKP_cf(desc, level, user).exceptionally(t -> {
                        t.printStackTrace();
                        return null;
                    });
                    App.sendFailed(ireason, sreason, response);
                } else if ("deleteKP".equals(action)) {
                    String id_s = request.getParameter("idkp");
                    int id = Integer.parseInt(id_s);
                    EKP kp = EKP.getByID(id);
                    ETest test = getETest(request);
                    test.deleteKP(kp, user).thenAccept(tf -> {

                    }).exceptionally(t -> {
                        t.printStackTrace();
                        return null;
                    });
                    App.sendFailed(ireason, sreason, response);
//                } else if ("associate".equals(action)) {
//                    //TODO: turn into async mode.
//                    String kpid_s = request.getParameter("idkp");
//                    int kpid = Integer.parseInt(kpid_s);
//                    EKP kp = EKP.getByID_m(kpid);
//                    ETest test = getETest(request);
//                    test.associate(kp, user);
//                    App.sendFailed(ireason, sreason, response);
                } else if ("addKP".equals(action)) { //TODO: stop using this one, instead use SKP.addKP and then STest.associate.
                    String id_s = request.getParameter("idkp");
                    int id = Integer.parseInt(id_s);
                    EKP kp = EKP.getByID(id);
                    ETest test = getETest(request);
                    test.addKP_cf(kp, user).thenAccept(tf -> {
                    }).exceptionally(t -> {
                        t.printStackTrace();
                        return null;
                    });
                    App.sendFailed(ireason, sreason, response);
                } else if ("mergeTests".equals(action)) {
                    String id_s = request.getParameter("testids");
                    ETest.merge(id_s).get();
                    App.sendFailed(ireason, sreason, response);
                } else if ("searchTests".equals(action)) {
                    String s = request.getParameter("s");
                    String[] as = s.split("AND");
                    /**
                     *
                     * the usual use case: when a user wants to create a new
                     * EKP. and the user wants to avoid making many copies of
                     * the same EKP(they can be merged), so the user wants to
                     * find out the EKP they are trying to creating. so we have
                     * to search for it.
                     *
                     * Be noted, there is no point to list all EKP's. but we can
                     * list those EKP's with desc starts with the target string.
                     */
                    App.ConditionSearch cs = new App.ConditionSearch(as);
                    HashSet<ETest> tests = new HashSet<>();
                    ETest.search(cs, tests);
                    serve(response, tests, "search");
                } else if ("fixRelELevel".equals(action)) { //TODO: fixRelEKP is in GET
                    HashSet<ELevel> halvesLevel = new HashSet<>();
                    String sysName = request.getParameter("sys");
                    ELevelSystem sys = ELevelSystem.getByName(sysName);
//        System.out.println("\nwill fix relation between ETest & ELevel");
//        System.out.println("will clear all ETest <- ELevel");
                    sys.clearETest(); //then whatever is needed will be created in ETest.ELevel_half
//        System.out.println("will fix ETest -> ELevel without <-");
                    int fixed = ETest.ELevel_half(sys, halvesLevel);
                    App.sendFailed(ireason, sreason, response);
                } else {
                    throw new Exception("unknown action:" + action);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            ireason = -1;
            sreason = "uncaught exception:" + t.getMessage();
        }
        if (ireason != 0) {
            App.sendFailed(ireason, sreason, response);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

    static void serve(HttpServletResponse response, HashSet<ETest> tests, String category) throws IOException {
        tests = ETest.distinct(tests);
        int[] ids = App.OIDtoPrimitive(tests);
        serve(response, ids, category);
    }

    static void serve(HttpServletResponse response, int[] ids, String category) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator g = App.getJSONgenerator(baos);
        g.writeStartObject();
        g.writeStringField("category", category);
        App.jsonArrayIDs(g, ids, "tests"); //originally this is category.
//        g.writeArrayFieldStart(action);
//        for (ETest test : tests) {
//            g.writeNumber(test.id);
//        }
//        g.writeEndArray();
        g.writeEndObject();
        g.flush();
        g.close();
        App.serve(response, baos);
    }

    private void serve(HttpServletResponse response, String category, HashSet<Integer> tests) throws IOException {
        int[] ids = App.IntegerToPrimitive(tests);
        serve(response, ids, category);
    }

    static ETest getETest(HttpServletRequest request) {
        String id_s = request.getParameter("idtest");
        int id = Integer.parseInt(id_s);
        ETest test = ETest.loadByID_m(id); //getByID
        if (test == null) {
            throw new IllegalStateException("can not find the ETest#" + id_s);
        }
        return test;
    }

    private int getInt(HttpServletRequest request, String name) throws Exception {
        String id_s = request.getParameter(name);
        if (id_s == null) {
            throw new Exception(name + " is a must"); //IllegalState
        }
        int id = Integer.parseInt(id_s);
        return id;
    }
}
