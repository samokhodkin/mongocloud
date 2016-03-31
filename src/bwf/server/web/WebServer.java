package bwf.server.web;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

public interface WebServer{
   public interface Parameters{
      public Parameters add(String key, String value);
   }
   public interface RequestHandler{
      //return true to stop further preocessing
      public boolean handle(
         HttpServletRequest request, HttpServletResponse response
      ) throws IOException, ServletException;
   }
   public interface Context{
      public void insertHandler(RequestHandler rh);
   }
   public interface RContext extends Context{
      public void setWelcomeFiles(String[] files);
   }
   public interface SContext extends Context{
      public Parameters addServlet(String path, String name, HttpServlet servlet, boolean async);
      public Parameters addFilter(String path, Filter filter, boolean async);
      public void addSessionListener(HttpSessionListener sl);
   }
   
   public void setMaxThreads(int n);
   public void addConnector(String host, int port, long idle);
   public void addConnector(String host, int port, long idle, int bufferSize);
   public void addConnector(String host, int port, long idle, int bufferSize, int sslPort);
   public void addSslConnector(
      String host, int port, long idle, 
      String ksPath, String ksPass, String alias, String keyPass
   );
   public RContext addResourceContext(String uri, String dir, boolean dirListed);
   public SContext addServletContext(String uri);
   
   public void start() throws Exception;
   public void join() throws InterruptedException;
}