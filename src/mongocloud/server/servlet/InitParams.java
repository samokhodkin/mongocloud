package mongocloud.server.servlet;

/*
 * Initialisation of WebBasicAuthFilter and WebFsFilter classes via web.xml
 * 
 * Context-wide param example (part of <web-app>):
 *   <context-param>
 *       <param-name>mongocloud.dbName</param-name>
 *       <param-value>mongoTest</param-value>
 *   </context-param>
 *
 * Filter param example (part of <filter>):
 *   <init-param>
 *     <param-name>mongocloud.dbFiles</param-name>
 *     <param-value>files</param-value>
 *   </init-param>
 */
 
public interface InitParams{
   //context parameters, common to both filters
   //also used as context attrs, for pasing objects
   String PARAM_DB="mongo.db.name";             //mongo db name
   String PARAM_COLL_FILES="mongo.collection.files";  //mongo collection name for files
   String PARAM_COLL_USERS="mongo.collection.users";  //mongo collection name for users
   String PARAM_FS_PREFIX="fsPrefix";  //mongo collection name for users
   String PARAM_DEFAULT_USERS="defaultUsers";           //initial user list, string "user1:pass1,user2:pass2,.."
   
   //auth filter init parameters
   String PARAM_AUTH_REALM="authRealm"; //BASIC auth realm, any string
   String PARAM_AUTH_FORCE="forceAuth"; //disallow anonymous connection; true/false
   String PARAM_AUTH_AUTOCREATE="autocreate"; //disallow anonymous connection; true/false
}