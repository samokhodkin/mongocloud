package bwf.util;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

public class ObjectUtil{
   public static boolean equals(Object o1, Object o2){
      if(o1==null) return o2==null;
      return o2!=null && o1.equals(o2);
   }
   
   public static boolean matches(Object o1, Object o2){
      if(o1==null || o2==null) return true;
      return o1.equals(o2);
   }
}