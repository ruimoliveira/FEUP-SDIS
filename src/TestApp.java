import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

	private TestApp() {
	}

	public static void main(String[] args) throws IOException {

		/* arguments */
		if (args.length < 3 || args.length > 4) {
			System.out.println("java TestApp <peer_ap> <sub_protocol> <opnd>*");
			System.out.println("where:");
			System.out.println("	<peer_ap> Is the peer's access point.");
			System.out.println("	<sub_protocol> Is the operation the peer of the backup service must execute.");
			System.out.println("		BACKUP, RESTORE, DELETE, RECLAIM or STATE");
			System.out.println("	<opnd> * is the list of operands of the specified operation:");
			System.out.println("		<file_path> <replication degree> for BACKUP");
			System.out.println("		<file_path> RESTORE and DELETE");
			System.out.println("		<size> in KByte, for RECLAIM.");
			return;
		}

		args[1] = args[1].toUpperCase();
		if (args[1].compareTo("BACKUP") != 0 && args[1].compareTo("RESTORE") != 0 && args[1].compareTo("DELETE") != 0
				&& args[1].compareTo("RECLAIM") != 0 && args[1].compareTo("STATE") != 0) {
			System.out.println("Operations supported: BACKUP | RESTORE | DELETE | RECLAIM | STATE");
			return;
		}

		/* command handler */
		String host = "localhost";
		try {
			Registry registry = LocateRegistry.getRegistry(host);
			RMIservice stub = (RMIservice) registry.lookup(args[0]);

			switch (args[1]) {
			case "BACKUP":
				stub.backup(args[2], Integer.parseInt(args[3]));
				break;
			case "RESTORE":
				stub.restore();
				break;
			case "DELETE":
				stub.delete();
				break;
			case "RECLAIM":
				stub.reclaim();
				break;
			case "STATE":
				stub.state();
				break;
			}

		} catch (Exception e) {
			System.err.println("TestApp exception: " + e.toString());
			e.printStackTrace();
		}
	}
}