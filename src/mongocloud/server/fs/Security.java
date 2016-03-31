package mongocloud.server.fs;

import java.util.*;
import java.security.Principal;
import org.bson.Document;

/* 
 * Utility methods for access control
 * 
 * This isn't a closed security model, as some methods in the storage model
 * make their own checks of the token aganst the database (and they must take care of the ALL token)
 * 
 * Terms:
 * token = representation of a principal, here just a user name
 * acl = access control list, list of tokens that may access the object
 */

public class Security{
   public static String ALL="*";
   
   //who has access to the root
   public static List<String> DEFT_ACL=Collections.unmodifiableList(
      Arrays.asList(new String[]{ALL})
   );
   
   public static String getToken(Principal p){
      if(p==null) return ALL;
      return p.getName();
   }
   
   public static boolean checkAccess(String token, List<String> acl){
      for(int i=0;i<acl.size();i++){
         if(check(token,acl.get(i))) return true;
      }
      return false;
   }
   
   /*
    * The acl for a new object created by a token in the context of the parent acl
    * (e.g. new file created by user in a directory)
    * note: the out may be the same instance as parentAcl, take care for it
    */
   public static List<String> deriveAcl(String token, List<String> parentAcl, List<String> out){
      int n=parentAcl.size(), w=0;
      for(int i=0; i<n; i++){
         String s=derive(token,parentAcl.get(i));
         if(s==null) continue;
         if(out.size()>w) out.set(w,s);
         else out.add(w,s);
         w++;
      }
      while(out.size()>w) out.remove(out.size()-1);
      return out;
   }
   
   //check token aganst control token
   private static boolean check(String token, String control){
      if(control.equals(ALL)) return true;
      if(token.equals(ALL)) return false;
      return token.equals(control);
   }
   
   private static String derive(String token, String control){
      if(control.equals(ALL)) return token;
      if(token.equals(ALL)) return control;
      return token.equals(control)? token: null;
   }
   
   public static void main(String[] args){
      List<String> acl=Arrays.asList(new String[]{"user1","user2"});
      System.out.println(checkAccess("user1",acl));
      System.out.println(checkAccess("user3",acl));
      System.out.println(deriveAcl("user1",acl,new ArrayList<String>()));
   }
}