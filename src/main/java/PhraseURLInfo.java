
public class PhraseURLInfo extends FinalLinks{
    int PhraseFrequency;

    int PageRank;
    int Weight;


    public int PhraseFrequency() {
        return  PhraseFrequency;
    }
    public int getPageRank() {
        return  PageRank;
    }
    public int getWeight() {
        return  Weight;
    }
    public void setPhraseFrequency(int PhraseFrequency)
    {
        this.PhraseFrequency=PhraseFrequency;
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
