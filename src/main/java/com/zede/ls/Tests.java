/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zede.ls;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Deprecated //use STest instead
public class Tests extends HttpServlet {

    String dir = "/Users/yogi/jack/japanese"; //TODO: from config

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     *
     *
     * TODO: now it is synchronous approach. better to be asynchronous approach.
     *
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        File fdir = new File(dir);
        String[] afn = fdir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });
        System.out.println("# of files:" + afn.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        StringWriter sw=new StringWriter();
        JsonFactory factory = new JsonFactory();
//            JsonGenerator g = factory.createGenerator(sw);
        JsonGenerator g = factory.createGenerator(baos, JsonEncoding.UTF8);
        g.writeStartObject();
        g.writeNumberField("ireason", 5);
//generator.writeStringField("brand", "Mercedes");
        g.writeArrayFieldStart("tests");
        for (String fn : afn) {
            g.writeStartObject();
            g.writeStringField("fn", fn);
            g.writeStringField("key", ""); //the key word of the record.
            //many other fields.
            g.writeEndObject();
        }
        g.writeEndArray();
        g.writeEndObject();
        g.flush();
        g.close();
        int size = baos.size();
        response.setContentLength(size);
        response.setContentType("application/javascript;charset=UTF-8");
        ServletOutputStream sos = response.getOutputStream();
        sos.write(baos.toByteArray());
        sos.flush();
        sos.close();
//        try (PrintWriter out = response.getWriter()) {
//            /* TODO output your page here. You may use following sample code. */
////            out.println("<!DOCTYPE html>");
////            out.println("<html>");
////            out.println("<head>");
////            out.println("<title>Servlet Tests</title>");            
////            out.println("</head>");
////            out.println("<body>");
////            out.println("<h1>Servlet Tests at " + request.getContextPath() + "</h1>");
////            out.println("</body>");
////            out.println("</html>");
//        }
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
        processRequest(request, response);
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
