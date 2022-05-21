import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class crawlerMain {
    public static final int TN = 14;//number of threads
    // public Crawler crawler;

    public static void main(String[] args) throws IOException, InterruptedException {



        DB db1=new DB();

        LinkedList<String> LinksQueue = new LinkedList<>();
        int pageNo = db1.getCrawledPages(); ;
        System.out.println(pageNo);

        if (pageNo == 0 || pageNo >= Crawler.MX_link_count) {

            //initial seed
            pageNo = 0;
            LinksQueue.add("https://en.wikipedia.org/wiki/Wikipedia:Contents");
            LinksQueue.add("http://www.w3schools.blog/");
            LinksQueue.add("http://www.geeksforgeeks.com");
            LinksQueue.add("http://www.javatpoint.com/");

            db1.specifyDB("CrawlerResultDB");
            db1.db.getCollection("CrawlingCollection_Links_Queue").drop();
            db1.db.getCollection("CrawlerResult").drop();

            db1.insertLinks(LinksQueue);

        } else {//Continue crawling where we left off
            //retrieve from database
            ArrayList<String> retLinks=db1.getLinks();
            for(int i=0;i<retLinks.size();i++){
                LinksQueue.add(retLinks.get(i));
            }
            //System.out.println(LinksQueue);
            System.out.println("Previous Crawl has been interrupted! Continuing from where we left off:_");

        }
        Crawler crawler = new Crawler(pageNo, LinksQueue, db1);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("In shutdown hook,saving popularity");
                crawler.StorePopularity();

            }
        }, "Shutdown-thread"));
        Thread T[] = new Thread[TN];
        for (int i = 0; i < TN; i++) {
            T[i] = new Thread(crawler);
            System.out.println("Hi");
            T[i].start();
        }
        for (int i = 0; i < TN; i++) {
            T[i].join();
        }
    }


}