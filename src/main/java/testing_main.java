import java.io.IOException;
import java.util.ArrayList;

public class testing_main {
    public static void main(String[] args) throws IOException {


        QueryProcessor Qr = new QueryProcessor("kitchen");
        ArrayList<FinalLinks> results = Qr.queryProcessorLogic();
        if (results==null){
            System.out.println("null");
        }else{
            System.out.println("result");
            System.out.println(results.size());

        }

    }


}
