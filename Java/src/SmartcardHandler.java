import javax.smartcardio.*;
import javax.xml.bind.DatatypeConverter;
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

    public void test() {
        try {
            //connection.getATR();
            //System.out.println(sendAPDU("00A404000E315041592E5359532E444446303100"));

            System.out.println("ATR: " + getATR());

            // Find the location of the PSE record
            String resp = sendAPDU("00A404000E315041592E5359532E4444463031");
            System.out.println(resp);
            String DFName = hexToString(parseTLVbyTag(resp, "84"));
            String SFI = intToHex(((Integer.parseInt((parseTLVbyTag(resp, "88")), 16)) << 3) | 4);

            System.out.println("DF Name: " + DFName);
            System.out.println("SFI: " + SFI);

            // Actually get the PSE record
            resp = sendAPDU("00B201" + SFI + "00");
            System.out.println(resp);

            String AID = (parseTLVbyTag(resp, "4F"));
            System.out.println("AID: " + AID);

            String AIDLabel = hexToString(parseTLVbyTag(resp, "50"));
            System.out.println("Label: " + AIDLabel);

            // Select the application

            System.out.println("SND: " + "00A40400" + intToHex(AID.length()/2) + AID + "00");
            //00A4040007A0000000031010
            //00a4040007A000000003101000
            resp = sendAPDU("00A40400" + intToHex(AID.length()/2) + AID + "00");
            //System.out.println(resp);
            String PDOL = parseTLVbyTag(resp, "9F38");
            //System.out.println(PDOL);

            // GET PROCESSING OPTIONS request

            //resp = sendAPDU("80A80000048302" + PDOL);
            //System.out.println(   "80A800000283" + PDOL.substring(4) + "000000000000");
            resp = sendAPDU("80A800000783" + PDOL.substring(4) + "000000000000");
            //80A80000078305
            //80A80000028305
            System.out.println(resp);
            //String AFL = parseTLVbyTag(resp, "94");
            String[] tmpAFL = parseTLVbyTag(resp, "94").split("(?<=\\G.{8})");
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

    public String[] getCardInfo() throws CardException {
        // Find the location of the PSE record
        String resp = sendAPDU("00A404000E315041592E5359532E4444463031");
        String DFName = hexToString(parseTLVbyTag(resp, "84"));
        String SFI = intToHex(((Integer.parseInt((parseTLVbyTag(resp, "88")), 16)) << 3) | 4);
        // Actually get the PSE record
        resp = sendAPDU("00B201" + SFI + "00");
        String AID = (parseTLVbyTag(resp, "4F"));
        String AIDLabel = hexToString(parseTLVbyTag(resp, "50"));
        // Select the application
        resp = sendAPDU("00A40400" + intToHex(AID.length()/2) + AID + "00");
        String PDOL = parseTLVbyTag(resp, "9F38");
        // GET PROCESSING OPTIONS request
        resp = sendAPDU("80A800000783" + PDOL.substring(4) + "000000000000");
        String[] tmpAFL = parseTLVbyTag(resp, "94").split("(?<=\\G.{8})");
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
        int index = tlv.indexOf(tag);
        int size = Integer.parseInt(tlv.substring(index+tag.length(), index+tag.length()+2), 16);
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
