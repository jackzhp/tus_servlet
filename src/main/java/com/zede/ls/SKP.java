package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import static com.zede.ls.EKPbundle.json;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class SKP extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
//    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//
//        EUser user = null; //TODO: ...
////        EKP kp=null;
//        String action = request.getParameter("act");
//        if ("chgKPDesc".equals(action)) {
//            String id_s = request.getParameter("kpid");
//            int id = Integer.parseInt(id_s);
//            String desc = request.getParameter("desc");
//            EKP.getByID_cf(id).thenApply((EKP kp) -> {
//                kp.chgDesc(desc, user);
//                return true;
//            }).exceptionally((Throwable t) -> {
//                t.printStackTrace();
//                return null;
//            });
//        } else if ("chgKPlevel".equals(action)) {
//
//        } else {
////            System.
//        }
//        response.setContentType("text/html;charset=UTF-8");
//        try (PrintWriter out = response.getWriter()) {
//            /* TODO output your page here. You may use following sample code. */
//            out.println("<!DOCTYPE html>");
//            out.println("<html>");
//            out.println("<head>");
//            out.println("<title>Servlet SKP</title>");
//            out.println("</head>");
//            out.println("<body>");
//            out.println("<h1>Servlet SKP at " + request.getContextPath() + "</h1>");
//            out.println("</body>");
//            out.println("</html>");
//        }
//    }
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
        String sreason = "";
        try {
            EUser user = null; //TODO: ... check authentication, check role.
//        EKP kp=null;
            String action = request.getParameter("act");
            ArrayList<EKP> kps = null;
            if ("nolevel".equals(action)) {
                String sysName = request.getParameter("sys");
                ELevelSystem sys = ELevelSystem.getByName(sysName);
                kps = EKP.noELevel(sys);
            } else if ("notest".equals(action)) {
                kps = EKP.noETest();
            } else {
                throw new Exception("should not get here");
//            System.
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator g = App.getJSONgenerator(baos);
            g.writeStartObject();
            g.writeStringField("category", action);
            g.writeObjectFieldStart("kps"); //g.writeArrayFieldStart("kps");
            HashSet<EKP> set = new HashSet<>();
            set.addAll(kps);
            json(g, set);
            g.writeEndObject(); //g.writeEndArray();
            g.writeEndObject();
            g.flush();
            g.close();
            App.serve(response, baos);
        } catch (Throwable t) {
            ireason = -1;
            sreason = "uncaughted exception:" + t.getMessage();
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
            EUser user = null; //TODO: ...
//        EKP kp=null;
            String action = request.getParameter("act");
            if ("chgKPdesc".equals(action)) {
                //TODO: change to async mode.
                String id_s = request.getParameter("idkp");
                int id = Integer.parseInt(id_s);
                String desc = request.getParameter("desc");
                EKP.getByID_cf(id, true).thenCompose((EKP kp) -> {
                    return kp.chgDesc_cf(desc, user);
                }).exceptionally((Throwable t) -> {
                    t.printStackTrace();
                    return null;
                });
                App.sendFailed(ireason, sreason, response);
            } else if ("delete".equals(action)) {
                String id_s = request.getParameter("idkp");
                int id = Integer.parseInt(id_s);
                EKP.getByID_cf(id,true).thenApply((EKP kp) -> {
                    kp.delete();
                    return true;
                }).exceptionally((Throwable t) -> {
                    t.printStackTrace();
                    return null;
                });
                App.sendFailed(ireason, sreason, response);
            } else if ("chgKPlevel".equals(action)) {
                String id_s = request.getParameter("idkp");
                int id = Integer.parseInt(id_s);
                String sysName = request.getParameter("sys");
                String level_s = request.getParameter("level");
//                ELevelSystem sys=ELevelSystem.getByName(sysName);
                ELevel level = ELevel.get_m(sysName, level_s);//sys.getLevel_m(major, minor);
                EKP.getByID_cf(id).thenApply((EKP kp) -> {
                    kp.set(level);
                    return true;
                }).exceptionally((Throwable t) -> {
                    t.printStackTrace();
                    return null;
                });
                App.sendFailed(ireason, sreason, response);
            } else if ("mergeKPs".equals(action)) {
                String kpids = request.getParameter("kpids");
                EKP.merge(kpids).get();
                App.sendFailed(ireason, sreason, response);
            } else {
                throw new Exception("unknown action:" + action);
            }
        } catch (Throwable t) {
            ireason = -1;
            sreason = "uncaughted exception:" + t.getMessage();
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
