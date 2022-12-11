import Doctor.Doctor;
import Doctor.DoctorGUI;
import Facility.BarOwnerGUI;
import Facility.Facility;
import MatchingService.MatchingService;
import MixingProxy.MixingProxyGUI;
import MixingProxy.MixingProxy;
import Registrar.Registrar;
import Visitor.Visitor;
import Visitor.VisitorGUI;
import com.google.zxing.NotFoundException;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

public class GlobalMain {
    public static void main(String[] args) throws Exception {
        Registrar.main(args);
        MatchingService.main(args);
        MixingProxy.main(args);

        new BarOwnerGUI(new Facility("Hamann", "Vantegemstraat 3, 9230 Wetteren", "+32 9 333 77 77"));
        new VisitorGUI(new Visitor("Wannes", "+32 456 30 81 66"));
        new VisitorGUI(new Visitor("Wout", "+32 456 30 81 67"));
        new DoctorGUI(new Doctor("Toon Eeraerts"));
    }
}
