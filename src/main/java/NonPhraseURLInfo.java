//import java.util.Comparator;

public  class NonPhraseURLInfo extends FinalLinks {
    int TF_IDF;
    int No_Of_QueryWords_in_the_document;
    int PageRank;
    int Weight;


    public int getTF_IDF() {
        return  TF_IDF;
    }
    public int getNo_Of_QueryWords_in_the_document() {
        return  No_Of_QueryWords_in_the_document;
    }

    public int getPageRank() {
        return  PageRank;
    }
    public int getWeight() {
        return  Weight;
    }
    public void setTF_IDF(int TF_IDF)
    {
        this.TF_IDF=TF_IDF;
    }
    public void setNo_Of_QueryWords_in_the_document(int No_Of_QueryWords_in_the_document)
    {
        this.No_Of_QueryWords_in_the_document=No_Of_QueryWords_in_the_document;
    }
    public void setPageRank(int PageRank)
    {
        this.PageRank=PageRank;
    }
    public void setWeight(int Weight)
    {
        this.Weight=Weight;
    }

}

