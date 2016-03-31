package bwf.util;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

public class StringUtil{
   public static boolean matches(String s1, String s2){
      if(s1!=null && s1.length()==0) s1=null;
      if(s2!=null && s2.length()==0) s2=null;
      return equals(s1,s2);
   }
   
   public static boolean equals(Object o1, Object o2){
      if(o1==null) return o2==null;
      return o1.equals(o2);
   }
   
   /**
    * parse class fields from args list
    * e.g.
    * {"-myField", "xyz"} results in c.myField="xyz"
    * Respected are String, int, double, boolean fields
    */
   public static void parseArgs(Class<?> c, String[] args) throws Exception{
      for(int i=0;i<args.length;){
         String name=args[i++];
         String value=args[i++];
         if(name.startsWith("-")) name=name.substring(1);
         Field f=c.getDeclaredField(name);
         if(f==null) continue;
         if((f.getModifiers()&Modifier.STATIC)==0) continue;
         Class<?> type=f.getType();
         f.setAccessible(true);
         if(type.equals(String.class)) f.set(null,value);
         if(type.equals(String[].class)) f.set(null,parseStringArray(value));
         if(type.equals(String[][].class)) f.set(null,parseStringArray2(value));
         if(type.equals(Integer.TYPE)) f.setInt(null,Integer.parseInt(value));
         if(type.equals(Float.TYPE)) f.setDouble(null,Float.parseFloat(value));
         if(type.equals(Double.TYPE)) f.setDouble(null,Double.parseDouble(value));
         if(type.equals(Boolean.TYPE)) f.setBoolean(null,Boolean.parseBoolean(value));
      }
   }
   
   public static void printArgs(Class<?> c) throws Exception{
      while(c!=null){
         for(Field f:c.getDeclaredFields()){
            if((f.getModifiers()&Modifier.STATIC)==0) continue;
            f.setAccessible(true);
            System.out.println("-"+f.getName()+" <"+f.getType().getSimpleName()+"> (default: "+f.get(null)+")");
         }
         c=c.getSuperclass();
      }
   }
   
   public static String[] parseStringArray(String s){
      return toStringArray(parseList(s));
   }
   
   public static String[][] parseStringArray2(String s){
      List<String[]> out=new ArrayList<String[]>();
      List<Object> data=parseList(s);
      for(int i=0;i<data.size();i++){
         Object o=data.get(i);
         if(o instanceof List) out.add(toStringArray((List<Object>)o));
         else out.add(null);
      }
      return out.toArray(new String[out.size()][]);
   }
   
   private static String[] toStringArray(List<Object> list){
      String[] out=new String[list.size()];
      for(int i=0;i<out.length;i++){
         Object o=list.get(i);
         if(o instanceof String) out[i]=(String)o;
      }
      return out;
   }
   
   final static Pattern listPattern=Pattern.compile(
      "\\G(?:"+
         "([\\s]+)" + "|"+
         "(\\{)" + "|"+
         "(\\})" + "|"+
         "\"((?:[^\"]*|\\\\\")*)\"" + "|"+
         "\\s*([^{},\"\\s]+(?:\\s+[^{},\"\\s]+)*)\\s*" + "|"+
         "(,)"+
      ")"
   );
   private static List<Object> parseList(String s){
      return parseList(listPattern.matcher(s),0);
   }
   private static List<Object> parseList(Matcher m, int off){
      List<Object> list=new ArrayList<Object>();
      m.region(off,m.regionEnd());
      boolean comma=false;
      while(m.find()){
         if(m.start(1)>0) continue; //' '
         else if(m.start(2)>0){ //{
            list.add(parseList(m,m.end()));
            comma=false;
         }
         else if(m.start(3)>0){ //}
            if(comma) list.add(null);
            return list;
         }
         else if(m.start(4)>0){ //"..."
            list.add(m.group(4));
            comma=false;
         }
         else if(m.start(5)>0){ //... ...
            list.add(m.group(5).trim());
            comma=false;
         }
         else if(m.start(6)>0){ //,
            if(comma) list.add(null);
            comma=true;
         }
      }
      return list;
   }
   
   static String stringField;
   static int intField;
   static double doubleField;
   static float floatField;
   static boolean booleanField;
   
   public static void main(String[] args) throws Exception{
      String s="{\"xxx\",aaa,\" ccc \",\"\",{1,2,3},}";
      System.out.println(parseList(listPattern.matcher(s),0));
      System.out.println(Arrays.asList(parseStringArray(s)));
      s="{{\"xxx\",aaa},{\" ccc \",\"\"},{1,2,3},}";
      System.out.println(parseList(listPattern.matcher(s),0));
      for(String[] item:parseStringArray2(s)){
         if(item==null) System.out.println(item);
         else System.out.println(Arrays.asList(item));
      }
      System.exit(0);
      
      printArgs(StringUtil.class);
      parseArgs(StringUtil.class, new String[]{
         "-stringField", "aaa",
         "-intField", "123",
         "-doubleField", "1.234",
         "-floatField", "1.2",
         "-booleanField", "true",
      });
      printArgs(StringUtil.class);
   }
}