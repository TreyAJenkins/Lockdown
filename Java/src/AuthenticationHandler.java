import javax.smartcardio.CardException;

/**
 * Created by Trey Jenkins on January 26, 2020 at 16:46
 */
public class AuthenticationHandler {

    private SmartcardHandler smartcardHandler;


    public AuthenticationHandler() {
        try {
            smartcardHandler = new SmartcardHandler();
        } catch (CardException e) {
            e.printStackTrace();
        }
    }

    public boolean ready() {
        try {
            return smartcardHandler.isCardPresent();
        } catch (CardException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean authenticate() {
        return true;
    }

}
