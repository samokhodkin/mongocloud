package mongocloud.server.servlet;

import java.util.*;
import java.io.IOException;
import java.security.Principal;

import mongocloud.server.servlet.BasicPrincipal;
import mongocloud.server.servlet.HttpUtil;

import javax.servlet.*;
import javax.servlet.http.*;

import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.*;
import org.bson.types.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static bwf.util.StringUtil.*;

/*
 * Simple http-basic authentication filter
 * name-pass pairs are checked against the database
 * If forseAuth=true, anonymous connections are accepted (with null principal)
 * 
 * db format: {
 *    name: string
 *    pass: string
 * }
 * 
 * configurable via injection
 * used in standalone demo
 **/

public class BasicAuthFilter implements Filter{
   public MongoCollection<Document> db; //injected
   public String realm;       //injected
   public boolean forseAuth=false; //injected; accept only athenticated requests
   public boolean autoCreate=false; //injected; create new user on the fly
   
   //user record fields
   public static final String KEY_NAME="name";
   public static final String KEY_PASS="pass";
   
   public void doFilter(
      ServletRequest req0, ServletResponse resp0, FilterChain chain
   ) throws IOException, ServletException{
//System.out.println("====================BasicAuthFilter.doFilter("+req0+"), forseAuth="+forseAuth);
      HttpServletRequest req=(HttpServletRequest)req0;
//System.out.println("  url="+req.getRequestURL());
//System.out.println("  pathInfo="+req.getPathInfo());
      HttpServletResponse resp=(HttpServletResponse)resp0;
      BasicPrincipal bp=HttpUtil.getBasicPrincipal(req);
      if(bp==null){ //not logged in
//System.out.println("  not logged");
         if(!forseAuth){
//System.out.println("  proceed");
            chain.doFilter(req,resp); //continue
            return;
         }
//System.out.println("  req. auth");
         HttpUtil.requestBasicAuth(req, resp, realm);
         return;
      }
      //check db
//System.out.println("  looking for "+bp.name);
      Document doc=db.find(
         new Document(KEY_NAME, bp.name)
      ).first();
//System.out.println("  doc="+doc);
      if(doc==null){ //not found
         if(autoCreate) db.insertOne(
            new Document(KEY_NAME, bp.name)
            .append(KEY_PASS, bp.pass)
         ); //succeed
         else{
            HttpUtil.requestBasicAuth(req, resp, realm);
            return;
         }
      }
      else{
//System.out.println("  doc.getString(KEY_PASS)="+doc.getString(KEY_PASS));
//System.out.println("  bp.pass="+bp.pass);
         if(!matches(doc.getString(KEY_PASS),bp.pass)){
            HttpUtil.requestBasicAuth(req, resp, realm);
            return;
         }
      }
      chain.doFilter(HttpUtil.setPrincipal(bp, req), resp);
   }
   
   public void  destroy(){
      //do nothing
   }
   public void init(FilterConfig filterConfig){
//System.out.println("********* BasicAuthFilter.init() **********");
//System.out.println("  db="+db);
//System.out.println("  realm="+realm);
//System.out.println("  forseAuth="+forseAuth);
   }
}
