package mongocloud.server.fs;

import java.util.*;
import java.io.*;
import java.security.Principal;

import net.sf.webdav.*;
import net.sf.webdav.exceptions.*;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import org.bson.*;
import org.bson.types.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * A simple file system that stores data in the mongodb
 * 
 * All objects are stored in a single collection
 * 
 * Data format:
 * file:{
 *  _id: ObjectId
 *  isDirectory: false
 *  name: string
 *  path: string - 
 *  parent: ObjectId - used for non-recursive listing; desn't exist for top-level objects
 *  ancestors: [ObjectId] - used for finding all descendants (recursive); empty array for top-level objects
 *  size: integer
 *  lastModified: date
 *  created: date
 *  data: byte array
 *  acl: [string] - list of owners; 
 * }
 * folder:{
 *  _id: ObjectId
 *  isDirectory: true
 *  name: string
 *  path: string
 *  parent: ObjectId - used for non-recursive listing; desn't exist for top-level objects
 *  ancestors: [ObjectId] - used for finding all descendants (recursive); empty array for top-level objects
 *  lastModified: date
 *  created: date
 *  acl: [string] - list of owners; 
 * }
 *
 */

public class MongoFs implements IWebdavStore{
   
   public MongoCollection<Document> db; //injected
   
   private static Logger log=LoggerFactory.getLogger(MongoFs.class);
   
   private static final String KEY_ID="_id";
   private static final String KEY_ISDIR="isDirectory";
   private static final String KEY_NAME="name";
   private static final String KEY_PATH="path";
   private static final String KEY_PARENT="parent";
   private static final String KEY_ANCESTORS="ancestors";
   private static final String KEY_CREATED="created";
   private static final String KEY_LASTMOD="lastModified";
   private static final String KEY_SIZE="size";
   private static final String KEY_ACL="acl";
   private static final String KEY_DATA="data";
   private static final String KEY_CTYPE="contentType";
   private static final String KEY_ENC="contentEncoding";
   
   private static final String[] F_ANCESTORS={KEY_ANCESTORS};
   private static final String[] F_DATA={KEY_DATA};
   private static final String[] F_NONE={};
   private static final String[] F_SIZE={KEY_SIZE};
   
   private static final InputStream EMPTY_STREAM=new InputStream(){
      public int read(){
         return -1;
      }
      public int available(){
         return 0;
      }
   };
   
   private static final Date created=new Date();
   private static final Date lastModified=created;
   
   /**
    *
   */
   public ITransaction begin(final Principal principal){
//System.out.println("MFS.begin("+principal+")");
      return new ITransaction(){
         public Principal getPrincipal(){
            return principal;
         }
      };
   }

   /**
   */
   public void checkAuthentication(ITransaction transaction){
//System.out.println("MFS.checkAuthentication("+transaction+")");
      
   }
   
   /**
   */
   public void commit(ITransaction transaction){
//System.out.println("MFS.commit("+transaction+")");
      
   }
   
   /**
   */
   public void rollback(ITransaction transaction){
//System.out.println("MFS.rollback("+transaction+")");
      
   }
   
   /**
   */
   public void createFolder(ITransaction transaction, String path){
//System.out.println("MFS.createFolder("+path+")");
      if(path==null || path.equals("/")) throw new WebdavException("root dir cannot be created");
      Document parent=find(getParentPath(path), true, transaction, F_ANCESTORS);
      if(parent==null) throw new ObjectNotFoundException(getParentPath(path));
      Date now=new Date();
      Document doc=new Document();
      doc.put(KEY_NAME, getFileName(path));
      doc.put(KEY_PATH, path);
      doc.put(KEY_ISDIR, true);
      doc.put(KEY_CREATED, now);
      doc.put(KEY_LASTMOD, now);
      doc.put(KEY_PARENT, parent.getObjectId(KEY_ID));
      doc.put(KEY_ANCESTORS, deriveAncestors(parent));
      doc.put(KEY_ACL, deriveAcl(transaction, parent));
      db.insertOne(doc);
   }
   
   /**
   */
   public void createResource(ITransaction transaction, String path){
//System.out.println("MFS.createResource("+path+")");
      Document parent=find(getParentPath(path), true, transaction, F_ANCESTORS);
      if(parent==null) throw new ObjectNotFoundException(getParentPath(path));
      Date now=new Date();
      Document doc=new Document();
      doc.put(KEY_NAME, getFileName(path));
      doc.put(KEY_PATH, path);
      doc.put(KEY_ISDIR, false);
      doc.put(KEY_CREATED, now);
      doc.put(KEY_LASTMOD, now);
      doc.put(KEY_PARENT, parent.getObjectId(KEY_ID));
      doc.put(KEY_ANCESTORS, deriveAncestors(parent));
      doc.put(KEY_ACL, deriveAcl(transaction, parent));
      db.insertOne(doc);
   }
   
   /**
   */
   public InputStream getResourceContent(ITransaction transaction, String path){
//System.out.println("MFS.getResourceContent("+path+")");
      Document file=find(path, false, transaction, F_DATA);
      if(file==null) throw new ObjectNotFoundException();
      Binary data=(Binary)file.get(KEY_DATA);
      return data==null? EMPTY_STREAM: new ByteArrayInputStream(data.getData()); 
   }
   
   /**
   */
   public long setResourceContent(
      ITransaction transaction, String path, InputStream content, 
      String contentType, String characterEncoding
   ){
//System.out.println("MFS.setResourceContent("+path+")");
      Document res=find(path, false, transaction, F_NONE);
      if(res==null) throw new ObjectNotFoundException();
      byte[] data=readAll(content);
      db.updateOne(
         new Document(KEY_ID, res.get(KEY_ID)),
         new Document("$set",
            new Document(KEY_DATA, new Binary(data))
            .append(KEY_LASTMOD, new Date())
            .append(KEY_SIZE, data.length)
            .append(KEY_CTYPE, contentType)
            .append(KEY_ENC, characterEncoding)
         )
      );
      return data.length;
   }
   
   /**
   */
   public String[] getChildrenNames(ITransaction transaction, String path){
//System.out.println("MFS.getChildrenNames("+path+")");
      Document dir=find(path, true, transaction, F_NONE);
      if(dir==null) throw new ObjectNotFoundException();
      MongoIterable<Document> cursor=db.find(
         new Document(KEY_PARENT, dir.get(KEY_ID))
         .append("$or", Arrays.asList(new Document[]{
            new Document(KEY_ACL, Security.ALL),
            new Document(KEY_ACL, Security.getToken(transaction.getPrincipal()))
         }))
      ).projection(
         new Document(KEY_NAME, true)
      );
      List<String> buf=new ArrayList<String>();
      for(Document entry:cursor) buf.add((String)entry.get(KEY_NAME));
      return buf.toArray(new String[buf.size()]);
   }
   
   /**
   */
   public long getResourceLength(ITransaction transaction, String path){
//System.out.println("MFS.getResourceLength("+path+")");
      Document res=find(path, true, transaction, F_SIZE);
      if(res==null) throw new ObjectNotFoundException();
      return res.getLong(KEY_SIZE);
   }
   
   /**
   */
   public void removeObject(ITransaction transaction, String path){
//System.out.println("MFS.removeObject("+path+")");
      DeleteResult res=db.deleteMany(
         new Document(KEY_PATH,path)
         .append(KEY_ACL,Security.getToken(transaction.getPrincipal()))
      );
      if(res.getDeletedCount()==0) throw new ObjectNotFoundException();
   }
   
   /**
   */
   public StoredObject getStoredObject(ITransaction transaction, String path){
//System.out.println("MFS.getStoredObject("+path+")");
      Document doc=find(path, null, transaction, null);
      if(doc==null) return null;
//      if(doc==null) throw new ObjectNotFoundException();
      StoredObject obj=new StoredObject();
      obj.setFolder(doc.getBoolean(KEY_ISDIR)==Boolean.TRUE);
      obj.setLastModified(doc.getDate(KEY_LASTMOD));
      obj.setCreationDate(doc.getDate(KEY_CREATED));
      Number size=(Number)doc.get(KEY_SIZE);
      obj.setResourceLength(size==null? 0: size.longValue());
      //obj.setNullResource(size==null); ????
      return obj;
   }
   
   /*
    * /dir/file -> /dir
    * /file -> null
    * / -> null
    * null -> null
    */
   private static String getParentPath(String path){
      if(path==null) return null;
      int i=path.lastIndexOf('/');
      if(i<=0) return null;
      return path.substring(0,i);
   }
   
   /*
    * /dir/file -> file
    * /file -> file
    * / -> null
    * null -> null
    */
   private static String getFileName(String path){
      if(path==null) return null;
      if(path.equals("/")) return null;
      int i=path.lastIndexOf('/');
      if(i<0) return path;
      return path.substring(i+1);
   }
   
   private Document find(
      String path, Boolean isDir, ITransaction transaction, String[] fields
   ){
      if(path==null || path.equals("") || path.equals("/")){ //root object
         List<String> acl=new ArrayList<String>();
         acl.addAll(Security.DEFT_ACL);
         return new Document() //no id
            .append(KEY_ACL, acl)
            .append(KEY_ISDIR, true)
            .append(KEY_ANCESTORS, new ArrayList<String>())
            .append(KEY_CREATED, created)
            .append(KEY_LASTMOD, lastModified)
         ;
      }
      if(path.endsWith("/")) path=path.substring(0,path.length()-1);
//System.out.println("MFS.find() path="+path);
      Document filter=new Document(KEY_PATH, path);
      filter.append("$or", Arrays.asList(new Document[]{
         new Document(KEY_ACL, Security.ALL),
         new Document(KEY_ACL, Security.getToken(transaction.getPrincipal()))
      }));
      if(isDir!=null) filter.append(KEY_ISDIR, isDir);
      FindIterable<Document> cursor=db.find(filter);
      if(fields!=null){
         Document proj=new Document(KEY_ACL, true);
         for(String f: fields) proj.append(f, true);
         cursor=cursor.projection(proj);
      }
      Document d=cursor.first();
//System.out.println("MFS.find() -> "+d);
      return d;
   }
   
   private static List<ObjectId> deriveAncestors(Document obj){
      List<ObjectId> list=(List<ObjectId>)obj.get(KEY_ANCESTORS);
      list.add(obj.getObjectId(KEY_ID));
      return list;
   }
   
   private static List<String> deriveAcl(ITransaction t, Document obj){
      List<String> acl=(List<String>)obj.get(KEY_ACL);
      return Security.deriveAcl(Security.getToken(t.getPrincipal()), acl, acl);
   }
   
   private static byte[] readAll(InputStream in){
      try{
         ByteArrayOutputStream out=new ByteArrayOutputStream();
         int c;
         while((c=in.read())!=-1) out.write(c);
         return out.toByteArray();
      }
      catch(IOException e){
         throw new WebdavException(e);
      }
   }
   
   public static void main(String[] args){
      System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX+"mongocloud.server.fs", "TRACE");
      MongoClient mongo=new MongoClient();
      MongoDatabase db=mongo.getDatabase("mongocloud");
      MongoCollection<Document> coll=db.getCollection("files");
      MongoFs fs=new MongoFs();
      fs.db=coll;
      
      class MyTransaction implements ITransaction{
         Principal p;
         MyTransaction(final String userName){
            p=new Principal(){
               public String getName(){return userName;}
            };
         }
         public Principal getPrincipal(){return p;}
      };
      ITransaction t1=new MyTransaction("alice");
      ITransaction t2=new MyTransaction("bob");
      
      ITransaction t=t1;
      
      coll.drop();
      fs.createFolder(t,"/aaa");
      fs.createFolder(t,"/bbb");
      fs.createFolder(t,"/aaa/aaa");
      fs.createFolder(t,"/aaa/bbb");
      fs.createResource(t,"/aaa/bbb/data1");
      fs.createResource(t,"/bbb/data2");
      fs.setResourceContent(
         t, "/bbb/data2", new ByteArrayInputStream("12345".getBytes()), "text/plain", "Latin1"
      );
      System.out.println("=======================");
      
      System.out.println("db:");
      for(Document d: coll.find()) System.out.println("   "+d);
      System.out.println("=======================");
      
      for(String path: new String[]{
         "/","/aaa","/bbb","/aaa/bbb",
      }){
         System.out.println(path+": "+Arrays.asList(fs.getChildrenNames(t,path)));
      }
      System.out.println("=======================");
      
      for(String path: new String[]{
         "/","/aaa","/bbb","/aaa/bbb", "/bbb/data2"
      }){
         StoredObject obj=fs.getStoredObject(t,path);
         System.out.println("stored object for "+path+": "+obj+", is NR:"+obj.isNullResource());
      }
      System.out.println("=======================");
      
//      
//      fs.removeObject(t1, "/bbb/data2");
//      for(Document d: coll.find()) System.out.println("   "+d);
//      System.out.println("=======================");
//      
//      System.out.println("--with t2--");
//      t=t2;
//      
//      for(String path: new String[]{
//         "/","/aaa","/bbb","/aaa/bbb",
//      }){
//         System.out.println(path+": "+Arrays.asList(fs.getChildrenNames(t,path)));
//      }
//      System.out.println("=======================");
      
      
//      fs.createFolder(t2,"/aaa");
//      fs.createFolder(t2,"/bbb");
//      fs.createFolder(t2,"/aaa/aaa");
//      fs.createFolder(t2,"/aaa/bbb");
//      fs.createResource(t1,"/aaa/bbb/data1");
      
   }
}