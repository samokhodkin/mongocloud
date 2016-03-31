package myserver;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import bwf.server.web.*;
import bwf.server.web.jetty.*;

import mongocloud.server.fs.*;
import mongocloud.server.servlet.*;

import net.sf.webdav.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;

import ichat.server.api.*;
import ichat.server.local.*;
import ichat.server.servlet.*;
import ichat.util.*;

import static bwf.util.StringUtil.*;

//cmd:
//net use w: https://localhost:8443/fs-private 123 /user:user

public class Main{
   static String rootPass="jakomolim";
   
   static String host="0.0.0.0";
   static int httpPort=8080;
   static int httpsPort=8443;
   
   //localhost should be the last
   static String[] knownCertNames={
      "samokhodkin.tk", "localhost"
//      "localhost"
   };
   
   static String homePath;
   static String wwwRootPath;
   static String ksPath;
   static String certName;
   static String ksPass="123456";
   static String keyPass=ksPass;
   
   static String mongoDbName="mongocloud";
   static String mongoUsers="users";
   static String mongoPublicFiles="public";
   static String mongoPrivateFiles="private";
   
//   //content-modifying http methods; used to protect the root dir from changes
//   static final Set<String> modifyingMethods=new HashSet<String>();
//   static{
//      modifyingMethods.add("POST");
//      modifyingMethods.add("PUT");
//      modifyingMethods.add("DELETE");
//      modifyingMethods.add("MKCOL");
//   }
   
   public static void main(String[] args) throws Exception{
//debug ssl handshake
//System.setProperty("javax.net.debug", "ssl,handshake");

      certName=detectCertName();
      homePath=detectHomePath();
      
      ksPath=homePath+"/signing/keystore.jks";
      wwwRootPath=homePath+"/wwwroot";

System.out.println("certName="+certName);
System.out.println("homePath="+homePath);
System.out.println("ksPath="+ksPath);
System.out.println("wwwRootPath="+wwwRootPath);
      
      JettyServer server=new JettyServer();
server.setMinMmf(JettyServer.MMF_DISABLE);
      
      if(httpPort>0) server.addConnector(host,httpPort,60000);
      if(httpsPort>0){
         JettyServer.SslConnectorSpec spec=server.addSslConnector(
            host,httpsPort,60000
         );
         spec.ksPath=ksPath;
         spec.ksPass=ksPass;
         spec.alias=certName;
         spec.keyPass=keyPass;
      
         spec.excludeProtocols=new String[0]; //override default exclusions
         spec.includeProtocols=new String[]{
            "SSLv2Hello", "TLSv1","TLSv1.1","TLSv1.2", 
            //retain only secure ones + old handshake
            //SSLv2Hello is not a protocol, rather its an old handshake type
            //which is continued by a newer protocol
            //some clients, notable a windows mini-redirect and java<=1.6
            //need this type
         };
         spec.excludeCiphers=new String[0];
      }
      
      buildChat(server,"/chat");
      server.addResourceContext("/info", wwwRootPath+"/info", true);
      //must be last because of '/'
      buildMongofs(server, "/", "/info/fs.html");
      
      server.start();
      server.join();
   }
   
   static void buildMongofs(WebServer server, String path, String defaultPath){
      MongoClient mongo=new MongoClient();
      MongoDatabase db=mongo.getDatabase(mongoDbName);
      MongoCollection<Document> dbPubFiles=db.getCollection(mongoPublicFiles);
      MongoCollection<Document> dbPrivFiles=db.getCollection(mongoPrivateFiles);
      MongoCollection<Document> dbUsers=db.getCollection(mongoUsers);
      
      //add root to user db
      dbUsers.updateMany(
         new Document(BasicAuthFilter.KEY_NAME,"root"),
         new Document("$set", 
            new Document(BasicAuthFilter.KEY_NAME,"root")
            .append(BasicAuthFilter.KEY_PASS,rootPass)
         ),
         new UpdateOptions().upsert(true)
      );
      
      MongoFs pubFs=new MongoFs();
      pubFs.db=dbPubFiles;
      
      MongoFs privFs=new MongoFs();
      privFs.db=dbPrivFiles;
      
      FsFilter fPubFs=new FsFilter();
      fPubFs.fs=pubFs;
      
      FsFilter fPrivFs=new FsFilter();
      fPrivFs.fs=privFs;
      
      BasicAuthFilter fAuth=new BasicAuthFilter();
      fAuth.db=dbUsers;
      fAuth.realm="protected area";
      fAuth.forseAuth=true;
      fAuth.autoCreate=true;
      
      ForseScheme forseSsl=new ForseScheme();
      forseSsl.scheme="https";
      forseSsl.port=httpsPort;
      
      WebServer.SContext ctx=server.addServletContext(path);
      
      fPubFs.prefix="/fs-public";
      ctx.addFilter(fPubFs.prefix+"/*",fPubFs,false);
      
      fPrivFs.prefix="/fs-private";
      String privFsMapping=fPrivFs.prefix+"/*";
      ctx.addFilter(privFsMapping,forseSsl,false);
      ctx.addFilter(privFsMapping,fAuth,false);
      ctx.addFilter(privFsMapping,fPrivFs,false);
      
      //redirect unhandled requests to resource
      ctx.addFilter("/*",new AbstractHttpFilter(){
         public void doFilter(
            HttpServletRequest req, HttpServletResponse resp, FilterChain chain
         ) throws IOException, ServletException{
            resp.sendRedirect(defaultPath);
         }
      },false);
   }
   
   static void buildChat(WebServer server,String path){
      ChatServlet.dirAvatars=new File(wwwRootPath, "chat/avatar/big");
      ChatServlet.dirSmileys=new File(wwwRootPath, "chat/smiley");
      ChatServlet.chatService=new LocalChatService();
      
      server.addResourceContext(path+"/res",wwwRootPath+"/chat",true);//false);
      
      WebServer.SContext chatContext=server.addServletContext(path);
      chatContext.addServlet("/app/*","Chat servlet",new ChatServlet(),true);
      chatContext.addSessionListener(new HttpSessionListener(){
         public void sessionCreated(HttpSessionEvent se){
            System.out.println("chat:sessionCreated("+se.getSession().getId()+")");
         }
         public void sessionDestroyed(HttpSessionEvent se){
            System.out.println("chat:sessionDestroyed("+se.getSession().getId()+")");
            HttpSession s=se.getSession();
            Chat ch=(Chat)s.getAttribute(ChatServlet.S_ATTR_CHAT);
            User u=(User)s.getAttribute(ChatServlet.S_ATTR_USER);
            if(ch==null || u==null) return;
            ch.removeUser(u,new Completion<Boolean>(){
               public void complete(Boolean b){
                  //do nothing
               }
               public void error(Exception e){
                  //do nothing
               }
            });
         }
      });
   }
   
   private static void ensureDir(MongoFs fs, String path, ITransaction t){
      StoredObject dir=fs.getStoredObject(t,path);
      if(dir==null) fs.createFolder(t,path);
   }
   
   static String detectCertName() throws Exception{
      Set myAddresses=new HashSet();
      for(NetworkInterface netint: Collections.list(NetworkInterface.getNetworkInterfaces())){
         for(InetAddress inetAddress: Collections.list(netint.getInetAddresses())){
            myAddresses.add(inetAddress.getHostAddress());
         }
      }
      for(String name:knownCertNames){
         try{
            for(InetAddress netaddr: InetAddress.getAllByName(name)){
               if(myAddresses.contains(netaddr.getHostAddress())) return name;
            }
         }
         catch(Exception e){
            System.out.println("WARNING: "+name+" is not a domain name");
         }
      }
      throw new RuntimeException("cert names doesn't match any local address");
   }
   
   static String detectHomePath() throws IOException{
      File f=new File(new File(".").getCanonicalPath());
      for(;;){
         if(
            new File(f,"bin").isDirectory() &&
            new File(f,"wwwroot").isDirectory() &&
            new File(f,"signing").isDirectory()
         ){
            return f.getPath();
         }
         String parent=f.getParent();
         if(parent==null) break;
         f=new File(parent);
      }
      throw new RuntimeException("Home not found");
   }
   
   static String path(String p1, String p2){
      if(p1==null) return p2;
      if(p1.length()==0) return p2;
      if(p2.startsWith("/")){
         if(p1.equals("/")) return p2;
         if(p1.endsWith("/")) return p1+p2.substring(1);
         return p1+p2;
      }
      if(!p1.endsWith("/")) return p1+"/"+p2;
      return p1+p2;
   }
}