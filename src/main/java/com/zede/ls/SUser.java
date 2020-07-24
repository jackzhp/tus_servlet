package com.zede.ls;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class SUser extends HttpServlet {

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
            out.println("<title>Servlet SUser</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet SUser at " + request.getContextPath() + "</h1>");
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
        int ireason = 0;
        String sreason = null;
        HttpSession session = request.getSession();
        //TODO: do not need to getByID, user is a session object once authenticated.
//        EUser user = EUser.getByID(1); // App.user1;//for temp //TODO: the current user.
        try {
            String action = request.getParameter("act");
            if ("nonce".equals(action)) {
                String username = request.getParameter("username");
                String nonce = "asdasdf"; //TODO: random
                HashMap<String, String> passes = EUser.getUserNames(username).get();
                if (passes.isEmpty()) {
                    throw new Exception("unknown username:" + username);
                }
                session.setAttribute("nonceAuthenticating", nonce);
                session.setAttribute("noncePasses", passes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JsonGenerator g = App.getJSONgenerator(baos);
                g.writeStartObject();
                g.writeStringField("nonce", nonce);
                g.writeArrayFieldStart("pks");
                Collection<String> c = passes.values();
                for (String p : c) {
                    g.writeString(p.split(":")[0]);
                }
                g.writeEndArray();
                g.writeEndObject();
                g.flush();
                g.close();
                App.serve(response, baos);
//                EUser.preparePasses(passes, nonce);
            } else { //in this branch, the user must be authenticated.
                EUser user = (EUser) session.getAttribute("user");
                if (user == null) {
                    ireason = -1;
                    sreason = "not authenticated";
                } else {
                    if ("tests".equals(action)) {
                        boolean reviewOnly = "true".equals(request.getParameter("reviewOnly"));
                        ETest[] tests = user.getTest(reviewOnly).get(); //TODO: async is complicated, so for temp.
//                    ETest test = tests[0];
//                    File f = test.getFile(false);
//                    App.serve(response, f);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        JsonFactory factory = new JsonFactory();
                        JsonGenerator g = App.getJSONgenerator(baos);
                        g.writeStartObject();
//                    g.writeNumberField("ireason", 5);
//generator.writeStringField("brand", "Mercedes");
                        g.writeArrayFieldStart("tests");
                        if (false) {
//        for (String fn : afn) {
//            g.writeStartObject();
//            g.writeStringField("fn", fn);
//            g.writeStringField("key", ""); //the key word of the record.
//            //many other fields.
//            g.writeEndObject();
//        }
                        } else {
                            for (ETest test : tests) {
                                test.json(g);
                            }
                        }
                        g.writeEndArray();
                        g.writeEndObject();
                        g.flush();
                        g.close();
                        App.serve(response, baos);
                    } else if ("test".equals(action)) {
                        boolean reviewOnly = "true".equals(request.getParameter("reviewOnly"));
                        ETest[] tests = user.getTest(reviewOnly).get(); //TODO: async is complicated, so for temp.
                        ETest test = tests[0];
                        File f = test.getFile(false);
                        App.serve(response, f);
                    } else if ("level".equals(action)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        user.prepareToServeLevel(baos);
                        App.serve(response, baos);
                    } else {
                        ireason = -1;
                        sreason = "unkown action:" + action;
                    }
                }
            }
        } catch (Throwable t) {
            ireason = -1;
            sreason = "uncaught exception:" + t.getMessage();
            t.printStackTrace();
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
        String sreason = null;
        try {
            //TODO: do not need to getByID, user is a session object once authenticated.
            HttpSession session = request.getSession();
            EUser user = (EUser) session.getAttribute("user");// EUser.getByID(1); // App.user1;//for temp //TODO: the current user.
            String action = request.getParameter("act");// "result";
            if ("authenticate".equals(action)) { //copied from STest
//                String username = request.getParameter("username");
                EUser.deauthenticate(user);
                session.removeAttribute("user");
                String sig = request.getParameter("sig");
                String pk = request.getParameter("pk");
                @SuppressWarnings("unchecked")
                HashMap<String, String> passes = (HashMap<String, String>) session.getAttribute("noncePasses");
                String nonce = (String) session.getAttribute("nonceAuthenticating");
                user = EUser.authenticate(passes, nonce, pk, sig);
                if (user != null) {
                    session.setAttribute("user", user);
                    App.sendFailed(ireason, sreason, response);
                } else {
                    throw new Exception("not authenticated");
                }
            } else {
                if (user == null) {
                    ireason = -1;
                    sreason = "not authenticated";
                } else {
                    if ("result".equals(action)) {
                        String id_s = request.getParameter("idtest");
                        int testid = Integer.parseInt(id_s);
//            EUser user = App.user1;//TODO: for temp  null; 
                        //Set<EKP> bads = null; //from request.
                        String bads_s = request.getParameter("bads");
                        int[] bads = App.getInts(bads_s);
                        long lts = System.currentTimeMillis();
                        //test.onTested(user, lts, bads);
                        user.onTested(lts, testid, bads);
                        App.sendFailed(ireason, sreason, response);
                    } else if ("deauthenticate".equals(action)) { //copied from STest
//                String username = request.getParameter("username");
                        EUser.deauthenticate(user);
                        session.removeAttribute("user");
                        App.sendFailed(ireason, sreason, response);
//        } else if ("addKP".equals(action)) { //copied from STest
//            String desc = request.getParameter("desc");// "description of KP"; //TODO: for temp
//            String grade = null;
//            test.newKP(desc, grade, user);
                    } else {
                        System.out.println("unknown action in SUser:" + action);
                    }
                }
            }
        } catch (Throwable t) {
            ireason = -1;
            sreason = "uncaughed exception:" + t.getMessage();
            t.printStackTrace();
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

}
