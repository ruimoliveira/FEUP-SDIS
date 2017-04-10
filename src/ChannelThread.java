import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ChannelThread implements Runnable {

	protected boolean running;
	protected String messageType, version, senderID,fileID ;
	protected int chunkNo, replicationDeg;
	protected byte[] chunkData;
	DatagramPacket packet;
	
	public ChannelThread(DatagramPacket packet) throws IOException {
		this.running = true;
		this.packet = packet;
		
		String[] received = Utils.getHeader(packet.getData());
		System.out.println("CHANNELTHREAD: 22"+received[0]);
		this.messageType = received[0];
		this.version = received[1];
		this.senderID = received[2];
		this.fileID = received[3];
		
		if (!messageType.equals("DELETE")) {
			this.chunkNo = Integer.parseInt(received[4]);
		}
		
		if (messageType.equals("PUTCHUNK")) {
			this.replicationDeg = Integer.parseInt(received[5].trim());
		}
		
		if(messageType.equals("CHUNK") || messageType.equals("PUTCHUNK")){
			this.chunkData = Utils.getBody(packet.getData(), packet.getLength());
		}

		/*
		PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
		CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body> 
		STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
		*/
	}

	public void run() {
		switch(messageType){
		case "PUTCHUNK":
			putchunk();
			break;
		case "GETCHUNK":
			getchunk();
			break;
		case "DELETE":
			delete();
			break;
		case "REMOVED":
			removed();
			break;
		}
	}
	
	void putchunk(){
		
		/* Checks if file already exists */
		String filepath = "db/" + Peer.peerID + "/" + fileID;// + "/" + this.chunkNo;
		File folder = new File(filepath);
		if (!folder.exists()) {
			System.out.println("Creating folder to save new chunk...");
			folder.mkdirs();
		}
		/* Check if has enough space */
		File file = new File(folder.getPath()+"/"+this.chunkNo);
		if (!file.exists()) {
			//if (Peer.maxBytes < (folder.length() + this.chunkData.length) || Peer.maxBytes != 0) {
			//if(Peer.maxBytes < 64000){
			//	System.out.println("Can not save chunk. Not enough space.");
			//} else {
			FileOutputStream chunk;
				try {
					/* Stores file */
					chunk = new FileOutputStream(file);
					file.createNewFile();
					chunk.write(this.chunkData);
					chunk.close();
				} catch (IOException e) {
					System.out.println("CHANNELTHREAD: Error saving to file.");
					e.printStackTrace();
				}
			//}
		}else System.out.println("Chunk already stored.");
		// else System.out.println("Chunk already stored.");
		
		/* Waiting for an interruption for a random amount of time before sending response */
		Random rng = new Random();
		int r = rng.nextInt(401);
		
		try {
			TimeUnit.MILLISECONDS.sleep(r);
		} catch (InterruptedException e) {
			/*PUTCHUNK thread interrupted*/
			System.out.println("Error random");
			e.printStackTrace();
		}
		
		/* Send STORED message to mcSocket
		 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF> */
		byte[] msg = Utils.codeMessage("STORED", this.fileID, this.chunkNo, null);
     	DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mcAddress, Peer.mcPort);
		try {
			Peer.mcSocket.send(packet);
		} catch (IOException e) {
			System.out.println("CHANNELTHREAD: Could not respond to PUTCHUNK message");
		}
		System.out.println("CHANNELTHREAD: STORED message sent.");
		
	}
	
	public void getchunk() {
		
		String chunkpath = "db/" + Peer.peerID + "/" + fileID + "/" + this.chunkNo;
		File chunkFile = new File(chunkpath);
		Chunk chunk = null;
		byte[] msg = null;
		if (chunkFile.exists() && !chunkFile.isDirectory()) {
			/* Prepare message */
			byte[] chunk_bytes;
			try {
				chunk_bytes = Files.readAllBytes(chunkFile.toPath());
				chunk = new Chunk(this.fileID, this.chunkNo, 0, chunk_bytes.length, chunk_bytes);
				msg = Utils.codeMessage("CHUNK", this.fileID, this.chunkNo, chunk);
			} catch (IOException e) {
				System.out.println("CHANNELTHREAD: Error loading chunk to send.");
				return;
			}

			/* Wait for a random amount of time between 0 and 400 ms for a CHUNK message. */
			byte[] buf = new byte[64256];
			DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
			try {
				System.out.println("CHANNELTHREAD: Waiting for CHUNK interruption...");
				Peer.mdrSocket.setSoTimeout(400);
				Peer.mdrSocket.receive(rPacket);

				/* convert to string */
				String[] header = Utils.getHeader(buf);

				/* If a CHUNK message was received, means there's no need to send one. */
				if (header[0] != null)
					if (header[0].equals("CHUNK") && header[1].equals(Peer.protocolV)
							&& !(header[2].equals(Peer.peerID)) && header[3].equals(chunk.getFileID())
							&& Integer.parseInt(header[4]) == chunk.getChunkNo()) {
						System.out.println("CHANNELTHREAD: Received CHUNK response, no need to send another one.");
						return;
					}
			} catch (SocketTimeoutException e) {
			} catch (IOException e) {
				System.out.println("CHANNELTHREAD: Error reading from MDR Socket");
				return;
			}
			
	     	DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mdrAddress, Peer.mdrPort);
			try {
				Peer.mdrSocket.send(packet);
			} catch (IOException e) {
				System.out.println("CHANNELTHREAD: Error sending from MDR Socket");
				e.printStackTrace();
			}
		}
	}
	
	public void delete() {
		/*
		 * TODO received delete thread
		 * 1. apagar todos os chunks com fileID especificado
		 */
		String filepath = "db/" + Peer.peerID + "/" + fileID;
		File folder = new File(filepath);
		if (!folder.exists()) {
			System.out.println("CHANNELTHREAD: " + "Don't have any folder corresponding to the given file ID.");
		} else {
			File[] contents = folder.listFiles();
			if (contents != null) {
				for (File f : contents) {
					f.delete();
				}
			}
			folder.delete();
		}
	}
	
	public void removed(){
		/*TODO received removed thread
		 * 1. verifica se tem o ficheiro (se nao tiver acaba por aqui)
		 * 2. actualiza a contagem de replicas
		 * 3. se o numero de replicas nao ficar abaixo do desejado termina aqui a funcao
		 * 4. espera um tempo aleatorio entre 0 e 400 ms
		 * 5. se nao receber uma mensagem PUTCHUNK envia ele proprio um PUTCHUNK para mdbSocket
		 * 		se nao receber para a funcao
		 * */
		
	}

	public void stopThread() {
		running = false;
	}

	public boolean isRelated(String messageType, String fileID, String chunkNoString) {

		int chunkNo = Integer.parseInt(chunkNoString);
		if (running && this.messageType.equals(messageType) && this.fileID.equals(fileID) && this.chunkNo == chunkNo)
			return true;
		else
			return false;
	}
}
