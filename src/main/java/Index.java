
import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.tartarus.snowball.ext.PorterStemmer;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.jsoup.Connection;
//import org.apache.lucene.analysis.PorterStemmer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
//import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.eq;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tartarus.snowball.ext.PorterStemmer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class Index {
    MongoDatabase db;
    MongoDatabase db2;

    public static void main( String[] args ) throws IOException{
        Index i=new Index();



        HashMap<String,HashMap<String,org.bson.Document>>IndexerMap= new HashMap<String,HashMap<String,org.bson.Document>>() ;
        //connect MongoDB
        DB dbi=new DB();

        /////////////////////////////removing stop words////////////////////////
        ArrayList<String> StopWords = dbi.getStopWords();
        long start = System.nanoTime();
        IndexerMap=i.Indexer(StopWords);

        i.StoreInDataBase(IndexerMap);
        long time = System.nanoTime() - start;
        System.out.printf("iNdexing  took %.3f seconds%n",  time/1e9);
        System.out.println(IndexerMap.size() );


    }

    public  HashMap<String, HashMap<String, org.bson.Document>> Indexer(ArrayList<String> StopWords)throws IOException
    {
        DB dbi2=new DB();
        db2=dbi2.connecttoCrawlerDataBase();

        int TotalNumberOfPages=dbi2.getCrawledPages();
        ////////////////////////////////////////////////////////////////////////////
        int k=0;
        MongoCollection<org.bson.Document> CrawlingCollection = db2.getCollection("CrawlerResult");
        FindIterable<org.bson.Document> URLStoIndexiterable = CrawlingCollection.find().noCursorTimeout(true);
        HashMap<String,HashMap<String,org.bson.Document>>IndexerMap= new HashMap<String,HashMap<String,org.bson.Document>>() ;


        for(org.bson.Document URLstobeIndexed : URLStoIndexiterable)
        {
            k++;

            System.out.println("///////////////////////"+k+"////////////////////////////////");
            try {
                Document doc =  Jsoup.connect((String) URLstobeIndexed.get("url")).get();

                //Get The page conent
                String PageText=doc.text();
                String pageTextWords[]=PageText.split(" ");
                for (int i=0;i<pageTextWords.length;i++)
                {
                    //Check if the word is not a stop word
                    if(!(StopWords.contains(pageTextWords[i].trim().toLowerCase())))
                    {
                        //Remove punctuation and numbers
                        pageTextWords[i]=pageTextWords[i].replaceAll("[^a-zA-Z ]", "");
                        //get the tags that contain that word in that document to use them in ranking
                        HashSet<String> Tags = new HashSet<String>();
                        if(!pageTextWords[i].equals("")) //because the replaceAll function may makes it empty string
                        {
                            Elements elts= doc.getElementsContainingOwnText(pageTextWords[i]);
                            for(Element e:elts)
                            {
                                Tags.add(e.tagName());
                            }
                        }

                        //Stemming
                        PorterStemmer stemmer = new PorterStemmer();
                        stemmer.setCurrent(pageTextWords[i].toLowerCase());
                        stemmer.stem();
                        pageTextWords[i]=stemmer.getCurrent();

                        if(!pageTextWords[i].equals(""))
                        {
                            //index here to Know the position
                            boolean WordExist = IndexerMap.containsKey(pageTextWords[i]);

                            if(WordExist)
                            {
                                //collection exists
                                HashMap<String,org.bson.Document> WordDocuments = IndexerMap.get(pageTextWords[i]);
                                boolean URLExist = WordDocuments.containsKey((String) URLstobeIndexed.get("url"));
                                if(URLExist)
                                {
                                    org.bson.Document ThisDocument = WordDocuments.get((String) URLstobeIndexed.get("url"));
                                    int newTF =(int) ThisDocument.get("TF")+1;
                                    HashSet<Integer> positions= (HashSet<Integer>) ThisDocument.get("positions");
                                    positions.add(i);
                                    HashSet<String> OldTags=  (HashSet<String>) ThisDocument.get("Tags");
                                    HashSet<String> NewTags= new HashSet<>();
                                    NewTags.addAll(OldTags);
                                    NewTags.addAll(Tags);

                                    ThisDocument.replace("TF", newTF);
                                    ThisDocument.replace("positions", positions);
                                    ThisDocument.replace("Tags", NewTags);

                                    WordDocuments.replace((String) URLstobeIndexed.get("url"), ThisDocument);
                                }

                                if(!URLExist)
                                {
                                    org.bson.Document newdoc= new org.bson.Document ();
                                    HashSet<Integer> positions = new HashSet<>();
                                    positions.add(i);
                                    //positions to be used in phrase searching
                                    newdoc.append("url",(String) URLstobeIndexed.get("url"));
                                    newdoc.append("TF",1);
                                    newdoc.append("positions", positions);
                                    newdoc.append("Tags", Tags);
                                    newdoc.append("weights", 0);
                                    newdoc.append("PageRank", URLstobeIndexed.get("popularity"));
                                    //Add popularity
                                    WordDocuments.put((String) URLstobeIndexed.get("url"), newdoc);
                                }
                                IndexerMap.replace(pageTextWords[i], WordDocuments);
                            }
                            else
                            {
                                //New word
                                org.bson.Document newdoc= new org.bson.Document ();
                                HashSet<Integer> positions = new HashSet<>();
                                positions.add(i);
                                //positions to be used in phrase searching
                                newdoc.append("url",(String) URLstobeIndexed.get("url"));
                                newdoc.append("TF",1);
                                newdoc.append("positions", positions);
                                newdoc.append("Tags", Tags);
                                newdoc.append("weights", 0);
                                newdoc.append("PageRank", URLstobeIndexed.get("popularity"));
                                //Add popularity
                                HashMap<String,org.bson.Document> WordDocuments=new HashMap<String,org.bson.Document>();
                                WordDocuments.put((String) URLstobeIndexed.get("url"), newdoc);
                                IndexerMap.put(pageTextWords[i], WordDocuments);
                            }
                        }
                    }

                }

                System.out.println("////////////////////"+(String) URLstobeIndexed.get("url")+"////////////"+k+"///////////////////////");

            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
        for(HashMap.Entry<String,HashMap<String,org.bson.Document>> Wordentry:IndexerMap.entrySet() )
        {
            HashMap<String,org.bson.Document> Documents = Wordentry.getValue();

            for(HashMap.Entry<String,org.bson.Document> DocumentEntry:Documents.entrySet() )
            {
                int importance;
                org.bson.Document OneDocument = DocumentEntry.getValue();
                HashSet<String> IndexedTags=  (HashSet<String>) OneDocument.get("Tags");

                if(IndexedTags.contains("title"))
                    importance=20;
                else if(IndexedTags.contains("head"))
                    importance=18;
                else if(IndexedTags.contains("strong"))
                    importance=16;
                else if(IndexedTags.contains("main"))
                    importance=14;
                else if(IndexedTags.contains("h1"))
                    importance=12;
                else if(IndexedTags.contains("h2"))
                    importance=10;
                else if(IndexedTags.contains("h3"))
                    importance=8;
                else if(IndexedTags.contains("h4"))
                    importance=6;
                else if(IndexedTags.contains("h5"))
                    importance=4;
                else if(IndexedTags.contains("h6"))
                    importance=2;
                else
                    importance=0;
                OneDocument.replace("weights", importance);
                Documents.put((String)OneDocument.get("url"), OneDocument);
                //IndexerMap.replace(Wordentry.getKey(),  Documents);

            }
            IndexerMap.put(Wordentry.getKey(),  Documents);

        }


        // String PageTextWitoutStopWords="";
        return IndexerMap;

    }


    public void StoreInDataBase(HashMap<String,HashMap<String,org.bson.Document>>IndexerMap)
    {

        DB dbi1=new DB();
        db=dbi1.connecttoTestSearchIndex();

        for(HashMap.Entry<String,HashMap<String,org.bson.Document>> Wordentry:IndexerMap.entrySet() )
        {

            MongoCollection<org.bson.Document> word = db.getCollection(Wordentry.getKey());
            HashMap<String,org.bson.Document> Documents = Wordentry.getValue();
            for(HashMap.Entry<String,org.bson.Document> DocumentEntry:Documents.entrySet() )
            {

                org.bson.Document OneDocument = DocumentEntry.getValue();
                word.insertOne(OneDocument);
            }
        }


    }

}
