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
import javax.servlet.http.HttpSession;

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

            HttpSession session = request.getSession();
            EUser user = (EUser) session.getAttribute("user");
            if (user == null) {
                ireason = -1;
                sreason = "not authenticated";
            } else {
//        EKP kp=null;
                String action = request.getParameter("act");
                HashSet<EKP> kps = new HashSet<>();
                if ("kps".equals(action)) {
                    String category = request.getParameter("c");
                    if ("4level".equals(category)) {
                        String sysName = request.getParameter("sys");
                        ELevelSystem sys = ELevelSystem.getByName(sysName);
                        String level_s = request.getParameter("level");
                        if ("0.0".equals(level_s)) { //nolevel
                            EKP.noELevel(sys, kps);
                        } else {
                            ELevel level = ELevel.get_m(sys, level_s);
                            if (level != null) {
//                                kps = new ArrayList<>(level.kps.size());
                                for (Integer kpid : level.kps) {
//TODO: level.kps might contain an EKP, but whose level is not ELevel. let's call them "missing" or "conflicts"
//    how to deal with this?
//  we list and present them(shows their levels)
//   and allow the user to confirm their levels.

                                    try {
                                        EKP kp = EKP.getByID_m(kpid, true);
                                        kps.add(kp); //true to ensure it is loaded from EKPbundle, rather than the one from ETest.
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                }
                            } else {
                                throw new Exception("level is null for " + sysName + ":" + level_s);
                            }
                        }
                    } else if ("notest".equals(category)) {
                        EKP.noETest(kps);
                    } else {
                        throw new Exception("unknow category:" + category);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    JsonGenerator g = App.getJSONgenerator(baos);
                    g.writeStartObject();
                    g.writeStringField("category", category);
                    g.writeObjectFieldStart("kps"); //g.writeArrayFieldStart("kps");
                    HashSet<EKP> set = new HashSet<>();
                    set.addAll(kps);
                    json(g, set);
                    g.writeEndObject(); //g.writeEndArray();
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    App.serve(response, baos);
                } else if ("fixHalfLevel".equals(action)) {
                    //TODO: turn this into async mode
//fix relation "half level"(EKP has refer to ELevel, but ELevel does not refer to the EKP)
                    String sysName = request.getParameter("sys");
//                    HashSet<EKP> halvesKP = new HashSet<>();
                    HashSet<ELevel> changed = new HashSet<>();
                    int n = ELevelSystem.fix_EKP_ELevel(sysName, changed); //, halvesKP
                    HashSet<ELevelSystem> set = new HashSet<>();
                    for (ELevel level : changed) {
                        set.add(level.sys);
                    }
                    for (ELevelSystem sys : set) {
                        sys.save();
                    }
//                    App.sendFailed(ireason, sreason, response);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    JsonGenerator g = App.getJSONgenerator(baos);
                    g.writeStartObject();
                    g.writeNumberField("fixed", n);
                    g.writeNumberField("levels", changed.size());
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    App.serve(response, baos);
                } else {
                    throw new Exception("unknow act:" + action);
                }
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
            EUser user = (EUser) session.getAttribute("user");
            if (user == null) {
                ireason = -1;
                sreason = "not authenticated";
            } else {
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
                    EKP.getByID_cf(id, true).thenApply((EKP kp) -> {
                        kp.delete();
                        return true;
                    }).exceptionally((Throwable t) -> {
                        t.printStackTrace();
                        return null;
                    });
                    App.sendFailed(ireason, sreason, response);
                } else if ("addKP".equals(action)) { //moved from STest.addKP
                    //TODO: turn into async mode.
                    String desc = request.getParameter("desc");// "description of KP"; //TODO: for temp
                    ELevel level = ELevel.get_m(user.target.sys, request.getParameter("level"));
                    EKP.newKP_cf(desc, level, user).exceptionally(t -> {
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
                } else if ("searchKPs".equals(action)) { //TODO: not implemented yet.
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
                    

                } else {
                    throw new Exception("unknown action:" + action);
                }
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
