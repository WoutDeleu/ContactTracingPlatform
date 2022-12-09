package Visitor;

import Interfaces.RegistrarInterface;
import Interfaces.MixingProxyInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static Services.Methods.stringToHash;

public class VisitorGUI extends JFrame {
    private JFrame frame;
    private JButton enrollButton;
    private JComboBox selectFacility;
    private JButton visitButton;
    private JButton writeToFileButton;
    private JButton refreshButton;
    private Visitor visitor;
    private int saveDuration = 14; // Days before we delete the capsules from visiting a facility
    private int incubation = 0;
    private PublicKey publicKeyRegistrar = null;

    public static void main(String[] args) throws NotBoundException, RemoteException {
        VisitorGUI vistor = new VisitorGUI(new Visitor("Wout", "+32 69696696"));
    }
    public VisitorGUI(Visitor visitor) throws RemoteException, NotBoundException {
        this.visitor = visitor;

        String selector = " ___";
        frame = new JFrame("Visitor - " + visitor.getName());

        refreshButton = new JButton("REFRESH");
        enrollButton = new JButton("Enroll");
        visitButton = new JButton("Visit selected facility");
        visitButton = new JButton("Visit ");
        writeToFileButton = new JButton("Write logs to file");

        refreshButton.addActionListener(a -> {
            if (a.getSource() == refreshButton) {

                frame.dispose();
                try {
                    new VisitorGUI(this.visitor);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                } catch (NotBoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        enrollButton.addActionListener(a -> {
            /************************************** 1.2 USER ENROLLMENT *************************************/
            try {
                // fire to localhost port 2100
                Registry myRegistry = LocateRegistry.getRegistry("localhost", 2100);
                // search for RegistrarService
                RegistrarInterface impl = (RegistrarInterface) myRegistry.lookup("RegistrarService");

                incubation = impl.getINCUBATION_DAYS();

                // Register visitor to the registrar
                boolean registrationSuccessful = impl.registerVisitor(visitor.getName(), visitor.getPhoneNr());
                if (registrationSuccessful) System.out.println("Visitor data after enrollment: " + visitor);
                else System.out.println("Something went wrong during enrollment of: " + visitor);

                // Get a set of signed tokens
                visitor.setTokens(impl.getSignedTokens(visitor.getPhoneNr()));
                System.out.println("Visitor data after receiving tokens: " + visitor);

                // Key needed in 2. Visiting a facility
                publicKeyRegistrar = impl.getPublicKey();
            } catch (Exception e) {
                e.printStackTrace();
            }
            /************************************* 1.2 USER ENROLLMENT **************************************/
        });

        visitButton.addActionListener(a -> {
            /*********************************** 2. VISITING A FACILITY *************************************/
            try {
                if(selectFacility.getSelectedItem() != null) {

                    // visitor scans a QR code
                    System.out.println("select: " + (String) selectFacility.getSelectedItem());
                    Visit visit = visitor.scanQR((String) selectFacility.getSelectedItem()); // Also saves Visit in Visitor

                    // fire to localhost port 2200
                    Registry mixingProxyRegistry = LocateRegistry.getRegistry("localhost", 2200);
                    // search for MixingProxyService
                    MixingProxyInterface mpi = (MixingProxyInterface) mixingProxyRegistry.lookup("MixingProxyService");

                    // Create a capsule and send it to the MixingProxy to verify
                    // Capsule = timestamp, T_user_x_dayi, hash(Ri,num_CF_dayi) (hash uit de QR-code dus)
                    int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    ArrayList<byte[]> tokenPair = visitor.getAndRemoveToken(today);
                    visit.setTokenPair(tokenPair);

                    ArrayList<byte[]> signedConfirmation = mpi.verifyAndSendConfirmation(visit, publicKeyRegistrar);
                    Visualiser visualiser = new Visualiser(signedConfirmation.get(0));
                }

            } catch (Exception e) { e.printStackTrace(); }
            /*********************************** 2. VISITING A FACILITY *************************************/
        });

        writeToFileButton.addActionListener(a -> {
            /******************************** 3. REGISTERING INFECTED USER **********************************/
            visitor.exportVisits();
            /******************************** 3. REGISTERING INFECTED USER **********************************/
        });


        // fire to localhost port 2100
        Registry myRegistry = LocateRegistry.getRegistry("localhost", 2100);
        // search for RegistrarService
        RegistrarInterface impl = (RegistrarInterface) myRegistry.lookup("RegistrarService");

        selectFacility = new JComboBox(impl.getAllFacilityNames());

        frame.setLayout(new FlowLayout());
        frame.add(refreshButton);
        frame.add(enrollButton);
        frame.add(writeToFileButton);
        frame.add(visitButton);
        frame.add(selectFacility);
        frame.setSize(250, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
