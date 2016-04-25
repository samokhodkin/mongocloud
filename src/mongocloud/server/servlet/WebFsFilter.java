package mongocloud.server.servlet;

import com.mongodb.*;
import com.mongodb.client.*;
import mongocloud.server.fs.*;

import javax.servlet.*;

/**
 * Same as FsFilter yet configurable via init-params
 * See InitParameters.java
 */

public class WebFsFilter extends FsFilter implements InitParams{
   public void init(FilterConfig filterConfig) throws ServletException{
System.out.println("************************************************************************");
System.out.println("***********************WebFsFilter.init()************************");
System.out.println("************************************************************************");
      fs=new MongoFs();
      fs.db=(MongoCollection)filterConfig.getServletContext().getAttribute(PARAM_COLL_FILES);
      prefix=filterConfig.getInitParameter(PARAM_FS_PREFIX);
      super.init(filterConfig);
   }
}