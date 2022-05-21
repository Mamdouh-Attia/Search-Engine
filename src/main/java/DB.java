import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import javax.print.Doc;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Filter;

public class DB {
    MongoClient client;
    String uri="mongodb://localhost:27017";
    MongoDatabase db;

    public DB(){
        client = MongoClients.create(uri);

    }
    public MongoDatabase connecttoTestSearchIndex(){
        MongoDatabase db=client.getDatabase("TestSearchIndex");
        //const cursor = db.collection.find().noCursorTimeout();

        return db;

    }
    public MongoDatabase connecttoCrawlerDataBase(){
        MongoDatabase db=client.getDatabase("CrawlerDataBase");


        return db;

    }
    public boolean isitExist(String word){
        //
        MongoDatabase db=client.getDatabase("TestSearchIndex");
        MongoIterable<String> PhraseCollectionstable= db.listCollectionNames(); // Get all collections(words stored in the database)
        boolean CollectionExist = false;
        for(String s : PhraseCollectionstable )
        {
            if(s.equals(word))
            {
                //collection exists
                CollectionExist=true;
                break; //because there is only one collection for that word
            }
        }

        return CollectionExist;
    }

    public ArrayList<String> getStopWords() throws IOException {
        ArrayList<String>StopWords = new ArrayList<String>();
        FileInputStream StopFile=new FileInputStream("D:\\2nd year 2nd term\\JDK\\eclipse\\MongoDBApp\\stopwords.txt");
        byte[] b = new byte[ StopFile.available()];

        StopFile.read(b);
        StopFile.close();
        String StopData[]=new String(b).trim().split("\n");

        for(int i=0;i<StopData.length;i++)
        {
            StopWords.add(StopData[i].trim());

        }
        return StopWords;
    }



    public boolean updateCrawledPages(int cp){
        MongoDatabase db=client.getDatabase("CrawlerResultDB");
        MongoCollection state = db.getCollection("CrawlingCollection_Links_Queue");
        state.updateOne(Filters.eq("_id","pages"),new Document("$set",new Document("crawledPagesNum", cp)));
        return true;

    }
    public boolean storeResultFromCrawler(CrawlerResult cr){
        this.db=client.getDatabase("CrawlerResultDB");
        MongoCollection col=db.getCollection("CrawlerResult");
        BasicDBObject doc_3 = new BasicDBObject("_id", cr.cs)
                .append("url",cr.url)
                .append("title",cr.title)
                .append("popularity",0);
        col.insertOne( new Document(doc_3.toMap()));
        return true;
    }
    public boolean CheckForCS(String cs){
        this.db=client.getDatabase("CrawlerResultDB");
        MongoCollection col=db.getCollection("CrawlerResult");
        FindIterable<Document> it=col.find(Filters.eq("_id",cs));
        for(Document doc:it){
            return false;
        }
        return true;

    }

    public boolean storeCrawlerResult(){
        //
        MongoDatabase db=client.getDatabase("demo");
        MongoCollection coll=db.getCollection("searches");
        FindIterable<Document> it= coll.find();
        for(Document doc:it){
            System.out.println(doc);
        }
        return true;
    }



    public ArrayList<String> getCrawlerResult(){
        this.db=client.getDatabase("CrawlerResultDB");
        MongoCollection<org.bson.Document> CrawlingCollection = db.getCollection("CrawlerResult");
        FindIterable<org.bson.Document> URLStoIndexiterable = CrawlingCollection.find();
        for(Document doc:URLStoIndexiterable){
            System.out.println(doc.get("url"));
        }
        return new ArrayList<>();
    }
    public boolean insertLinks(LinkedList<String> links){
        this.db=client.getDatabase("CrawlerResultDB");
        try{
            MongoCollection collec=db.getCollection("CrawlingCollection_Links_Queue");
            collec.deleteOne(Filters.eq("_id","Links_Queue"));
            collec.deleteOne(Filters.eq("_id","pages"));

            //org.bson.Document document = new org.bson.Document("_id", "Links_Queue");
            BasicDBObject doc_3 = new BasicDBObject("_id", "Links_Queue").append("Array", links);
            collec.insertOne( new Document(doc_3.toMap()));
            BasicDBObject doc_4 = new BasicDBObject("_id", "pages").append("crawledPagesNum", 0);
            collec.insertOne( new Document(doc_4.toMap()));

            System.out.println("See the dB");

        }
        catch (Exception e){
            System.out.println("ERror when inserting the seeds :"+e);
            return false;
        }
        return true;
    }
    public void specifyDB(String db){
        this.db=client.getDatabase(db);
    }
    public boolean updateCrawlerLinks(LinkedList<String> newLinks){
        //
        MongoDatabase db=client.getDatabase("CrawlerResultDB");
        MongoCollection collec=db.getCollection("CrawlingCollection_Links_Queue");

        collec.deleteOne(Filters.eq("_id","Links_Queue"));

        //org.bson.Document document = new org.bson.Document("_id", "Links_Queue");
        BasicDBObject doc_3 = new BasicDBObject("_id", "Links_Queue").append("Array", newLinks);
        collec.insertOne( new Document(doc_3.toMap()));



        return true;
    }
    public int getCrawledPages(){
        MongoDatabase db=client.getDatabase("CrawlerResultDB");
        MongoCollection state = db.getCollection("CrawlingCollection_Links_Queue");
        FindIterable<Document> it=state.find(Filters.eq("_id","pages"));
        int count=0;
        for(Document doc:it){
            count=(Integer) doc.get("crawledPagesNum");
        }
        return count;

    }
    public ArrayList<String> getLinks(){
        this.db=client.getDatabase("CrawlerResultDB");
        MongoCollection state = db.getCollection("CrawlingCollection_Links_Queue");
        FindIterable<Document> it=state.find(Filters.eq("_id","Links_Queue"));
        ArrayList<String> result=new ArrayList<>();
        for(Document doc:it){
            System.out.println(doc);
            result.addAll((ArrayList<String>)doc.get("Array"));

            //for(Map.Entry<String ,Object> e:doc.entrySet())
            // result=(LinkedList<String>) e.getValue();
            // if(e.getKey()=="Array")
            //      System.out.println(e);
        }

        return result;
    }




}