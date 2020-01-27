import javax.smartcardio.CardTerminal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Trey Jenkins on January 26, 2020 at 16:51
 */
public class CardInfo {
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cardType;
    private String cardHolder;

    private String appID;
    private String appName;

    private String hash;

    public CardInfo(String[] cardInfo) {
        this.cardType = cardInfo[2];
        this.cardHolder = cardInfo[4];

        this.appID = cardInfo[0];
        this.appName = cardInfo[1];

        // Parse the Track 2 data
        String track2[] = cardInfo[3].split("D");
        this.cardNumber = track2[0];
        this.expireYear = track2[1].substring(0,2);
        this.expireMonth = track2[1].substring(2,4);

        // Compute the hash
        try {
            String hashString = appID + appName + cardType + cardNumber + expireMonth + expireYear + cardHolder;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(hashString.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            this.hash = String.format("%064X", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            this.hash = "";
        }

        // Keep only the last 4 of the card number for privacy reasons
        this.cardNumber = this.cardNumber.substring(this.cardNumber.length()-4);
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpireMonth() {
        return expireMonth;
    }

    public String getExpireYear() {
        return expireYear;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public String getAppID() {
        return appID;
    }

    public String getAppName() {
        return appName;
    }

    public String getHash() {
        return hash.substring(hash.length()-8);
        //return hash;
    }

    @Override
    public String toString() {
        return cardType + " " + cardNumber + " Exp: " + expireMonth + "/" + expireYear + " " + cardHolder + " [" + getHash() + "]";
    }
}
