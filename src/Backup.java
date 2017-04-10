import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Backup implements Runnable {
	Chunk chunk;
	ArrayList<String> guardians = new ArrayList<String>();

	public Backup(Chunk chunk) {
		this.chunk = chunk;
	}

	public void run() {

		for (int i = 0; i < 5; i++) {
			/* SEND START */
			byte[] msg = Utils.codeMessage("PUTCHUNK", chunk.getFileID(), chunk.getChunkNo(), chunk);
			DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mdbAddress, Peer.mdbPort);

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
			byte[] buf = new byte[256];
			DatagramPacket rPacket = new DatagramPacket(buf, buf.length);

			long time = 1000 * (int) Math.pow(2, i);
			long initTime = System.currentTimeMillis();
			// long timeLapse = 0;
			while (false || (System.currentTimeMillis() - initTime) < time/* time >= 0 */) {
				// time = time - timeLapse;
				// long initTime = System.currentTimeMillis();
				try {
					Peer.mcSocket.setSoTimeout(20);
					Peer.mcSocket.receive(rPacket);

					/* convert to string */
					String[] header = Utils.getHeader(buf);
					// byte[] body = Utils.getBody(buf);

					/*
					 * if msg is relevant then saves the peerID of the sender
					 * who stored the chunk>
					 */
					if (header[0] != null)
						if (header[0].equals("STORED") && header[1].equals(Peer.protocolV)
								&& !(header[2].equals(Peer.peerID)) && !isGuardian(header[2])
								&& header[3].equals(chunk.getFileID())
								&& Integer.parseInt(header[4]) == chunk.getChunkNo()) {
							System.out.println("BACKUP: Received STORED response");
							guardians.add(header[2]);
						}

				} catch (SocketTimeoutException e) {
				} catch (IOException e) {
					System.out.println("BACKUP: Could not receive STORED response");
					e.printStackTrace();

				}
				// System.out.println("BACKUP: timelapse..." + timeLapse);
				// timeLapse = System.currentTimeMillis() - initTime;
			}

			// if has enough guardians stops
			if (guardians.size() >= this.chunk.getReplication()) {
				System.out.println("BACKUP: has enough guardians - " + guardians.size());
				break;
			}

		}
		System.out.println("BACKUP: finished");

	}

	/* TODO: save guardians as requested in project specifications */

	private boolean isGuardian(String guardian) {
		for (int i = 0; i < guardians.size(); i++) {
			if (guardians.get(i).equals(guardian)) {
				return true;
			}
		}
		return false;
	}

}
