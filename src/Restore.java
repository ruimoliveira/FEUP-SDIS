import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class Restore extends Thread {

	File recipentFile;
	String fileID;
	
	Restore(File f, String fileID) {
		this.recipentFile = f;
		this.fileID = fileID;
	}

	public void run() {
		ByteArrayOutputStream fileData = new ByteArrayOutputStream( );
		boolean lastChunk = false, failedToReceive = false;
		int chunkNo = 0;
		while(!lastChunk && !failedToReceive){
			
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

			/* RECEIVE START */

			for (int i = 0; i < 5; i++) {
				byte[] buf = new byte[64256];
				DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
	
				try {
					System.out.println("RESTORE: Waiting for CHUNK message...");
					int time = 1000 * (int) Math.pow(2, i);
					Peer.mdrSocket.setSoTimeout(time);
					Peer.mdrSocket.receive(rPacket);
	
					/* convert to string */
					String[] msgReceived = Utils.getHeader(buf);
	
					/*
					 * if msg is relevant then saves the peerID of the sender who
					 * stored the chunk>
					 */
					if (msgReceived[0] != null)
						if (msgReceived[0].equals("CHUNK") && msgReceived[1].equals(Peer.protocolV)
								&& !(msgReceived[2].equals(Peer.peerID)) && msgReceived[3].equals(this.fileID)
								&& Integer.parseInt(msgReceived[4]) == chunkNo) {
							System.out.println("RESTORE: Received CHUNK response");
							
							byte[] body = Utils.getBody(buf);
							if (body == null || body.length < 64000) {
								lastChunk = true;
							}
							if (body != null) {
								fileData.write(body);
								break;
							}
							
						}
	
				} catch (SocketTimeoutException e) {
					if (i == 4) {
						failedToReceive = true;
						System.out.println("RESTORE: No CHUNK response received");
					}
				} catch (IOException e) {
					System.out.println("RESTORE: Could not receive CHUNK response");
					e.printStackTrace();
				}
			}
			/* RECEIVE FINNISH */
			
			chunkNo++;
		}

		FileOutputStream fileSaver;
		if (!failedToReceive) {
			/* Save file */
			byte[] body = fileData.toByteArray();
			try {
				fileSaver = new FileOutputStream(this.recipentFile);
				fileSaver.write(body);
				fileSaver.close();
			} catch (IOException e) {
				System.out.println("RESTORE: Error writing data to file.");
				e.printStackTrace();
			}
			System.out.println("RESTORE: File saved successfully!");
		}
	}
}
