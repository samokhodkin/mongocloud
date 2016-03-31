package mongocloud.server.servlet;

import java.security.Principal;

public class BasicPrincipal implements Principal{
   public final String name;
   public final String pass;
   public final String userAgent;
   public final String address;
   
   public BasicPrincipal(
      String name, String pass, String address, String userAgent
   ){
      this.name=name;
      this.pass=pass;
      this.address=address;
      this.userAgent=userAgent;
   }
   
   public String getName(){
      return name;
   }
   
   @Override
   public int hashCode(){
      return name.hashCode();
   }
   
   @Override
   public boolean equals(Object o){
      return (o instanceof Principal) && name.equals(((Principal)o).getName());
   }
   
   public String toString(){
      return name;
   }
}