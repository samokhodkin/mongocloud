package mongocloud.server.servlet;

import com.mongodb.*;
import com.mongodb.client.*;
import mongocloud.server.fs.*;

import javax.servlet.*;

/**
 * Same as BasicAuthFilter yet configurable via init-params
 * See InitParameters  
 */

public class WebBasicAuthFilter extends BasicAuthFilter implements InitParams{
   public void init(FilterConfig filterConfig){
System.out.println("************************************************************************");
System.out.println("***********************WebBasicAuthFilter.init()************************");
System.out.println("************************************************************************");
      db=(MongoCollection)filterConfig.getServletContext().getAttribute(PARAM_COLL_USERS);
      realm=filterConfig.getInitParameter(PARAM_AUTH_REALM);
      forseAuth=Boolean.parseBoolean(filterConfig.getInitParameter(PARAM_AUTH_FORCE));
      autoCreate=Boolean.parseBoolean(filterConfig.getInitParameter(PARAM_AUTH_AUTOCREATE));
      super.init(filterConfig);
   }
}