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
                String s = request.getQueryString();
//        Hashtable<String, String[]> map=HttpUtils.parseQueryString(s);
//         request
                if (s == null) {
                } else {
                    System.out.println("q:" + s);
                    //fn=fileName
                    File fdir = new File(dir);
                    String[] as = s.split("=");
                    if (as.length != 2) {
                    } else {
                        String fn = as[1];
                        File f = new File(fdir, fn);
                        String mime;
                        if (false) {
                            ServletContext servletContext = request.getServletContext();
                            mime = servletContext.getMimeType(fn);
                        } else {
                            mime = "application/octet-stream";
                        }
                        response.setContentType(mime);
                        App.serve(response, f, mime);
//                byte[] bytes = new byte[4096];
//                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
//                ServletOutputStream sos = response.getOutputStream();
//                while (true) {
//                    int len = bis.read(bytes);
//                    if (len > 0) {
//                        sos.write(bytes, 0, len);
//                    } else if (len == -1) {
//                        break;
//                    }
//                }
//                sos.flush();
//                sos.close();
//                bis.close();
                        return;
                    }
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
