package bwf.server.web;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

public abstract class AbstractHttpFilter implements Filter{
   public abstract void doFilter(
      HttpServletRequest req, HttpServletResponse resp, FilterChain chain
   ) throws IOException, ServletException;
   
   public void doFilter(
      ServletRequest req, ServletResponse resp, FilterChain chain
   ) throws IOException, ServletException{
      doFilter((HttpServletRequest)req, (HttpServletResponse)resp, chain);
   }

   public void  destroy(){}
   public void init(FilterConfig filterConfig){}
}