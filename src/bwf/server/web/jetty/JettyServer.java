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
import org.eclipse.jetty.http.MimeTypes;

import javax.servlet.*;
import javax.servlet.http.*;

import bwf.server.web.WebServer;
import java.net.*;
import java.security.*;

/*
 * Handling automatic https redirect in jetty
 * http://stackoverflow.com/questions/26123604/redirect-from-http-to-https-automatically-in-embedded-jetty
 * 
 * 1. create http connector with HttpConfiguration using
 *   HttpConfiguration.setSecureScheme("https");
 *   HttpConfiguration.setSecurePort(sslPort);
 * 2. add SecureSchemeHandler to context
 * 3. the SecureSchemeHandler reads HttpConfiguration and makes the redirect
 * the drawback - this works only for the whole context
 * 
 * For finer-grained control use the web.ForseSecure filter, 
 * in this case the sslPort is not needed for http connector.
 * 
 * todo: Virtual hosts
 * use ContextHandler.setVirtualHosts
 */
public class JettyServer implements WebServer{
   public static int MMF_DEFAULT=-2;
   public static int MMF_DISABLE=-1;
   private int minMmf=MMF_DEFAULT;
   
   public static class ConnectorSpec{
      public String name;
      public String host;
      public int port;
      public long idle;
      public int sslPort;
      public int bufferSize;
      
      ServerConnector create(Server s){
         ServerConnector conn=new ServerConnector(s, new HttpConnectionFactory(getHttpConf()));
         conn.setName(name);
         conn.setHost(host);
         conn.setPort(port);
         conn.setIdleTimeout(idle);
         return conn;
      }
      
      protected HttpConfiguration getHttpConf(){
         HttpConfiguration conf=new HttpConfiguration();
         if(sslPort>0){
            conf.setSecureScheme("https");
            conf.setSecurePort(sslPort);
         }
         if(bufferSize>0) conf.setOutputBufferSize(bufferSize);
         return conf;
      }
   }
   public static class SslConnectorSpec extends ConnectorSpec{
      public String ksPath;
      public String ksPass;
      public String alias;
      public String keyPass;
      
      /* If
       * S - suppoted
       * E - enabled
       * I - included
       * X - excluded
       * then
       * E -> (I? S&I : E) & ~X
       * for a initial E list see signing/Ciphers.java
       */
      public String[] excludeCiphers;
      public String[] includeCiphers;
      
      /*
       */
      public String[] excludeProtocols;
      public String[] includeProtocols;
      
      ServerConnector create(Server s){
         SslContextFactory sslContextFactory = new SslContextFactory();
         sslContextFactory.setKeyStorePath(ksPath);
         sslContextFactory.setKeyStorePassword(ksPass);
         sslContextFactory.setCertAlias(alias);
         sslContextFactory.setKeyManagerPassword(keyPass);
         if(excludeProtocols!=null) sslContextFactory.setExcludeProtocols(excludeProtocols);
         if(includeProtocols!=null) sslContextFactory.setIncludeProtocols(includeProtocols);
         if(excludeCiphers!=null) sslContextFactory.setExcludeCipherSuites(excludeCiphers);
         if(includeCiphers!=null) sslContextFactory.setIncludeCipherSuites(includeCiphers);
         
         HttpConfiguration conf=getHttpConf();
         conf.addCustomizer(new SecureRequestCustomizer());
         
         ServerConnector conn= new ServerConnector(
            s, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(conf)
         );
         conn.setName(name);
         conn.setHost(host);
         conn.setPort(port);
         conn.setIdleTimeout(idle);
         return conn;
      }
   }
   
   private Server server;
   private HandlerList handlers=new HandlerList();
   private List<ConnectorSpec> connSpecs=new ArrayList<ConnectorSpec>();
   private int maxThreads;
   
   //jetty-specific
   //use MMF for static content with size >= n
   //MMF_DEFAULT - use jetty's default
   //MMF_DISABLE - no MMF
   public void setMinMmf(int n){
      minMmf=n;
   }
   
   public void setMaxThreads(int n){
      maxThreads=n;
   }
   
   public ConnectorSpec addConnector(){
      ConnectorSpec spec=new ConnectorSpec();
      connSpecs.add(spec);
      return spec;
   }
   
   public void addConnector(String host, int port, long idle){
      addConnector(host, port, idle, 0, 0);
   }
   
   public void addConnector(String host, int port, long idle, int bufferSize){
      addConnector(host, port, idle, bufferSize, 0);
   }
   
   public void addConnector(
      String host, int port, long idle, int bufferSize, int sslPort
   ){
      ConnectorSpec spec=addConnector();
      spec.host=host;
      spec.port=port;
      spec.idle=idle;
      spec.bufferSize=bufferSize;
      spec.sslPort=sslPort;
   }
   
   public void addSslConnector(
      String host, int port, long idle, 
      String ksPath, String ksPass, String alias, String keyPass
   ){
      SslConnectorSpec spec=addSslConnector(host, port, idle);
      spec.ksPath=ksPath;
      spec.ksPass=ksPass;
      spec.alias=alias;
      spec.keyPass=keyPass;
   }
   
   public SslConnectorSpec addSslConnector(String host, int port, long idle){
      SslConnectorSpec spec=addSslConnector();
      spec.host=host;
      spec.port=port;
      spec.idle=idle;
      return spec;
   }
   
   public SslConnectorSpec addSslConnector(){
      SslConnectorSpec spec=new SslConnectorSpec();
      connSpecs.add(spec);
      return spec;
   }
   
   //todo: multiple filters
   public RContext addResourceContext(
      String uri, String dir, boolean dirListed
   ){
      final ContextHandler resContext=new ContextHandler();
      resContext.setContextPath(uri);
      final ResourceHandler res=new ResourceHandler();
      if(minMmf!=MMF_DEFAULT){
         res.setMinMemoryMappedContentLength(minMmf);
      }
      res.setDirectoriesListed(dirListed);
      res.setResourceBase(dir);
      MimeTypes mt=res.getMimeTypes();
      if(mt==null) mt=new MimeTypes();
      mt.addMimeMapping("js","application/javascript");
      res.setMimeTypes(mt);
      resContext.setHandler(res);
      handlers.addHandler(resContext);
      return new RContext(){
         public void insertHandler(final RequestHandler rh){
            final Handler nestedHandler=resContext.getHandler();
            resContext.setHandler(new AbstractHandler(){
               @Override
               public void handle(
                  String target, Request baseRequest,
                  HttpServletRequest request, HttpServletResponse response
               ) throws IOException, ServletException{
                  if(rh.handle(request,response)){
                     baseRequest.setHandled(true);
                     return;
                  }
                  nestedHandler.handle(target, baseRequest, request, response);
               }
            });
         }
         public void setWelcomeFiles(String[] files){
            res.setWelcomeFiles(files);
         }
      };
   }
   
   public SContext addServletContext(String uri){
      final ServletContextHandler servlets=new ServletContextHandler();
      servlets.setContextPath(uri);
      SessionManager manager=new HashSessionManager();
      final SessionHandler sessionHandler=new SessionHandler(manager);
      servlets.setSessionHandler(sessionHandler);
      handlers.addHandler(servlets);
      return new SContext(){
         public Parameters addServlet(String path, String name, HttpServlet servlet, boolean async){
            final ServletHolder sh=new ServletHolder(name, servlet);
            sh.setAsyncSupported(async);
            servlets.addServlet(sh, path);
            return new Parameters(){
               public Parameters add(String key, String value){
                  sh.setInitParameter(key, value);
                  return this;
               }
            };
         }
         public Parameters addFilter(String path, Filter filter, boolean async){
            final FilterHolder fh=new FilterHolder(filter);
            fh.setAsyncSupported(async);
            servlets.addFilter(fh, path, EnumSet.of(DispatcherType.REQUEST));
            return new Parameters(){
               public Parameters add(String key, String value){
                  fh.setInitParameter(key, value);
                  return this;
               }
            };
         }
         public void addSessionListener(HttpSessionListener sl){
            sessionHandler.addEventListener(sl);
         }
         public void insertHandler(final RequestHandler rh){
            servlets.insertHandler(new HandlerWrapper(){
               @Override
               public void handle(
                  String target, Request baseRequest,
                  HttpServletRequest request, HttpServletResponse response
               ) throws IOException, ServletException{
                  if(rh.handle(request,response)){
                     baseRequest.setHandled(true);
                     return;
                  }
                  super.handle(target, baseRequest, request, response);
               }
            });
         }
      };
   }
   
   public void start() throws Exception{
      QueuedThreadPool threadPool = new QueuedThreadPool();
      if(maxThreads>0) threadPool.setMaxThreads(100);
      server=new Server(threadPool);
      for(ConnectorSpec cs:connSpecs){
         server.addConnector(cs.create(server));
      }
      server.setHandler(handlers);
      server.start();
   }
   
   public void join() throws InterruptedException{
      server.join();
   }
   
   public static void main(String[] args) throws Exception{
      WebServer server=new JettyServer();
      server.setMaxThreads(20);
      server.addConnector("localhost",8080,10*60000,0,0);
      
      WebServer.Context res=server.addResourceContext("/static","../../wwwroot",true);
      res.insertHandler(new WebServer.RequestHandler(){
         public boolean handle(
            HttpServletRequest req, HttpServletResponse resp
         ) throws IOException, ServletException{
            System.out.println("ResourceContext.RequestHandler.handle("+req+")");
            resp.setHeader("Access-Control-Allow-Origin","*");
            return false;
         }
      });
      
      SContext c=server.addServletContext("/servletcontext");
      c.addServlet("/hello","hello servlet",new HttpServlet(){
         @Override
         public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            log(""+req);
            Enumeration<String> e=req.getHeaderNames();
            while(e.hasMoreElements()){
               String h=e.nextElement();
               System.out.println("  "+h+": "+req.getHeader(h));
            }
            String authStr=req.getHeader("Authorization");
            if(authStr==null){
               resp.addHeader("WWW-Authenticate", "Basic realm=\""+getServletName()+"\"");
               resp.sendError(resp.SC_UNAUTHORIZED);
               return;
            }
            authStr=new String(Base64.getDecoder().decode(authStr.split("\\s+")[1]),"Latin1");
            resp.setContentType("text/plain");
            resp.getWriter().write("Hello!\nAuth string="+authStr);
//            HttpSession s=req.getSession();
//            if(s.getAttribute("user")==null){
//               s.setMaxInactiveInterval(30);
//               s.setAttribute("user","me");
//            }
         }
      },false);
      
      Filter f=new Filter(){
         public void doFilter(
            ServletRequest req0, ServletResponse resp0, FilterChain chain
         ) throws IOException, ServletException{
            HttpServletRequest req=(HttpServletRequest)req0;
            System.out.println("JettyServer.doFilter("+req.getPathInfo()+")");
            System.out.println("  user="+req.getUserPrincipal());
            System.out.println("  method="+req.getMethod());
            System.out.println("  pathInfo="+req.getPathInfo());
            System.out.println("  servletPath="+req.getServletPath());
            System.out.println("  contextPath="+req.getContextPath());
            System.out.println("  uri="+req.getRequestURI());
         }
         public void  destroy(){
            System.out.println("JettyServer.filter.destroy()");
         }
         public void init(FilterConfig filterConfig) throws ServletException{
            System.out.println("JettyServer.filter.init()");
         }
      };
      c.addFilter("/filter/a",f,false);
      c.addFilter("/filter/b/*",f,false);
      
      c.addSessionListener(new HttpSessionListener(){
         public void sessionCreated(HttpSessionEvent se){
            System.out.println("sessionCreated("+se+")");
         }
         public void sessionDestroyed(HttpSessionEvent se){
            System.out.println("sessionDestroyed("+se+")");
         }
      });
      
      c.insertHandler(new WebServer.RequestHandler(){
         public boolean handle(
            HttpServletRequest request, HttpServletResponse response
         ) throws IOException, ServletException{
            System.out.println("ServletContext.RequestHandler.handle("+request+")");
            return false;
         }
      });
      
      server.start();
      server.join();
//      URLConnection uc=new URL("http://localhost:8080/servlet/hello").openConnection();
//      System.out.println(uc.getContent());
   }
}
