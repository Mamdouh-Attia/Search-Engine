import java.util.Comparator;

public class PhraseURLInfoComparator implements Comparator<PhraseURLInfo> {

    @Override

    public int compare (PhraseURLInfo o1, PhraseURLInfo o2) {
        return (int) (0.4*o1.PhraseFrequency+0.3*o1.PageRank+0.3*(o1.Weight/20)- 0.4*o2.PhraseFrequency+0.3*o2.PageRank+0.3*(o2.Weight/20)) ;
    }
}