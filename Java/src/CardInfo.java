import javax.smartcardio.CardTerminal;

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

    @Override
    public String toString() {
        return cardType + " " + cardNumber + " Exp: " + expireMonth + "/" + expireYear + " " + cardHolder;
    }
}
