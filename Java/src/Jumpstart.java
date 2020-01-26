import javax.smartcardio.CardException;
import java.util.Arrays;

/**
 * Created by Trey Jenkins on January 26, 2020 at 13:08
 */
public class Jumpstart {

    public static void main(String[] args) throws CardException {
        SmartcardHandler handler = new SmartcardHandler();
        System.out.println("Terminal name: " + handler.getTerminalName());
        System.out.println("Card present: " + handler.isCardPresent());
        if (!handler.isCardPresent()) {
            System.out.println("Waiting for card");
            while (!handler.isCardPresent()) {}
        }
        handler.connect();
        String[] cardInfo = handler.getCardInfo();
        handler.close();
        System.out.println(Arrays.toString(cardInfo));
    }

}
