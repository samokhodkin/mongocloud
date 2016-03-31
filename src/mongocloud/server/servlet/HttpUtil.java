package mongocloud.server.servlet;

import java.util.*;
import java.io.*;
import java.security.Principal;
import javax.servlet.*;
import javax.servlet.http.*;


public class HttpUtil{
   /*
    * parse the basic auth header "Authorization: Basic @base64($name:$pass)"
    */
   public static BasicPrincipal getBasicPrincipal(HttpServletRequest req){
//System.out.println("HttpUtil.getBasicPrincipal()");
      String authStr=req.getHeader("Authorization");
//System.out.println("  authStr="+authStr);
      if(authStr==null) return null;
      if(!authStr.startsWith("Basic ")) return null;
      authStr=authStr.substring(authStr.lastIndexOf(' ')+1);
      try{
         authStr=new String(Base64.getDecoder().decode(authStr),"Latin1");
      }
      catch(Exception e){
         throw new Error();//"Latin1" always supported
      }
      int i=authStr.indexOf(':');
      if(i<0) throw new Error("illegal auth string in request: "+authStr);
      return new BasicPrincipal(
         authStr.substring(0,i),  //name
         authStr.substring(i+1),  //pass
         req.getRemoteAddr(),
         req.getHeader("User-Agent")
      );
   }
   
   public static HttpServletRequest setPrincipal(final Principal p, HttpServletRequest req){
      return new HttpServletRequestWrapper(req){
         @Override
         public Principal getUserPrincipal(){
            return p;
         }
      };
   }
   
   public static HttpServletRequest withBasicPrincipal(HttpServletRequest req){
//System.out.println("HttpUtil.withBasicPrincipal()");
      return setPrincipal(getBasicPrincipal(req),req);
   }
   
   public static void requestBasicAuth(
      HttpServletRequest req, HttpServletResponse resp, String realm
   ) throws IOException{
      resp.addHeader("WWW-Authenticate", "Basic realm=\""+realm+"\"");
      resp.sendError(resp.SC_UNAUTHORIZED);
   }
   
}