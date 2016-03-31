package mongocloud.test;

/**
 * @(#)FileStrorageTest.java
 *
 *
 * @author 
 * @version 1.00 2015/12/3
 */

import javax.servlet.*;
import java.security.*;
import javax.servlet.http.*;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;

import mongocloud.server.fs.*;
import mongocloud.server.servlet.*;
import mongocloud.server.servlet.HttpUtil;

import bwf.server.web.*;
import bwf.server.web.jetty.*;

import net.sf.webdav.*;
import net.sf.webdav.exceptions.*;

import java.io.*;

public class MongoFsTest{
   public static void main(String[] args) throws Exception{
      System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX+"net.sf.webdav", "TRACE");
      
      MongoClient mongo=new MongoClient();
      MongoDatabase db=mongo.getDatabase("mongocloud");
      MongoCollection<Document> dbFiles=db.getCollection("files");
      MongoCollection<Document> dbUsers=db.getCollection("users");
      
      System.out.println("db contents");
      System.out.println("------------");
      for(Document d:dbFiles.find()) System.out.println("  "+d);
      
      dbUsers.updateMany(
         new Document(BasicAuthFilter.KEY_NAME,"user")
              .append(BasicAuthFilter.KEY_PASS,"123"),
         new Document("$set", 
            new Document(BasicAuthFilter.KEY_NAME,"user")
            .append(BasicAuthFilter.KEY_PASS,"123")
         ),
         new UpdateOptions().upsert(true)
      );
      dbUsers.updateMany(
         new Document(BasicAuthFilter.KEY_NAME,"testwebdavuser")
              .append(BasicAuthFilter.KEY_PASS,"123"),
         new Document("$set", 
            new Document(BasicAuthFilter.KEY_NAME,"testwebdavuser")
            .append(BasicAuthFilter.KEY_PASS,"123")
         ),
         new UpdateOptions().upsert(true)
      );
      for(Document d:dbUsers.find()) System.out.println(d);
      
      final MongoFs fs=new MongoFs(){
         @Override
         public void checkAuthentication(ITransaction t) throws WebdavException{
            Principal p=t.getPrincipal();
            if(p==null) throw new UnauthenticatedException("Login please"); //forse auth
            //if(!p.equals("user")) throw new UnauthenticatedException("Login please"); //forse auth
            //throw new UnauthenticatedException("Login please"); //reset auth
         }
      };
      
      fs.db=dbFiles;
            
      WebServer server=new JettyServer();
      server.addConnector("0.0.0.0", 8080, 10*60000, 0, 0);
      WebServer.SContext c=server.addServletContext("/");
      HttpServlet servlet=new WebdavServlet(){
         @Override
         public void init() throws ServletException {
            System.out.println("MongoFs servlet.init()");
            init(fs,null,null,0,false);
         }
         
         @Override
         public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException{
            super.service(HttpUtil.withBasicPrincipal(req), resp);
         }
      };
      WebServer.Parameters p=c.addServlet("/*", "MONGOFS1", servlet, false);
      
      server.start();
      server.join();
   }
}
