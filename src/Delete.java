import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Delete extends Thread {
	String fileID = null;
	Delete(String fileID) {
		this.fileID = fileID;
	}

	public void run() {
			
		for (int i = 0; i < 3; i++) {
			byte[] msg = Utils.codeMessage("DELETE", this.fileID, 0, null);
			DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mcAddress, Peer.mcPort);

			try {
				Peer.mcSocket.send(packet);
			} catch (IOException e) {
				System.out.println("DELETE: Could not send DELETE message");
				e.printStackTrace();
			}
			Random rng = new Random();
			int r = rng.nextInt(201);
		
			try {
				TimeUnit.MILLISECONDS.sleep(r);
			} catch (InterruptedException e) {
				/*PUTCHUNK thread interrupted*/
				e.printStackTrace();
			}
		}
	}
}
