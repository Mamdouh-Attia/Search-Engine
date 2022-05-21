import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.tartarus.snowball.ext.PorterStemmer;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import java.util.*;
public class QueryProcessor {
    String Query;
    DB dbi;
    MongoDatabase db;
    ArrayList<String> StopWords;
    int TotalNumberOfPages;
    Index in;
    public QueryProcessor(String Q)throws IOException{
        Query=Q;
        dbi=new DB();
        db=dbi.connecttoTestSearchIndex();
        TotalNumberOfPages=5032;
        in=new Index();
        StopWords = dbi.getStopWords();

    }

    ArrayList<FinalLinks> queryProcessorLogic(){
        try
        {
            if(Query.charAt(0)=='"'&&Query.charAt(Query.length()-1)=='"')
            {
                return phraseSearching();
            }
            else
                return nonphraseSearching();
        }
        catch(IOException e)
        {
            return null;
        }

    }
    public ArrayList<FinalLinks> phraseSearching() throws IOException {
        // Phrase searching
        Query=Query.substring(1,Query.length()-1);
        String Original=Query;
        String QueryWords[]=Query.split(" ");// To be processed to search by it in the data base
        String URLResult= ""; //To be filled with URLs containing this query(if any) //needs it in this scope
        int Weight= 0;//will be updated later
        int PhraseFrequency= 0;//will be updated later
        int PageRank= 0;//will be updated later

        ArrayList<PhraseURLInfo> listOfURLs = new ArrayList<PhraseURLInfo>();
        HashMap<String,MongoCollection<org.bson.Document>>RankerWordsMap = new HashMap<String,MongoCollection<org.bson.Document>>();

        PhraseURLInfo PUI=new PhraseURLInfo();
        boolean AllWordsExist = true;

        int FirstNonStopWord=-1;
        boolean ThereIsANonStopWord=false;
        for (int i=0;i<QueryWords.length;i++)//Check that each word stem exists in the data base
        {
            if(!(StopWords.contains(QueryWords[i].trim().toLowerCase())))
            {

                if(QueryWords[i]!="")
                    QueryWords[i]=QueryWords[i].replaceAll("[^a-zA-Z ]", "");// Remove punctuations
                if(QueryWords[i]!="")
                {
                    //Check if it is a worf in the query which is non stop word do we search for it
                    //if all words are stop words we can't search
                    ThereIsANonStopWord=true;
                    if(FirstNonStopWord==-1)
                    {
                        FirstNonStopWord=i;
                    }
                    //Stemming
                    PorterStemmer stemmer = new PorterStemmer();
                    stemmer.setCurrent(QueryWords[i].toLowerCase());
                    stemmer.stem();
                    QueryWords[i]=stemmer.getCurrent();

                    // Get all collections(words stored in the database)
                    MongoIterable<String> PhraseCollectionstable= db.listCollectionNames(); // Get all collections(words stored in the database)
                    boolean CollectionExist = false;
                    for(String s : PhraseCollectionstable )
                    {
                        if(s.equals(QueryWords[i]))
                        {
                            //collection exists
                            CollectionExist=true;
                            RankerWordsMap.put(QueryWords[i], db.getCollection(QueryWords[i]));
                            break; //because there is only one collection for that word
                        }
                    }

                    if(!CollectionExist) //This word is not in the database
                    {
                        AllWordsExist=false;
                        break;
                    }
                }
            }
        }

        if(AllWordsExist&& ThereIsANonStopWord)
        {
            //All words stem are in the database
            //Get the URLs of each word stem
            ArrayList<ArrayList<String>> ListOfURLLists=new ArrayList<ArrayList<String>>();
            for(HashMap.Entry<String,MongoCollection<org.bson.Document>> Wordentry:RankerWordsMap.entrySet() )
            {
                ArrayList<String> ThisWordURLs=new ArrayList<String>();
                MongoCollection<org.bson.Document> Documents = Wordentry.getValue();

                FindIterable<org.bson.Document> ThisWordDocuments = Documents.find();
                for(org.bson.Document URLDocument : ThisWordDocuments)
                {
                    ThisWordURLs.add((String) URLDocument.get("URL")) ;
                }
                ListOfURLLists.add(ThisWordURLs);

            }

            //Get the intersection of the lists of URLs
            boolean UrlIntersection=true;
            for (int i=1;i<ListOfURLLists.size();i++)
            {
                ListOfURLLists.get(i).retainAll(ListOfURLLists.get(i-1));//Get the intersection of each list with the previous one
                if(ListOfURLLists.get(i).size()==0)//No intersection
                {
                    UrlIntersection=false;
                    break;
                }
            }
            if(UrlIntersection)
            {
                //There is intersection between URLs
                //The intersection is in the last list
                //ListOfLists.get(ListOfLists.size()-1);
                //For each URL --->parse it and check it contains the phrase
                for(int i=0;i<ListOfURLLists.get(ListOfURLLists.size()-1).size();i++)
                {
                    //For each URL in the intersection
                    //Get the content
                    String URL=ListOfURLLists.get(ListOfURLLists.size()-1).get(i);
                    Document Parsed = Jsoup.connect(URL).get();
                    String PageContent=Parsed.text();
                    if(PageContent.contains(Original))
                    {
                        URLResult=URL;
                        PhraseFrequency=PageContent.split(Query, -1).length-1;

                        MongoCollection<org.bson.Document> word = RankerWordsMap.get(QueryWords[FirstNonStopWord]);
                        //Get all URLs of this Word
                        FindIterable<org.bson.Document> ThisWordDocuments = word.find();

                        for(org.bson.Document URLDocument : ThisWordDocuments)
                        {
                            if(((String) URLDocument.get("URL")).equals(URL))
                            {
                                int FirstWordWeight=(int) URLDocument.get("weights");
                                Weight=FirstWordWeight;
                            }
                        }
                    }
                    //==//
                    Document doc = Jsoup.parse(URLResult);
                    String wantedlines="";
                    if ((doc.text().length())-doc.text().indexOf(Original)>30)
                        wantedlines=(doc.text()).substring(doc.text().indexOf(Original)  , doc.text().indexOf(Original)+30);
                    else
                        wantedlines=(doc.text()).substring(doc.text().indexOf(Original)  , doc.text().length());
                    PUI.setplainText(wantedlines);
                    PUI.settitle(doc.title());
                    PUI.setURL(URLResult);
                    PUI.setPhraseFrequency(PhraseFrequency);
                    PUI.setWeight(Weight);
                    listOfURLs.add(PUI) ;
                }
            }
        }

        //
        PhraseURLInfoComparator com = new PhraseURLInfoComparator();
        Collections.sort(listOfURLs, com);
        return (ArrayList<FinalLinks>)((ArrayList<?>)(listOfURLs ));
    }


    public ArrayList<FinalLinks> nonphraseSearching() throws IOException{
        //Not phrase
        //get query words

        String QueryWords[]=Query.split(" ");// To be processed to search by it in the data base
        String OriginalQueryWords[]=Query.split(" "); //To store the original words without processing
        //To store URLs containing the word without stemming and store their info needed to rank
        HashMap<String,Integer>URLLists= new HashMap<>(); //To be filled with URLs containing this query(if any)
        ArrayList<Integer>TF_IDFLists= new ArrayList<>();//To be filled with corresponding TF-IDF of URLs containing this query(if any)
        ArrayList<Integer>No_Of_QueryWords_in_the_document= new ArrayList<>();
        ArrayList<Integer>PageRank= new ArrayList<>();
        ArrayList<Integer>WeightsList= new ArrayList<>();
        ArrayList<NonPhraseURLInfo> listOfURLs = new ArrayList<NonPhraseURLInfo>();
        int listOfURLsIndex=0;
        //To store URLs containing the word with stemming and store their info needed to rank
        HashMap<String,Integer>URLListsStem= new HashMap<>(); //To be filled with URLs containing this query(if any)
        ArrayList<Integer>TF_IDFListsStem= new ArrayList<>();//To be filled with corresponding TF-IDF of URLs containing this query(if any)
        ArrayList<Integer>No_Of_QueryWords_in_the_documentStem= new ArrayList<>();
        ArrayList<Integer>PageRankStem= new ArrayList<>();
        ArrayList<Integer>WeightsListStem= new ArrayList<>();
        ArrayList<NonPhraseURLInfo> listOfStemmedURLs = new ArrayList<NonPhraseURLInfo>();
        int listOfStemmedURLsIndex=0;
        HashMap<String,MongoCollection<org.bson.Document>>RankerWordsMap = new HashMap<String,MongoCollection<org.bson.Document>>();

        NonPhraseURLInfo NonPhraseObj=new NonPhraseURLInfo();
        for (int i=0;i<QueryWords.length;i++)//Check that each word stem exists in the data base
        {
            //remove stop words from the query ,stem the rest of words and convert them to lower case letters
            if(!(StopWords.contains(QueryWords[i].trim().toLowerCase())))
            {
                if(QueryWords[i]!="")
                    QueryWords[i]=QueryWords[i].replaceAll("[^a-zA-Z ]", "");
                if(QueryWords[i]!="")
                {
                    //Stemming
                    PorterStemmer stemmer = new PorterStemmer();
                    stemmer.setCurrent(QueryWords[i].toLowerCase());
                    stemmer.stem();
                    QueryWords[i]=stemmer.getCurrent();
                    //////////////////////////////////////////
                    // Get all collections(words stored in the database)
                    MongoIterable<String> Collectionstable= db.listCollectionNames(); // Get all collections(words stored in the database)
                    boolean CollectionExist = false;
                    for(String s : Collectionstable )
                    {
                        if(s.equals(QueryWords[i]))
                        {
                            //collection exists
                            CollectionExist=true;
                            RankerWordsMap.put(QueryWords[i], db.getCollection(QueryWords[i]));
                            break; //because there is only one collection for that word
                        }
                    }

                    if(CollectionExist)
                    {
                        //get all documents in the collection of this word
                        MongoCollection<org.bson.Document> word = RankerWordsMap.get(QueryWords[i]);
                        FindIterable<org.bson.Document> ThisWordDocuments = word.find();
                        //DF = number of documents containing this word
                        long DF=word.countDocuments();
                        int IDF = TotalNumberOfPages/ (int)DF;
                        for(org.bson.Document URLDocument : ThisWordDocuments)
                        {
                            int TF =(int) URLDocument.get("TF");
                            int TF_IDF = TF*IDF;
                            //Get Url stored in that document and retrieve the array of positions and array of tags in this document
                            String URL= (String) URLDocument.get("URL");
                            int wordWeigt=(int) URLDocument.get("weights");

                            //check if a word from the query exists in the non stemming form in this URL(the URL exist in the list of the URLs containing the non stemming word)

                            //boolean URLExist=false;
                            int index=0;
                            //check if the URL contained before in the List of URLs containing a non stemmed word from the query
                            if(URLLists.containsKey(URL))
                            {//This URL exists in the result
                                index=URLLists.get(URL);
                                //This URL exists in the result
                                //increase the TF_IDF, the weight sum of words and the count of words in it
                                //the page rank will not change

                                NonPhraseURLInfo npi= listOfURLs.get(index);

                                int LastTF_IDF=npi.getTF_IDF();
                                TF_IDF+=LastTF_IDF;
                                npi.setTF_IDF(TF_IDF);

                                int NumberOfwORDS=npi.getNo_Of_QueryWords_in_the_document();
                                NumberOfwORDS++;
                                npi.setNo_Of_QueryWords_in_the_document(NumberOfwORDS);

                                int OldWeights=npi.getWeight();
                                OldWeights+=wordWeigt;
                                npi.setWeight(OldWeights);
                                Document doc = Jsoup.parse(URL);
                                String wantedlines="";
                                if ((doc.text().length())>30)
                                    wantedlines=(doc.text()).substring(0  , +30);
                                else
                                    wantedlines=(doc.text()).substring(0, doc.text().length());
                                npi.setplainText(wantedlines);
                                npi.settitle(doc.title());

                            }
                            else
                            {
                                //If the URL is not in the List of URLs containing a non stemming word --> the last words in the query is not in this URL in a non stemming form
                                //Check if it contains this word(current word) without stemming
                                boolean ContainsThisWordWithoutStemming=false;
                                //parse URL to get its content to check if
                                Document Parsed = Jsoup.connect(URL).get();
                                String PageContent[]=Parsed.text().split(" ");
                                //retrieve the list of positions of this stem in the URL
                                ArrayList<Integer> positions= (ArrayList<Integer>) URLDocument.get("positions");

                                for(int k=0;k<positions.size();k++)
                                {
                                    //for each position of the stem check if the word in that position is the original word in the query(without processing)
                                    if(PageContent[positions.get(k)].replaceAll( "[.,:\" ]","").trim().toLowerCase().equals(OriginalQueryWords[i].trim().toLowerCase()))
                                    {
                                        ContainsThisWordWithoutStemming=true;
                                        break;
                                    }
                                }
                                if(!ContainsThisWordWithoutStemming)
                                {
                                    //this URL doesn't contain this word without stemming
                                    //Search for it in the List of URL containing stemmed words
                                    index=0;
                                    if(URLListsStem.containsKey(URL))
                                    {//This URL exists in the result
                                        index=URLListsStem.get(URL);
                                        //increase the TF_IDF and the count of words in it
                                        //the page rank will not change
                                        NonPhraseURLInfo npi= listOfStemmedURLs.get(index);

                                        int LastTF_IDF=npi.getTF_IDF();
                                        TF_IDF+=LastTF_IDF;
                                        npi.setTF_IDF(TF_IDF);

                                        int NumberOfwORDS=npi.getNo_Of_QueryWords_in_the_document();
                                        NumberOfwORDS++;
                                        npi.setNo_Of_QueryWords_in_the_document(NumberOfwORDS);

                                        int OldWeights=npi.getWeight();
                                        OldWeights+=wordWeigt;
                                        npi.setWeight(OldWeights);

                                    }
                                    else
                                    {
                                        NonPhraseURLInfo npi= new NonPhraseURLInfo();

                                        URLListsStem.put(URL, listOfStemmedURLsIndex);
                                        listOfStemmedURLsIndex++;
                                        npi.setURL(URL);
                                        //npi.setPageRank(IDF)
                                        npi.setTF_IDF(TF_IDF);
                                        npi.setWeight(wordWeigt);
                                        npi.setNo_Of_QueryWords_in_the_document(1);
                                        Document doc = Jsoup.parse(URL);
                                        String wantedlines="";
                                        if ((doc.text().length())>30)
                                            wantedlines=(doc.text()).substring(0  , 30);
                                        else
                                            wantedlines=(doc.text()).substring(0 , doc.text().length());
                                        npi.setplainText(wantedlines);
                                        npi.settitle(doc.title());
                                        listOfStemmedURLs.add(npi);
                                    }
                                }
                                else
                                {
                                    //The word contains that word without stemming
                                    //First check if it is in the list of URLListsStem
                                    //if so --> remove it from the URLListsStem and remove its data

                                    if(URLListsStem.containsKey(URL))
                                    {//This URL exists in the result
                                        //This URL exists in the URLListsStem
                                        //delete it
                                        index=URLListsStem.get(URL);
                                        listOfStemmedURLs.remove(index);
                                    }
                                    //insert it in URLLists and insert its data
                                    URLLists.put(URL, listOfURLsIndex);
                                    listOfURLsIndex++;
                                    NonPhraseURLInfo npi= new NonPhraseURLInfo();
                                    npi.setNo_Of_QueryWords_in_the_document(1);
                                    npi.setTF_IDF(TF_IDF);
                                    npi.setURL(URL);
                                    npi.setWeight(wordWeigt);
                                    Document doc = Jsoup.parse(URL);
                                    String wantedlines="";
                                    if ((doc.text().length())>30)
                                        wantedlines=(doc.text()).substring(0  , 30);
                                    else
                                        wantedlines=(doc.text()).substring(0  , doc.text().length());
                                    npi.setplainText(wantedlines);
                                    npi.settitle(doc.title());
                                    listOfURLs.add(npi);
                                }
                            }
                        }
                    }
                }
            }
        }

        NonPhraseURLInfoComparator com = new NonPhraseURLInfoComparator();
        Collections.sort(listOfURLs, com);
        Collections.sort(listOfStemmedURLs, com);
        listOfURLs.addAll(listOfStemmedURLs);

        return (ArrayList<FinalLinks>)((ArrayList<?>)(listOfURLs ));
    }


}
