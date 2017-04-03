import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class Restore extends Thread {

	File recipentFile;
	String fileID;
	MulticastSocket mc;
	
	Restore(File f, String fileID) {
		this.recipentFile = f;
		this.fileID = fileID;
	}

	public void run() {
		boolean lastChunk = false;
		int chunkNo = 0;
		while(!lastChunk){
			
			/* Start sending GETCHUNK message*/
			byte[] msg = Utils.codeMessage("GETCHUNK", this.fileID, chunkNo, null);

         	DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mcSocket.getLocalAddress(), Peer.mcSocket.getLocalPort());
			try {
				Peer.mcSocket.send(packet);
			} catch (IOException e) {
				System.out.println("RESTORE: Could not send GETCHUNK message");
				e.printStackTrace();
			}
			System.out.println("RESTORE: Sent GETCHUNK message");
			/* Finish sending*/
			
			/*Start receive*/
			byte[] buf = new byte[256];
			DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
			
			try {
				Peer.mdrSocket.receive(rPacket);
			} catch (IOException e) {
				System.out.println("RESTORE: Could not receive CHUNK response");
				e.printStackTrace();
			}
			
			/*convert to string*/
			String[] msgReceived = Utils.decodeMessage(buf);

			/* if msg is relevant then saves the peerID of the sender who stored the chunk> */
			if (msgReceived[0].equals("CHUNK") && msgReceived[1].equals(Peer.protocolV)
					&& msgReceived[3].equals(this.fileID)
					&& Integer.parseInt(msgReceived[4]) == chunkNo) {
				System.out.println("RESTORE: Received CHUNK response");
				
				byte[] chunk = Utils.stringToByte(msgReceived[5]);
				
				/*TODO: save CHUNK*/

				if(chunk.length < 64000){
					lastChunk = true;
				}
			}
			
			chunkNo++;
		}
		
	}
}
