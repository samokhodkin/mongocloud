package bwf.server.web;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

public class ForseScheme implements Filter{
   //inject
   public String scheme;
   public int port;
   
   public void doFilter(
      ServletRequest req0, ServletResponse resp0, FilterChain chain
   ) throws IOException, ServletException{
      HttpServletRequest req = (HttpServletRequest) req0;
      HttpServletResponse resp = (HttpServletResponse) resp0;
      if(req.getScheme().equals(scheme)){
         chain.doFilter(req, resp);
         return;
      }
      String uri=req.getRequestURI();
      String domain=req.getServerName();
      String url=scheme+"://"+domain+":"+port+uri;
      resp.sendRedirect(url);
   }
   
   public void  destroy(){
      //do nothing
   }
   public void init(FilterConfig filterConfig){
      //do nothing
   }
}