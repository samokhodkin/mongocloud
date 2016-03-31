package mongocloud.server.servlet;

/**
 * MongoFs servlet filter; a wrapper around net.sf.webdav.WebdavServlet
 * Needs a MongFs instance.
 * 
 * configurable via injection
 * used in standalone demo
 */

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import mongocloud.server.fs.*;

import net.sf.webdav.*;

public class FsFilter implements Filter{
   private FilterConfig config;
   
   private final WebdavServlet servlet=new WebdavServlet(){
      //wire ServletContext
      public ServletContext getServletContext(){
         return config.getServletContext();
      }
   };
   
   public MongoFs fs; //injected, not null
   public String prefix; //injected, null is ok
   
   public void doFilter(
      ServletRequest req, ServletResponse resp, FilterChain chain
   ) throws IOException, ServletException{
//System.out.println("====================FsFilter.doFilter("+req+")");
//HttpServletRequest hreq=(HttpServletRequest)req;
//System.out.println("  url: "+hreq.getRequestURL());
//System.out.println("  uri: "+hreq.getRequestURI());
//System.out.println("  servletPath: "+hreq.getServletPath());
//System.out.println("  pathInfo: "+hreq.getPathInfo());

      //servlet.service(req0,resp0);
      //WebdavServlet uses pathInfo as an object path
      //in filters the pathInfo is null
      //so we substitute it with servletPath
      //UPD. but pi must be in sync with sp: sp+pi=full path
      HttpServletRequestWrapper wrapper=new HttpServletRequestWrapper((HttpServletRequest)req){
         public String getServletPath(){
            return prefix;
         }
         public String getPathInfo(){
            String sp=super.getServletPath();
            if(prefix==null || !sp.startsWith(prefix)) return sp;
            return sp.substring(prefix.length());
         }
      };
//System.out.println("  wrapper.pathInfo: "+wrapper.getPathInfo());
      servlet.service(wrapper,resp);
   }
   public void  destroy(){
      //do nothing
   }
   public void init(FilterConfig filterConfig) throws ServletException{
System.out.println("********* FsFilter.init() **********");
System.out.println("  fs="+fs);
System.out.println("  prefix="+prefix);
      this.config=filterConfig;
      servlet.init(fs,null,null,0,false);
   }
}