package mongocloud.test;

/**
 * @(#)FileStrorageTest.java
 *
 *
 * @author 
 * @version 1.00 2015/12/3
 */

import javax.servlet.*;
import java.security.*;
import javax.servlet.http.*;

import bwf.server.web.*;
import bwf.server.web.jetty.*;

import mongocloud.server.servlet.HttpUtil;

import net.sf.webdav.*;
import net.sf.webdav.exceptions.*;

import java.io.*;

public class FileStrorageTest{
   public static void main(String[] args) throws Exception{
      //System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
      //System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX+"net.sf.webdav.LocalFileSystemStore", "TRACE");
      System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX+"net.sf.webdav", "TRACE");
            
      WebServer server=new JettyServer();
      server.addConnector("0.0.0.0", 8080, 10*60000, 0, 0);
      WebServer.SContext c=server.addServletContext("/");
      HttpServlet servlet=new WebdavServlet(){
         @Override
         public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException{
System.out.println("FileStrorageTest.main.servlet.service() calling withBasicPrincipal");
            HttpServletRequest req1=HttpUtil.withBasicPrincipal(req);
System.out.println("  req1.getUserPrincipal() -> "+req1.getUserPrincipal());
            super.service(req1, resp);
         }
      };
      WebServer.Parameters p=c.addServlet("/*", "webdav servlet", servlet, false);
      //p.add("ResourceHandlerImplementation", "net.sf.webdav.LocalFileSystemStore");
      p.add("ResourceHandlerImplementation", "bwf.test.FileStrorageTest$MyStorage");
      p.add("rootpath", "d:/tmp/webdav");
      
      server.start();
      server.join();
   }
   
   public static class MyStorage extends LocalFileSystemStore{
      public MyStorage(File f){
         super(f);
      }
      
      @Override
      public ITransaction begin(Principal p) throws WebdavException{
System.out.println("MyStorage.begin("+p+")");
//         if(p==null) throw new UnauthenticatedException("Login please");
//if(p!=null && p.getName().equals("aaa")) throw new UnauthenticatedException("Login please");
         return null;
      }
   }
}
