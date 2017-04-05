import java.net.*;
import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements RMIservice {

	public static String peerID, protocolV, serviceAP;
	public static InetAddress mcAddress, mdbAddress, mdrAddress;
	public static int mcPort, mdbPort, mdrPort;
	public static MulticastSocket mcSocket, mdbSocket, mdrSocket;
	public static int maxBytes = 0;
	
	/* TODO: decidir como/se se guarda listagem de chunks ou ficheiros ou wtv */
	static ArrayList<ArrayList<String>> db = new ArrayList<ArrayList<String>>();

	public static void main(String[] args) throws IOException {

		/* usage */
		if (args.length != 9) {
			System.out.println(
					"java Peer <protocol_version> <peer_id> <service_access_point> <mcAddress> <mcPort> <mdbAddress> <mdbPort> <mdrAddress> <mdrPort>");
			/*
			System.out.println("where:");
			System.out.println("	<protocol_version> 0|1 for with/without improvements respectively");
			System.out.println("	<peer_id> this peer's ID");
			System.out.println("	<service_access_point> TODO: complete usage");
			System.out.println("	<mcAddress> <mcPort> Address and Port for Multicast command messages");
			System.out.println("	<mdbAddress> <mdbPort> Address and Port for Multicast data backup messages");
			System.out.println("	<mdrAddress> <mdrPort> Address and Port for Multicast data restore messages");
			*/
			return;
		}

		protocolV = args[0];
		peerID = args[1];
		serviceAP = args[2];

		mcAddress = InetAddress.getByName(args[3]);
		mcPort = Integer.parseInt(args[4]);

		mdbAddress = InetAddress.getByName(args[5]);
		mdbPort = Integer.parseInt(args[6]);

		mdrAddress = InetAddress.getByName(args[7]);
		mdrPort = Integer.parseInt(args[8]);

		try {
			Peer obj = new Peer();
			RMIservice stub = (RMIservice) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(serviceAP, stub);

			System.err.println("PEER: Server ready");
		} catch (Exception e) {
			System.err.println("PEER: Server exception: " + e.toString());
			e.printStackTrace();
		}

		/* creates folder to save chunks if it doesn't exist already */
		File f = new File("database");
		if (!f.exists() || !f.isDirectory()) {
			System.out.println("PEER: Creating directory for file storage...");
			f.mkdir();
			System.out.println("PEER: path is " + f.getPath());
		}

		mcSocket = new MulticastSocket(mcPort);
		mdbSocket = new MulticastSocket(mdbPort);
		mdrSocket = new MulticastSocket(mdrPort);

		mcSocket.joinGroup(mcAddress);
		mdbSocket.joinGroup(mdbAddress);
		mdrSocket.joinGroup(mdrAddress);

		/* init channels */
		System.out.println("PEER: MC channel start");
		new Channel(mcSocket).start();
		System.out.println("PEER: MDB channel start");
		new Channel(mdbSocket).start();
		System.out.println("PEER: MDR channel start");
		new Channel(mdrSocket).start();
	}

	// file_path, rep
	public void backup(String file_path, int rep_degree) {
		System.out.println("PEER: Request for file backup received.");

		File f = new File(file_path);
		if (!f.exists()) {
			System.out.println("PEER: File doesn't exist.");
			// f.mkdir();
			// System.out.println(f.getPath());
		} else {
			System.out.println("PEER: File loaded.");
			try {

				byte[] file_bytes = Files.readAllBytes(f.toPath());
				System.out.println("PEER: file size is " + file_bytes.length + " byte");

				String fileID = MyFile.makeFileID(f);
				System.out.println("PEER: SHA256 code is " + fileID);
				
				MyFile myfile = new MyFile(fileID, peerID, rep_degree, file_bytes.length, file_bytes);
				System.out.println("PEER: Num of chunks is " + myfile.chunks.size());
				int total = 0;
				for (int i = 0; i < myfile.chunks.size(); i++) {
					total = total + myfile.chunks.get(i).getSize();
					new Backup(myfile.chunks.get(i), mdbSocket).start();
				}
				System.out.println("PEER: Size Total Chunks is " + total);

			} catch (IOException e) {
				System.err.println("Peer exception: " + e.toString());
				e.printStackTrace();
			}
		}

		// create
	}

	public void restore(String file_path) {
		System.out.println("PEER: Request for file restore received.");
		
		File f = new File(file_path);
		if (!f.exists()) {
			System.out.println("PEER: File to receive data not created.");
			return;
		} else {
			String fileID = MyFile.makeFileID(f);
			System.out.println("PEER: SHA256 code is " + fileID);
			
			new Restore(f, fileID).start();
		}
	}

	public void delete() {
		System.out.println("recebi pedido de delete");
		/* TODO: Delete.java */
	}

	public void reclaim() {
		System.out.println("recebi pedido de reclaim");
		/* TODO: Reclaim.java */
	}

	public void state() {
		System.out.println("recebi pedido de state");
		/* TODO: print peer status */
	}

	/**/
	public Peer() {
	}
}
