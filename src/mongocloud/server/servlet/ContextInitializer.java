package mongocloud.server.servlet;

import javax.servlet.*;

import mongocloud.server.fs.*;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import static bwf.util.StringUtil.*;

/**
 * 
 */

public class ContextInitializer implements ServletContextListener, InitParams{
   public void contextDestroyed(ServletContextEvent sce){
      //nothing
   }
   
   /*
    * create mongodb objects and save as context attrs
    */
   public void contextInitialized(ServletContextEvent sce){
      final ServletContext context=sce.getServletContext();
      MongoDatabase db=(MongoDatabase)context.getAttribute(PARAM_DB);
      MongoCollection userDb, filesDb;
      if(db==null){
         db=new MongoClient().getDatabase(context.getInitParameter(PARAM_DB));
         userDb=db.getCollection(context.getInitParameter(PARAM_COLL_USERS));
         filesDb=db.getCollection(context.getInitParameter(PARAM_COLL_FILES));
         context.setAttribute(PARAM_DB,db);
         context.setAttribute(PARAM_COLL_USERS, userDb);
         context.setAttribute(PARAM_COLL_FILES, filesDb);
      }
      else userDb=(MongoCollection)context.getAttribute(PARAM_COLL_USERS);
      
      String[][] usrList=parseStringArray2(
         context.getInitParameter(PARAM_DEFAULT_USERS) //"{{name1,pass1},{name2,pass2},...}"
      );
      if(usrList!=null){ //create users
         for(String[] namePass: usrList){
            //this is an "upsert" operation: update existing, insert otherwise, see last arg
            userDb.updateMany(
               new Document(BasicAuthFilter.KEY_NAME,namePass[0]),
               new Document("$set", 
                  new Document(BasicAuthFilter.KEY_NAME,namePass[0])
                  .append(BasicAuthFilter.KEY_PASS,namePass[1])
               ),
               new UpdateOptions().upsert(true)
            );
         }
      }
   }
}