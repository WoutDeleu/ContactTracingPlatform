package Visitor;

import MixingProxy.Entry;
import Services.Methods;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

import static Services.Methods.*;

public class Visitor implements Serializable {

    private String name;
    private String phoneNr;
    private Map<String, Visit> visits; // key = CF
    private MonthSignedTokenList tokens;
    private ArrayList<byte[]>[] usedTokens;
    private KeyPair keyPair;
    private List<Entry> infectedEntries;

    public Visitor(String name, String phone) {
        this.name = name;
        this.phoneNr = phone;
        visits = new HashMap();
        this.keyPair = getKeyPair();
        usedTokens =  null;
    }

    public String getName() { return name; }
    public String getPhoneNr() {
        return phoneNr;
    }
    public PublicKey getPublicKey () {
        return keyPair.getPublic();
    }
    public Map<String, Visit> getVisits(){
        return visits;
    }


    /************************************* 1.2 USER ENROLLMENT *************************************/
    public void setTokens(MonthSignedTokenList tokens) {
        this.tokens = tokens;
    }
    public MonthSignedTokenList getTokens() {
        return tokens;
    }
    /************************************* 1.2 USER ENROLLMENT *************************************/


    /*********************************** 2. VISITING A FACILITY *************************************/
    public Visit scanQR(String facilityName)  throws NotFoundException, IOException {
            // Read QR
            // TODO For now: just dummy procedure where it selects current day
            Calendar current_dateTime = Calendar.getInstance();
            String filename = "QRCodes_" + facilityName +"/QRCode_day" + current_dateTime.get(Calendar.DAY_OF_MONTH) + ".jpg";
            String[] qr = Methods.separateString(readQRCode(filename));

            String R_i = qr[0]; // random number
            String CFpart1 = qr[1]; // Unique identifier of the facility
            String CFpart2 = qr[2];
            String CFpart3 = qr[3];
            String CFpart4 = qr[4];
            String CF = joinStrings(new String[]{CFpart1, CFpart2, CFpart3, CFpart4});
            String nothing = qr[5]; // Necessary for double ;;
            String H = qr[6]; // Hash

            // Get Current time
            String currentTime = timeStampToString(LocalDateTime.now());

            // Print result log
            System.out.println("Lezen van de QR-code:");
            System.out.println("\t Random number: "+R_i);
            System.out.println("\t Unique Identifier CF: "+ CF);
            System.out.println("\t Hash: "+ H);
            System.out.println("\t Entry time: " + currentTime);

            Visit visit = new Visit(R_i, CF, H, currentTime);
            addVisit(visit);
            System.out.println("Na toevoegen visits: "+visits);

            return visit;
    }
    public static String readQRCode(String file) throws IOException, NotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(ImageIO.read(fileInputStream));
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer((bufferedImageLuminanceSource)));

        return (new MultiFormatReader().decode(binaryBitmap)).getText();
    }
    public void addVisit(Visit visit) {
        visits.put(visit.getScanTime(), visit);
    }
    public ArrayList<byte[]> getAndRemoveToken(int today) {
        ArrayList<byte[]> currentTokens = tokens.getAndRemoveSignatureToken(today);

        // Update usedTokens
        if(usedTokens == null) {
            usedTokens = new ArrayList[1];
            usedTokens[0] = currentTokens;
        }
        else {
            ArrayList<byte[]> [] newList = new ArrayList[usedTokens.length+1];
            for(int i=0; i< usedTokens.length; i++) {
                newList[i] = usedTokens[i];
            }
            newList[usedTokens.length] = currentTokens;
            usedTokens = newList;
        }
        return currentTokens;
    }
    public ArrayList<byte[]>[] getUsedTokens(){
        return usedTokens;
    }
    public void removeExpiredVisits(int INCUBATION_DAYS) {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        ArrayList<String> toRemove = new ArrayList<>();
        for(String date : visits.keySet()) {
            if(today > stringToDate(date).getDayOfMonth() + INCUBATION_DAYS) {
                toRemove.add(date);
            }
        }
        for(String date : toRemove) {
            visits.remove(date);
        }
    }
    /*********************************** 2. VISITING A FACILITY *************************************/


    /******************************** 3. REGISTERING AN INFECTED USER *******************************/
    public void exportVisits() {
        try {
            System.out.println("Export visits: "+visits);
            FileWriter myWriter = new FileWriter("src/main/java/Visitor/visitLogs.txt");
            for (Visit v : visits.values()) {
                String visit = v.getLogString();
                myWriter.write(visit+"\n");
            }
            myWriter.close();
            System.out.println("Visitor successfully wrote logs to the file.");
        } catch (IOException e) {e.printStackTrace();}
    }
    /******************************** 3. REGISTERING AN INFECTED USER *******************************/


    /**************************** 4. INFORMING POSSIBLY INFECTED USERS ******************************/
    public void setInfectedEntries(List<Entry> allEntries) {
        infectedEntries = allEntries;
        System.out.println("Infected entries goed ontvangen in de visitor: "+allEntries);
    }
    // Visitor is possible infected if one of the hashes can be found in his own visits list
    // And if the time windows overlap
    public void checkIfInfected() {
        for (Entry entry : infectedEntries) {
            String hash = hashToString(entry.getHash());
            // Search for a visit with the same hash
            for (Visit visit : visits.values()) {
                if (visit.getH().equals(hash)) {
                    // Same hash => both visited the same facility
                    // Check if the timestamps overlap
                    LocalDateTime time = stringToTimeStamp(visit.getScanTime());
                    if (time.isAfter(entry.getBeginTimeWindow()) && time.isBefore(entry.getEndTimeWindow())) {
                        System.out.println("!!! Risico op besmetting !!!");
                        //Todo zou cool zijn als we dit in de GUI krijgen
                        notifyReceived();
                    }

                }
            }
        }
    }
    public void notifyReceived() {
        // todo: zie docs
    }
    /**************************** 4. INFORMING POSSIBLY INFECTED USERS ******************************/


    @Override
    public String toString() {
        return "Visitor{" +
                "name='" + name + '\'' +
                ", phoneNr='" + phoneNr + '\'' +
                ", visits=" + visits +
                ", tokens=" + tokens +
                ", usedTokens=" + Arrays.toString(usedTokens) +
                ", keyPair=" + keyPair +
                '}';
    }

    public String toGUIString() {
        return "Visitor{" + '\n' +
                "name='" + name + '\'' + '\n' +
                ", phoneNr='" + phoneNr + '\'' + '\n' +
                ", visits=" + visits + '\n' +
                ", tokens= " + tokens + '\n' +
                ", usedTokens=" + Arrays.toString(usedTokens) + '\n' +
                ", keyPair=" + keyPair +
                '}';
    }
}
