import java.util.Comparator;

public class NonPhraseURLInfoComparator implements Comparator<NonPhraseURLInfo> {

    @Override

    public int compare (NonPhraseURLInfo o1, NonPhraseURLInfo o2) {
        return (int) (0.3*o1.TF_IDF+0.2*o1.No_Of_QueryWords_in_the_document+0.25*o1.PageRank+0.25*(o1.Weight/20)-0.3*o2.TF_IDF+0.2*o2.No_Of_QueryWords_in_the_document+0.25*o2.PageRank+0.25*(o2.Weight/20)) ;
    }
}