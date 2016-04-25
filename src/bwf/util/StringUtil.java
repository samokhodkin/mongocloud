package bwf.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

public class StringUtil{
   public static boolean matches(String s1, String s2){
      if(s1!=null && s1.length()==0) s1=null;
      if(s2!=null && s2.length()==0) s2=null;
      return ObjectUtil.equals(s1,s2);
   }
   
   public static String join(Collection<String> c, String sep){
      return join(c,sep,new StringBuilder()).toString();
   }
   
   public static StringBuilder join(Collection<String> c, String sep, StringBuilder out){
      boolean doSep=false;
      for(String s: c){
         if(doSep) out.append(sep);
         out.append(s);
         doSep=true;
      }
      return out;
   }
   
   //print public fields
   public static String toString(Object o){
      if(o==null) return "null";
      StringBuilder sb=new StringBuilder();
      Class c=o.getClass();
      sb.append(c.getSimpleName()).append("(");
      for(Field f: c.getFields()){
         if((f.getModifiers()&Modifier.STATIC)!=0) continue;
         try{
            sb.append(f.getName()).append("=").append(f.get(o)).append("; ");
         }
         catch(Exception e){}
      }
      return sb.append(")").toString();
   }
   
   public static String loadFile(String path) throws IOException{
      Reader in=new FileReader(path);
      StringBuilder sb=new StringBuilder();
      for(int c; (c=in.read())!=-1;) sb.append((char)c);
      return sb.toString();
   }
   
   public static String saveText(String text,String path) throws IOException{
      File f=new File(path);
      Writer out=new FileWriter(f);
      try{
         out.write(text);
         return f.getCanonicalPath();
      }
      finally{
         out.close();
      }
   }
   
   public static String[] loadPropertiesAsArray(String path) throws IOException{
      Reader in=new FileReader(path);
      Properties p=new Properties();
      p.load(in);
      String[] args=new String[p.size()*2];
      int i=0;
      for(String name: p.stringPropertyNames()){
         args[i++]=name;
         args[i++]=p.getProperty(name);
      }
      return args;
   }
   
   /**
    * parse class fields from args list
    * e.g.
    * {"-myField", "xyz"} results in c.myField="xyz"
    * Respected are String, String[], String[][], int, double, boolean fields
    * if -theName corresponds to static c.theName, the next item is processed as a value
    * otherwise args list is printed and false returned
    **/
   public static boolean parseArgs(Class<?> c, String[] args){
      Map<String,Field> fields=getAssignableFields(c);
      for(int i=0;i<args.length;){
         String name=args[i++];
         if(name.startsWith("-")) name=name.substring(1);
         if(!fields.containsKey(name)){
            printArgs(c);
            continue;
         }
         String value=args[i++];
         Field f=fields.get(name);
         Class<?> type=f.getType();
         f.setAccessible(true);
         try{
            if(type.equals(String.class)) f.set(null,value);
            if(type.equals(String[].class)) f.set(null,parseStringArray(value));
            if(type.equals(String[][].class)) f.set(null,parseStringArray2(value));
            if(type.equals(Integer.TYPE)) f.setInt(null,Integer.parseInt(value));
            if(type.equals(Float.TYPE)) f.setFloat(null,Float.parseFloat(value));
            if(type.equals(Double.TYPE)) f.setDouble(null,Double.parseDouble(value));
            if(type.equals(Boolean.TYPE)) f.setBoolean(null,Boolean.parseBoolean(value));
         }
         catch(IllegalAccessException e){
            throw new Error(e);
         }
      }
      return true;
   }
   
   public static void printArgs(Class<?> c){
      try{
         printArgs(getAssignableFields(c));
      }
      catch(IllegalAccessException e){
         throw new Error(e);
      }
   }
   
   private static Map<String,Field> getAssignableFields(Class<?> c){
      return ObjectUtil.getFields(
         c, Modifier.STATIC, Modifier.FINAL|Modifier.TRANSIENT
      );
   }
   
   private static void printArgs(Map<String,Field> fields) throws IllegalAccessException{
      for(Field f:fields.values()){
         f.setAccessible(true);
         System.out.println("-"+f.getName()+" <"+f.getType().getSimpleName()+"> (default: "+f.get(null)+")");
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
//      String s="{\"xxx\",aaa,\" ccc \",\"\",{1,2,3},}";
//      System.out.println(parseList(listPattern.matcher(s),0));
//      System.out.println(Arrays.asList(parseStringArray(s)));
//      s="{{\"xxx\",aaa},{\" ccc \",\"\"},{1,2,3},}";
//      System.out.println(parseList(listPattern.matcher(s),0));
//      for(String[] item:parseStringArray2(s)){
//         if(item==null) System.out.println(item);
//         else System.out.println(Arrays.asList(item));
//      }
//      System.exit(0);
      
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