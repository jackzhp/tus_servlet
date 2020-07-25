package com.zede.ls;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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

                String id_s = request.getParameter("testid");
                if (id_s == null) {
                    throw new Exception("testid is a must"); //IllegalState
                }
                int id = Integer.parseInt(id_s);
                String action = request.getParameter("act");
                if ("test".equals(action)) {
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
                } else if ("nokp".equals(action)) {
                    ArrayList<ETest> tests = ETest.EKP_none();
                    serve(response, tests, action);
                } else if ("halfkp".equals(action)) {
                    ArrayList<ETest> tests = ETest.EKP_half();
                    serve(response, tests, action);
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
                String id_s = request.getParameter("idtest");
                int id = Integer.parseInt(id_s);
                ETest test = ETest.loadByID_m(id); //getByID
                if (test == null) {
                    ireason = -1;
                    sreason = "can not find the ETest#" + id_s;
                } else {
//        if ("result".equals(action)) { //this moved to SUser
////            EUser user = App.user1;//TODO: for temp  null; 
//            Set<EKP> kps = null;
//            long lts = System.currentTimeMillis();
//            test.onTested(user, lts, kps);
//        } else 
                    if ("chgInfo".equals(action)) {
                        String info = request.getParameter("info");
                        test.chgInfo(info, user);
                        App.sendFailed(ireason, sreason, response);
                    } else if ("addKP".equals(action)) { //TODO: stop using this one, instead use SKP.addKP and then STest.associate.
                        //TODO: turn into async mode.
                        String desc = request.getParameter("desc");// "description of KP"; //TODO: for temp
                        ELevel level = ELevel.get_m(user.target.sys, request.getParameter("level"));
                        test.newKP_cf(desc, level, user).exceptionally(t -> {
                            t.printStackTrace();
                            return null;
                        });
                        App.sendFailed(ireason, sreason, response);
                    } else if ("associate".equals(action)) {
                        //TODO: turn into async mode.
                        String kpid_s = request.getParameter("idkp");
                        int kpid = Integer.parseInt(kpid_s);
                        EKP kp = EKP.getByID_m(kpid);
                        test.associate(kp, user);
                        App.sendFailed(ireason, sreason, response);
                    } else if ("deleteKP".equals(action)) {
                        id_s = request.getParameter("idkp");
                        id = Integer.parseInt(id_s);
                        EKP kp = EKP.getByID(id);
                        test.deleteKP(kp, user).thenAccept(tf -> {

                        }).exceptionally(t -> {
                            t.printStackTrace();
                            return null;
                        });
                        App.sendFailed(ireason, sreason, response);
                    } else {
                        throw new Exception("unknown action:" + action);
                    }
                }
            }
        } catch (Throwable t) {
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
    }// </editor-fold>

    static void serve(HttpServletResponse response, ArrayList<ETest> tests, String action) throws IOException {
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
        App.serve(response, baos);
    }

}
