import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ChannelThread extends Thread {

	protected boolean running;
	protected String messageType, version, senderID, fileID;
	protected int chunkNo, replicationDeg;
	protected byte[] buffer, chunkData;
	
	public ChannelThread(byte[] buffer) throws IOException {
		this.running = true;
		this.buffer = buffer;
		
		String[] received = Utils.decodeMessage(buffer);
		
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
		
		if(messageType.equals("CHUNK")){
			this.chunkData = received[5].getBytes();
		} else if (messageType.equals("PUTCHUNK")) {
			if(!(received[6]==null)){
				this.chunkData = received[6].getBytes();
			}
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
	
	public void putchunk(){
		
		/* Checks if file already exists */
		String filepath = "database/" + fileID;// + "/" + this.chunkNo;
		File folder = new File(filepath);
		if (!folder.exists()) {
			System.out.println("Creating folder to save new chunk...");
			folder.mkdirs();
			
			/* Check if has enough space */
			File file = new File(folder.getPath()+"/"+this.chunkNo);
			//if (Peer.maxBytes < (folder.length() + this.chunkData.length) || Peer.maxBytes != 0) {
			if(Peer.maxBytes < 64000){
				System.out.println("Can not save chunk. Not enough space.");
			} else {
				FileOutputStream chunk;
				try {
					/* Stores file */
					chunk = new FileOutputStream(file);
					chunk.write(this.chunkData);
					chunk.close();
				} catch (IOException e) {
					System.out.println("CHANNELTHREAD: Error saving to file.");
					e.printStackTrace();
				}
			}
		}// else System.out.println("Chunk already stored.");
		
		/* Waiting for an interruption for a random amount of time before sending response */
		Random rng = new Random();
		int r = rng.nextInt(401);
		try {
			TimeUnit.MILLISECONDS.sleep(r);
		} catch (InterruptedException e) {
			/*PUTCHUNK thread interrupted*/
			e.printStackTrace();
		}
		
		/* Send STORED message to mcSocket
		 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF> */
		byte[] msg = Utils.codeMessage("STORED", this.fileID, this.chunkNo, null);
     	DatagramPacket packet = new DatagramPacket(msg, msg.length, Peer.mdbSocket.getLocalAddress(), Peer.mdbSocket.getLocalPort());
		try {
			Peer.mcSocket.send(packet);
		} catch (IOException e) {
			System.out.println("Could not respond to PUTCHUNK message");
		}
		
	}
	
	public void getchunk(){
		/*TODO received getchunk thread
		 * 1. verificar se tem ficheiro
		 * 2. se tem envia, se nao, ignora
		 * 3. espera um tempo aleatorio entre 0 e 400 ms
		 * 4. se receber uma mensagem tipo CHUNK acaba a funcao sem enviar resposta
		 * 5. manda mensagem CHUNK para mdrSocket
		 * */
	}
	
	public void delete(){
		/*TODO received delete thread
		 * 1. apagar todos os chunks com fileID especificado
		 * */
		
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
		interrupt();
	}

	public boolean isRelated(String messageType, String fileID, String chunkNoString) {

		int chunkNo = Integer.parseInt(chunkNoString);
		if (running && this.messageType.equals(messageType) && this.fileID.equals(fileID) && this.chunkNo == chunkNo)
			return true;
		else
			return false;
	}
}
