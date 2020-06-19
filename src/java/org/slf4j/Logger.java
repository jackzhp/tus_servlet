/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.slf4j;

/**
 *
 * @author yogi
 */
public class Logger {

    public void error(String string, Throwable e) {
        System.out.println(string);//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        e.printStackTrace(System.out);
    }

    public void info(String string) {
System.out.println(string);//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void debug(String string) {
        System.out.println(string);//throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void error(String tmp) {
    System.out.println(tmp);//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void warn(String string) {
    System.out.println(string);//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
