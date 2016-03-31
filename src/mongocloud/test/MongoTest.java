package mongocloud.test;

/**
 * @(#)MongoTest.java
 *
 *
 * @author 
 * @version 1.00 2015/12/4
 */

import java.util.*;

import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.*;
import org.bson.types.*;

/* todo: need update() examples
 * updated:
 * to create or modify existing doc - use update+{upsert:true} 
 * to insert if not exists, otherwise not change - use update(id)->{}+{upsert:true}+$setOnInsert:{all fields here}
 * to insert or modify *some* fields of existing doc - use update(id)->{fields to insert *and* modify}+{upsert:true}+$setOnInsert:{fields to insert only}
 */

public class MongoTest{
   public static void main(String[] args) throws Exception{
      MongoClient mongo=new MongoClient();
      MongoDatabase db=mongo.getDatabase("firsttest");
      MongoCollection<Document> coll=db.getCollection("mycollection");
//      coll.insertOne(
//         new Document()
//         .append("string", "String value")
//         .append("integer", 100)
//         .append("double", 0.01)
//         .append("date", new Date())
//         .append("bytes", new byte[]{'h','e','l','l','o'})
//         .append("stringArray", Arrays.asList(new String[]{
//            "str1", "str2", "str3", 
//         }))
//         .append("objectArray", Arrays.asList(new Object[]{
//            new Document("a",1),
//            new Document("a",2),
//            new Document("a",3),
//            new Document("a",4),
//         }))
//         .append("objectId", new ObjectId())
//      );
//      //"integer"==100, notice: not the string "100"
//      MongoIterable<Document> cursor=coll.find(new Document("integer",100));
//      //objectArray is defined and not null
//      MongoIterable<Document> cursor=coll.find(new Document("objectArray",new Document("$exists",true)));
//      //"stringArray" contains "str1"
//      MongoIterable<Document> cursor=coll.find(
//         new Document("stringArray", "str1")
//      );
//      //"stringArray" contains 1+ elements from the set {"str1","str5"}
//      MongoIterable<Document> cursor=coll.find(
//         new Document("stringArray", new Document("$in",Arrays.asList(new String[]{
//            "str1", "str5"
//         })))
//      );
//      //"objectArray" contains 1+ objects with obj.a==n
//      MongoIterable<Document> cursor=coll.find(
//         new Document("objectArray", new Document("a",5))
//      );
//      //"objectArray" contains 1+ objects with obj.a>1
//      MongoIterable<Document> cursor=coll.find(
//         new Document("objectArray.a",new Document("$gt", 4))
//      );
//      //"objectArray" contains 1+ objects with obj.a>1 and  1+ objects with obj.a<3
//      MongoIterable<Document> cursor=coll.find(
//         new Document("objectArray", new Document()
//            .append("a",new Document("$gt",1))
//            .append("a",new Document("$lt",3))
//         )
//      );
      //"objectArray" contains 1+ objects with 1 < obj.a < 3
      MongoIterable<Document> cursor=coll.find(
         new Document("objectArray", new Document("$elemMatch", 
            new Document("a",new Document("$gt",1))
                 .append("a",new Document("$lt",3))
         ))
      );
      int i=0;
      for(Document d: cursor){
         //System.out.println((i++)+": "+((List)d.get("stringArray")).get(0).getClass());
         //System.out.println((i++)+": "+d.getObjectId("objectId"));
         System.out.println((i++)+": "+d.get("bytes").getClass());
      }
   }
}