package tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface ServeurInterface extends Remote {

	int calculOperations(HashMap<String, ArrayList<Integer>> operations) throws RemoteException;

}
