import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.lang.Thread;

public class Backup extends Thread {
	Chunk chunk;
	MulticastSocket mdb;
	ArrayList<String> guardians = new ArrayList<String>();

	Backup(Chunk chunk, MulticastSocket mdb) {
		this.chunk = chunk;
		this.mdb = mdb;
	}

	public void run() {
		for (int i = 0; i < 5; i++) {

			/* SEND START */
			byte[] msg = Utils.codeMessage("PUTCHUNK", chunk.getFileID(), chunk.getChunkNo(), chunk);

			/*String msg = "PUTCHUNK " + Peer.protocolV+" "+Peer.peerID+" "+ chunk.getFileID() + " "+ chunk.getChunkNo() + " " + chunk.getReplication()+ " "+CRLF+CRLF+ new String(chunk.getData());
			System.out.println(msg);
			*/
			//	System.out.println(mdb.getLocalPort());	
		
			//byte[] msg  = chunk.getData();
			//String msg = new String(chunk.getData());

         	DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mdbSocket.getLocalAddress(), Peer.mdbSocket.getLocalPort());
         	
			try {
				Peer.mdbSocket.send(packet);
			} catch (IOException e) {
				System.out.println("BACKUP: Could not send PUTCHUNK message");
				e.printStackTrace();
			}
			System.out.println("BACKUP: Sent PUTCHUNK message");
			/* SEND FINNISH */

			
			/* RECEIVE START */
			long startTime = System.currentTimeMillis();
			do {
				byte[] buf = new byte[256];
				DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
				
				try {
					Peer.mdbSocket.receive(rPacket);
				} catch (IOException e) {
					System.out.println("BACKUP: Could not receive STORED response");
					e.printStackTrace();
				}
				
				/*convert to string*/
				String[] msgReceived = Utils.decodeMessage(buf);

				/* if msg is relevant then saves the peerID of the sender who stored the chunk> */
				if (msgReceived[0].equals("STORED") && msgReceived[1].equals(Peer.protocolV) && !isGuardian(msgReceived[2])
						&& msgReceived[3].equals(chunk.getFileID()) && Integer.parseInt(msgReceived[4]) == chunk.getChunkNo()) {
					System.out.println("BACKUP: Received STORED response");
					guardians.add(msgReceived[2]);
				}
			} while ((System.currentTimeMillis() - startTime) < ((2 ^ i) * 1000));

			// if has enough guardians stops
			if (guardians.size() >= this.chunk.getReplication()) {
				break;
			}
			/* RECEIVE FINNISH */
			
			/*TODO: save guardians as requested in project specifications*/
		}
	}

	private boolean isGuardian(String guardian) {
		for (int i = 0; i < guardians.size(); i++) {
			if (guardians.get(i).equals(guardian)) {
				return true;
			}
		}
		return false;
	}
}
