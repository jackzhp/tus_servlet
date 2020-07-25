/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zede.ls;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * this the class to serve audio file.s
 *
 *
 */
public class SFiles extends HttpServlet {

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
            out.println("<title>nothing</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>nothing</h1>");
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
        HttpSession session = request.getSession();
        EUser user = (EUser) session.getAttribute("user");
        int ireason = 0;
        String sreason = "";
        try {
            if (user == null) {
                ireason = -1;
                sreason = "not authenticated";
            } else {
                File f = null;
                String mime = null, fn = null;
                String action = request.getParameter("act");
                if ("audio".equals(action)) {
                    fn = request.getParameter("fn");
                    if (fn == null || fn.isEmpty()) {
                        ireason = -1;
                        sreason = "fn is null or empty";
                    } else {
                        if (false) {
                            String s = request.getQueryString();
//        Hashtable<String, String[]> map=HttpUtils.parseQueryString(s);
//         request
                            if (s == null) {
                            } else {
                                System.out.println("q:" + s);
                                //fn=fileName
                                File fdir = App.dirAudio();
                                String[] as = s.split("=");
                                if (as.length != 2) {
                                } else {
                                    fn = as[1];
                                }
                            }
                        } else {
                        }
                        if (fn != null) {
                            f = new File(App.dirAudio(), fn);
                            mime = "application/octet-stream";
                        } else {
                        }
                    }
                } else if ("levels".equals(action)) {
                    String sysName = request.getParameter("sys");
                    f = ELevelSystem.getFile(sysName, false);
                    fn = f.getName();
                    mime = "application/javascript;charset=UTF-8";
                } else {
                    ireason = -1;
                    sreason = "unknown act:" + action;
                }
                if (f != null) {
                    if (mime == null) {
                        ServletContext servletContext = request.getServletContext();
                        mime = servletContext.getMimeType(fn);
                    }
                    response.setContentType(mime);
                    App.serve(response, f, mime);
                } else {
                    throw new Exception("file is null:act:" + action);
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
        processRequest(request, response);
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
