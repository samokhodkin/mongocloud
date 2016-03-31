package mongocloud.server.app;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;

import mongocloud.server.fs.*;
import mongocloud.server.servlet.*;

import bwf.util.StringUtil;
import bwf.server.web.*;
import bwf.server.web.jetty.*;

/*
 * Test application with embedded jetty (uses bwf.server.web.jetty API wrapper)
 * Creates webdav share at http://localhost:8080/mongocloud
 * To enable https assign values to sslPort and keystore parameters: 
 * ksPath, ksPass, keyPass, certName
 */

public class Main{
   //mongodb settings
   static String mongoDbName="mongocloud";
   static String mongoCollFiles="files";
   static String mongoCollUsers="users";
   
   //web settings
   //filesystem root address: http://host:port[contextPath][filterPrefix]
   static String host="0.0.0.0";
   static int port=8080;
   
   static String contextPath="/mongocloud";
   static String filterMapping="/*";
   static String filterPrefix=null;

   static int sslPort;     //8443;
   static String ksPath;   //"path/to/keystore.jks";
   static String ksPass;   //"keystore password";
   static String keyPass;  //"key password";
   static String certName; //cert alias; CN should match local host name
   
   static boolean forseAuth=true;
   static String authRealm="mongocloud";
   static String[][] defaultUsers={{"user","123"},{"bob","marley"},{"alice","cooper"}};
   
   public static void main(String[] args) throws Exception{
//System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX+"net.sf.webdav", "TRACE");
//System.setProperty("org.slf4j.simpleLogger.log.net.sf.webdav", "TRACE");
//System.setProperty("org.slf4j.simpleLogger.log.org.mongodb.driver", "ERROR");
      
      try{
         StringUtil.parseArgs(Main.class, args);
      }
      catch(Exception e){
         e.printStackTrace();
         StringUtil.printArgs(Main.class);
      }
      
      MongoClient mongo=new MongoClient();
      MongoDatabase db=mongo.getDatabase(mongoDbName);
      MongoCollection<Document> dbFiles=db.getCollection(mongoCollFiles);
      MongoCollection<Document> dbUsers=db.getCollection(mongoCollUsers);
      
      System.out.println("==== file system: ====");
      for(Document d:dbFiles.find()) System.out.println(" - "+d);
      System.out.println("======================\n");
      
      for(String[] namePass: defaultUsers){
         //set pass to existing user or create new one
         dbUsers.updateMany(
            new Document(BasicAuthFilter.KEY_NAME,namePass[0]),
            new Document("$set", 
               new Document(BasicAuthFilter.KEY_NAME,namePass[0])
               .append(BasicAuthFilter.KEY_PASS,namePass[1])
            ),
            new UpdateOptions().upsert(true)
         );
      }
      System.out.println("==== users: ====");
      for(Document d:dbUsers.find()) System.out.println(" - "+d);
      System.out.println("================\n");
      
      MongoFs fs=new MongoFs();
      BasicAuthFilter authf=new BasicAuthFilter();
      FsFilter fsf=new FsFilter();
      
      fs.db=dbFiles;
      fsf.fs=fs;
      fsf.prefix=filterPrefix;
      authf.db=dbUsers;
      authf.realm=authRealm;
      authf.forseAuth=forseAuth;
      authf.autoCreate=true;
      
      JettyServer server=new JettyServer();
      server.addConnector(host, port, 10*60000, 0, 0);
      if(sslPort>0){
         JettyServer.SslConnectorSpec sslspec=server.addSslConnector(
            host,sslPort,60000
         );
         sslspec.ksPath=ksPath;
         sslspec.ksPass=ksPass;
         sslspec.alias=certName;
         sslspec.keyPass=keyPass;
      }
      
      WebServer.SContext c=server.addServletContext(contextPath);
      
      c.addFilter(filterMapping, fsf, false);
      
      server.start();
      server.join();
   }
}