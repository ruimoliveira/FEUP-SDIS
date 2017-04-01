import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;

public interface Utils {

	static byte[] codeMessage(String msg_type, String given_fileID, Chunk chunk){
		byte[] msg = null;
		try{
			byte[] msg_type_b = msg_type.getBytes("US-ASCII");

			String version = Peer.protocolV + " ";
			byte[] version_b = version.getBytes("US-ASCII");

			String sender_id = Peer.peerID + " ";
			byte[] sender_id_b = sender_id.getBytes("US-ASCII");

			String file_id = given_fileID + " ";
			byte[] file_id_b = Base64.getEncoder().encode(file_id.getBytes());

			byte[] chunk_no_b = null;
			if (!msg_type.equals("DELETE")) {
				String chunk_no = chunk.getChunkNo() + " ";
				chunk_no_b = chunk_no.getBytes("US-ASCII");
			}

			byte[] replication_b = null;
			if (msg_type.equals("PUTCHUNK")) {
				String replication = chunk.getReplication() + " ";
				replication_b = replication.getBytes("US-ASCII");
			}

			char CR  = (char) 0x0D;
			char LF  = (char) 0x0A;
			String crlf  = "" + CR + LF;     // "" forces conversion to string
			byte[] crlf_b = crlf.getBytes("US-ASCII");

			ByteBuffer bb = null;
			if (msg_type.equals("PUTCHUNK")) {
				/* PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body> */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ chunk_no_b.length + replication_b.length + crlf_b.length + crlf_b.length + chunk.getSize());
			} else if (msg_type.equals("CHUNK")) {
				/* CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body> */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ chunk_no_b.length + crlf_b.length + crlf_b.length + chunk.getSize());
			} else if (msg_type.equals("DELETE")) {
				/* DELETE <Version> <SenderId> <FileId> <CRLF><CRLF> */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ crlf_b.length + crlf_b.length + chunk.getSize());
			} else {
				/*
				STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				*/
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ crlf_b.length + crlf_b.length);
			}
			
			bb.put(msg_type_b);
			bb.put(version_b);
			bb.put(sender_id_b);
			bb.put(file_id_b);
			
			if (!msg_type.equals("DELETE"))
				bb.put(chunk_no_b);
			
			if (msg_type.equals("PUTCHUNK"))
				bb.put(replication_b);
			
			bb.put(crlf_b);
			bb.put(crlf_b);

			if (msg_type.equals("PUTCHUNK") || msg_type.equals("CHUNK"))
				bb.put(chunk.getData());
			
			msg = bb.array();

			System.out.println("Putchunk bytes: "+ msg);
		}catch(UnsupportedEncodingException e){
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
		
		return msg;
	}
	
	static String[] decodeMessage(byte[] buf){
		String msgReceived = null;
		
		try {
			msgReceived = new String(buf, "US-ASCII");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		msgReceived = msgReceived.trim();
		System.out.println("Message received: " + msgReceived);
		String[] components = msgReceived.split(" ");
		
		return components;
	}
	
}
