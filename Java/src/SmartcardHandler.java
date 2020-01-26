import javax.smartcardio.*;
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Trey Jenkins on January 26, 2020 at 13:08
 */
public class SmartcardHandler {
    private CardTerminal terminal;
    private Card connection;
    private CardChannel cardChannel;

    public String getTerminalName() {
        return terminal.getName();
    }

    public boolean isCardPresent() throws CardException {
        return terminal.isCardPresent();
    }

    public boolean connect() {
        try {
            connection = terminal.connect("T=0");
            cardChannel = connection.getBasicChannel();
            return true;
        } catch (CardException e) {
            return false;
        }
    }

    public String getATR() {
        return DatatypeConverter.printHexBinary(connection.getATR().getBytes());
    }

    private String[] selectApplication(String pse) {

        String apps[] = pse.split("4F");
        String application[][] = new String[apps.length-1][2];

        for (int i = 1; i < apps.length; i++) {
            int AIDlen = Integer.parseInt(apps[i].substring(0,2),16);
            //System.out.println(apps[i]);
            //System.out.println("AIDlen: " + AIDlen*2);
            String AID = apps[i].substring(2,2+AIDlen*2);
            //System.out.println("AID: " + AID);
            String AIDLabel = hexToString(parseTLVbyTag(apps[i], "50"));
            //System.out.println("Label: " + AIDLabel);
            String[] res = {AID, AIDLabel};
            application[i-1] = res;
        }
        for (String[] app:application) {
            //System.out.println(Arrays.toString(app));
            if (app[1].contains("Visa") || app[1].contains("Master"))
                return app;
        }
        return application[0];

        //return application;
    }

    public void test() {
        try {
            //connection.getATR();
            //System.out.println(sendAPDU("00A404000E315041592E5359532E444446303100"));

            System.out.println("ATR: " + getATR());

            // Find the location of the PSE record
            String resp = sendAPDU("00A404000E315041592E5359532E4444463031");
            System.out.println(resp);
            String DFName = hexToString(parseTLVbyTag(resp, "84"));
            System.out.println("DF Name: " + DFName);
            String SFI = intToHex(((Integer.parseInt((parseTLVbyTag(resp, "88")), 16)) << 3) | 4);

            System.out.println("SFI: " + SFI);

            // Actually get the PSE record
            resp = sendAPDU("00B201" + SFI + "00");
            System.out.println(resp);



            // Select the application
            String[] application = selectApplication(resp);
            String AID = application[0];
            String AIDLabel = application[1];
            System.out.println(Arrays.toString(application));

            System.out.println("SND: " + "00A40400" + intToHex(AID.length()/2) + AID + "00");
            //00A4040007A0000000031010
            //00a4040007A000000003101000
            resp = sendAPDU("00A40400" + intToHex(AID.length()/2) + AID + "00");
            System.out.println(resp);
            String PDOL = "000000";

            // GET PROCESSING OPTIONS request
            if (resp.contains("9F38")) {
                PDOL = parseTLVbyTag(resp, "9F38");
                System.out.println("PDOL: " + PDOL);
                resp = sendAPDU("80A800000783" + PDOL.substring(4) + "000000000000");
            } else {
                System.out.println("PDOL: " + PDOL);
                resp = sendAPDU("80A8000002830000");
            }

            //resp = sendAPDU("80A80000048302" + PDOL);
            //System.out.println(   "80A800000283" + PDOL.substring(4) + "000000000000");
            //80A80000078305
            //80A80000028305
            System.out.println("GPO RX: " + resp);
            //String AFL = parseTLVbyTag(resp, "94");
            String[] tmpAFL = {resp.substring(resp.length()-12, resp.length()-4)};
            if (resp.contains("94"))
                tmpAFL = parseTLVbyTag(resp, "94").split("(?<=\\G.{8})");
            String[][] AFL = new String[tmpAFL.length][4];
            for (int i = 0; i < tmpAFL.length; i++) {
                AFL[i] = parseAFL(tmpAFL[i]);
                System.out.println("AFL: " + Arrays.toString(AFL[i]));
            }

            // Read the first record
            resp = sendAPDU("00B2" + AFL[0][1] + AFL[0][0] + "00");
            System.out.println(resp);

            String track2 = parseTLVbyTag(resp, "57");
            String cardholder = hexToString(parseTLVbyTag(resp, "5F20"));
            System.out.println("Track 2: " + track2);
            System.out.println("Cardholder: " + cardholder);

        } catch (CardException e) {
            e.printStackTrace();
        }
    }

    public String[] getRawCardInfo() throws CardException {
        // Find the location of the PSE record
        String resp = sendAPDU("00A404000E315041592E5359532E4444463031");
        String DFName = hexToString(parseTLVbyTag(resp, "84"));
        String SFI = intToHex(((Integer.parseInt((parseTLVbyTag(resp, "88")), 16)) << 3) | 4);
        // Actually get the PSE record
        resp = sendAPDU("00B201" + SFI + "00");
        // Select the application
        String[] application = selectApplication(resp);
        String AID = application[0];
        String AIDLabel = application[1];
        // Send select command
        resp = sendAPDU("00A40400" + intToHex(AID.length()/2) + AID + "00");
        String PDOL = "000000";

        // GET PROCESSING OPTIONS request
        if (resp.contains("9F38")) {
            PDOL = parseTLVbyTag(resp, "9F38");
            resp = sendAPDU("80A800000783" + PDOL.substring(4) + "000000000000");
        } else {
            resp = sendAPDU("80A8000002830000");
        }

        String[] tmpAFL = {resp.substring(resp.length()-12, resp.length()-4)};
        if (resp.contains("94"))
            tmpAFL = parseTLVbyTag(resp, "94").split("(?<=\\G.{8})");
        String[][] AFL = new String[tmpAFL.length][4];
        for (int i = 0; i < tmpAFL.length; i++) {
            AFL[i] = parseAFL(tmpAFL[i]);
        }

        // Read the first record
        resp = sendAPDU("00B2" + AFL[0][1] + AFL[0][0] + "00");

        String track2 = parseTLVbyTag(resp, "57");
        String cardholder = hexToString(parseTLVbyTag(resp, "5F20"));

        String[] response = {DFName, AID, AIDLabel, track2, cardholder.trim()};
        return response;
    }

    public CardInfo getCardInfo() throws CardException {
        return new CardInfo(getRawCardInfo());
    }

    public void close() throws CardException {
        //cardChannel.close();
        connection.disconnect(true);
    }

    private String[] parseAFL(String AFL) {
        String SFI = intToHex(Integer.parseInt(AFL.substring(0,2), 16) | 4);
        String first = AFL.substring(2,4);
        String last = AFL.substring(4,6);
        String offline = AFL.substring(6,8);
        String[] ret = {SFI, first, last, offline};
        return ret;
    }

    private String intToHex(int data) {
        return String.format("%1$02X", data);
    }

    private String hexToString(String hex) {
        return new String(DatatypeConverter.parseHexBinary(hex));
    }

    private String parseTLVbyTag(String tlv, String tag) {
        int index = 0;// = tlv.indexOf(tag);
        String[] tlvArr = tlv.split("(?<=\\G.{2})");
        //System.out.println(Arrays.toString(tlvArr));
        String[] tagArr = tag.split("(?<=\\G.{2})");
        //System.out.println(Arrays.toString(tagArr));

        boolean found = false;
        for (int i = 0; i < tlvArr.length; i++) {
            if (tlvArr[i].equals(tagArr[0])) {
                if (!found) index = i*2;
                found = true;
            }
        }

        if (tagArr.length>1) {
            index = tlv.indexOf(tag);
        }


        //System.out.println(index);
        //System.out.println(tlv);
        //System.out.println(tlv.substring(index+tag.length(), index+tag.length()+2));
        int size = Integer.parseInt(tlv.substring(index+tag.length(), index+tag.length()+2), 16);
        if (index > tlv.length()) {
            System.out.println("Bad Index: got " + index);
            System.out.println("Bad Index: " + tlv);
            System.out.println("Bad Index: " + tag);
        }
        return tlv.substring(index+tag.length()+2, index+tag.length()+2+(size*2));


    }

    private String sendAPDU(String cmd) throws CardException {
        byte[] cmdArray = hexStringToByteArray(cmd);
        ResponseAPDU resp = cardChannel.transmit(new CommandAPDU(cmdArray));
        byte[] respB = resp.getBytes();
        String hex = DatatypeConverter.printHexBinary(respB);
        return hex;
    }

    /**
     * Constructor for SmartcardHandler
     * @throws CardException
     */
    public SmartcardHandler() throws CardException {
        TerminalFactory tf = TerminalFactory.getDefault();
        //List<CardTerminal> terminals = tf.terminals().list();
        // Get the first available terminal
        terminal = tf.terminals().list().get(0);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
