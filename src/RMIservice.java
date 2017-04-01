import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIservice extends Remote {
	void backup(String file_path, int rep_degree) throws RemoteException;

	/* TODO */
	void restore() throws RemoteException;

	/* TODO */
	void delete() throws RemoteException;

	/* TODO */
	void reclaim() throws RemoteException;

	/* TODO */
	void state() throws RemoteException;
}
