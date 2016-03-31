package bwf.server.web.jetty;

import java.util.*;
import java.io.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.session.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.http.HttpVersion;

import javax.servlet.*;
import javax.servlet.http.*;

public class JettySsl{
   public static void main(String[] args) throws Exception{
      QueuedThreadPool threadPool = new QueuedThreadPool();
      threadPool.setMaxThreads(100);
      Server server=new Server(threadPool);
      
      HttpConfiguration http_config = new HttpConfiguration();
      http_config.setSecureScheme("https");
      http_config.setSecurePort(8443);
      http_config.setOutputBufferSize(32768);
      
      ServerConnector http=new ServerConnector(
         server, new HttpConnectionFactory(http_config)
      );
      http.setHost("localhost");
      http.setPort(8080);
      http.setIdleTimeout(30000);
      
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(
         "d:/serge/my_projects/mywebserver/signing/keystore.jks"
      ); //from home/java/classes to home/bin
      sslContextFactory.setKeyStorePassword("123456"); //see makecert
      sslContextFactory.setCertAlias("samokhodkin.tk");         //see makecert
      sslContextFactory.setKeyManagerPassword("123456"); //key pass, see localhost.in
      
      ServerConnector https = new ServerConnector(
         server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
         new HttpConnectionFactory()
      );
      https.setPort(8443);
      https.setIdleTimeout(30000);
      
      server.addConnector(http);
      server.addConnector(https);
      
      HandlerList handlers=new HandlerList();
      
      ContextHandler resContext=new ContextHandler();
      resContext.setContextPath("/static");
      ResourceHandler res=new ResourceHandler();
      res.setDirectoriesListed(true);
      res.setResourceBase("../../wwwroot");
      resContext.setHandler(res);
      handlers.addHandler(resContext);
      
      HttpServlet helloServlet=new HttpServlet(){
         @Override
         public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            resp.getWriter().write("Hello!");
         }
      };
      HttpServlet sessionServlet=new HttpServlet(){
         @Override
         public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            HttpSession s=req.getSession();
            String u=(String)s.getAttribute("user");
            if(u==null){
               System.out.println("registering user");
               s.setAttribute("user","me");
               s.setMaxInactiveInterval(30);
            }
            else{
               System.out.println("user is "+u);
            }
            resp.getWriter().write("Hello, "+u);
         }
      };
      ServletContextHandler servlets=new ServletContextHandler();
      servlets.setContextPath("/servlets");
      SessionManager manager=new HashSessionManager();
      SessionHandler sessionHandler=new SessionHandler(manager);
      sessionHandler.addEventListener(new HttpSessionListener(){
         public void sessionCreated(HttpSessionEvent se){
            System.out.println("sessionCreated("+se+")");
         }
         public void sessionDestroyed(HttpSessionEvent se){
            System.out.println("sessionDestroyed("+se+")");
         }
      });
      servlets.setSessionHandler(sessionHandler);
      servlets.addServlet(new ServletHolder("hello servlet", helloServlet), "/hello");
      servlets.addServlet(new ServletHolder("session servlet", sessionServlet), "/session");
      handlers.addHandler(servlets);
      
      handlers.addHandler(new DefaultHandler());
      
      server.setHandler(handlers);
      server.start();
      server.join();
   }
}
