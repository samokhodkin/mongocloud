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

public class HandlersTree{
   public static void main(String[] args) throws Exception{
      QueuedThreadPool threadPool = new QueuedThreadPool();
      threadPool.setMaxThreads(100);
      Server server=new Server(threadPool);
      
      ServerConnector http=new ServerConnector(server);//, new HttpConnectionFactory(http_config));
      http.setHost("localhost");
      http.setPort(8080);
      http.setIdleTimeout(30000);
      server.addConnector(http);
      
      class MyHandler extends AbstractHandler{
         String name;
         boolean stop;
         
         MyHandler(String name, boolean stop){
            this.name=name;
            this.stop=stop;
         }
         
         @Override
         public void handle(
            String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response
         ) throws IOException, ServletException{
            Writer out=response.getWriter();
            out.write(name);
            out.write("\n");
            baseRequest.setHandled(stop);
         }
      };
      
      //we'll try to build the tree:
      //h1+h2 at context1,
      //h4+h5+h6 at context2
      //h7 at context3
      
      HandlerList contextList=new HandlerList();
      
      HandlerList handlers1=new HandlerList();
      //stop handling at h2
      handlers1.addHandler(new MyHandler("that's handler-1", false));
      handlers1.addHandler(new MyHandler("that's handler-2", true));
      handlers1.addHandler(new MyHandler("that's handler-3", false));
      
      //don't stop handling
      //upd. 
      HandlerList handlers2=new HandlerList();
      handlers2.addHandler(new MyHandler("that's handler-4", false));
      handlers2.addHandler(new MyHandler("that's handler-5", false));
      handlers2.addHandler(new MyHandler("that's handler-6", false));
      
      //stop handling at h7
      HandlerList handlers3=new HandlerList();
      handlers2.addHandler(new MyHandler("that's handler-7", true));
      handlers2.addHandler(new MyHandler("that's handler-8", false));
      handlers2.addHandler(new MyHandler("that's handler-9", false));
      
      ContextHandler context1=new ContextHandler();
      context1.setContextPath("/context1");
      context1.setHandler(handlers1);
      contextList.addHandler(context1);
      
      ContextHandler context2=new ContextHandler();
      context2.setContextPath("/context2");
      context2.setHandler(handlers2);
      contextList.addHandler(context2);
      
      ContextHandler context3=new ContextHandler();
      context3.setContextPath("/context3");
      context3.setHandler(handlers3);
      contextList.addHandler(context3);
      
      //test ContextHandler.insertHandler()
      //1. set a base handler
      //2. insert a wrapper.
      //HandlerWrapper's flow control:
      //call baseRequest.setHandled(true) - stop further processing
      //call super.handle(..) - pass control to the nested handler 
      //do nothing - skip the nested handler and pass control to a next sibling
      ContextHandler context4=new ContextHandler();
      context4.setContextPath("/context4");
      HandlerWrapper wrapper=new HandlerWrapper(){
         @Override
         public void handle(
            String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response
         ) throws IOException, ServletException{
            Writer out=response.getWriter();
            out.write("i'm a wrapper!");
            out.write("\n");
            super.handle(target, baseRequest, request, response);
            //baseRequest.setHandled(true);
         }
      };
      wrapper.setHandler(new MyHandler("i'm the nested handler", true));
      context4.setHandler(wrapper);
      contextList.addHandler(context4);
      
      //try to chain a servlet handler and a resource handler
      ServletContextHandler context5=new ServletContextHandler();
      context5.setContextPath("/context5");
      context5.addServlet(new ServletHolder("hello servlet", new HttpServlet(){
         public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            Writer out=resp.getWriter();
            out.write("hello");
            out.write("\n");
         }
      }), "/hello");
      //?
      ((HandlerWrapper)context5.getHandler()).setHandler(new MyHandler("i'm chained to servlet context", true));
      contextList.addHandler(context5);
      //DOESN'T WORK!!! the ServletContextHandler doesn't allow to change the nested handler; 
      //the nested ServletHandler can't be used alone, assumes it's in a ServletContextHandler
      
      //try to chain a custom handler before the res. handler 
      ContextHandler context6=new ContextHandler();
      context6.setContextPath("/context6");
      ResourceHandler res=new ResourceHandler();
      res.setDirectoriesListed(true);
      res.setResourceBase("../../wwwroot");
      HandlerWrapper wrapper6=new HandlerWrapper(){
         @Override
         public void handle(
            String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response
         ) throws IOException, ServletException{
            System.out.println("I'm a wrapper around resource! req="+request.getRequestURL());
            super.handle(target, baseRequest, request, response);
            //baseRequest.setHandled(true);
         }
      };
      wrapper6.setHandler(res);
      context6.setHandler(wrapper6);
      contextList.addHandler(context6);
      //OK
      
      server.setHandler(contextList);
      
      server.start();
      server.join();
   }
}
