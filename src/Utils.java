import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;

public interface Utils {

	static byte[] codeMessage(String msg_type, String given_fileID, int given_chunkNo, Chunk chunk) {
		byte[] msg = null;
		try {
			String msg_type2 = msg_type + " ";
			byte[] msg_type_b = msg_type2.getBytes("US-ASCII");

			String version = Peer.protocolV + " ";
			byte[] version_b = version.getBytes("US-ASCII");

			String sender_id = Peer.peerID + " ";
			byte[] sender_id_b = sender_id.getBytes("US-ASCII");

			byte[] file_id_b = Base64.getEncoder().encode(given_fileID.getBytes());
			String tmp = " ";
			byte[] tmp2 = tmp.getBytes("US-ASCII");
			//file_id_b.push(tmp2);
			//byte[] file_id_b = Base64.getEncoder().encode(file_id.getBytes());

			byte[] chunk_no_b = null;
			if (!msg_type.equals("DELETE")) {
				String chunk_no = given_chunkNo + " ";
				chunk_no_b = chunk_no.getBytes("US-ASCII");
			}

			byte[] replication_b = null;
			if (msg_type.equals("PUTCHUNK")) {
				String replication = ""+chunk.getReplication() + " ";
				replication_b = replication.getBytes("US-ASCII");
			}

			char CR = (char) 0x0D;
			char LF = (char) 0x0A;
			String crlf = "" + CR + LF; // "" forces conversion to string
			byte[] crlf_b = crlf.getBytes("US-ASCII");

			ByteBuffer bb = null;
			if (msg_type.equals("PUTCHUNK")) {
				/*
				 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
				 */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
					+ tmp2.length + chunk_no_b.length + replication_b.length + crlf_b.length + crlf_b.length + chunk.getSize());
			} else if (msg_type.equals("CHUNK")) {
				/*
				 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
				 */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ chunk_no_b.length + crlf_b.length + crlf_b.length + chunk.getSize());
			} else if (msg_type.equals("DELETE")) {
				/* DELETE <Version> <SenderId> <FileId> <CRLF><CRLF> */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ crlf_b.length + crlf_b.length + chunk.getSize());
			} else {
				/*
				 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				 * GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				 * REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				 */
				bb = ByteBuffer.allocate(msg_type_b.length + version_b.length + sender_id_b.length + file_id_b.length
						+ tmp2.length + chunk_no_b.length + crlf_b.length + crlf_b.length);
			}

			bb.put(msg_type_b);
			bb.put(version_b);
			bb.put(sender_id_b);
			bb.put(file_id_b);
			bb.put(tmp2);
			if (!msg_type.equals("DELETE"))
				bb.put(chunk_no_b);

			if (msg_type.equals("PUTCHUNK"))
				bb.put(replication_b);

			bb.put(crlf_b);
			bb.put(crlf_b);

			if (msg_type.equals("PUTCHUNK") || msg_type.equals("CHUNK")){
				if(!(chunk.getData()==null)){
					bb.put(chunk.getData());
				}else{
					bb.put(new byte[0]);
				}

			}

			msg = bb.array();

			System.out.println("UTILS - Putchunk bytes: " + msg);
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTILS - Server exception: " + e.toString());
			e.printStackTrace();
		}

		return msg;
	}

	static String[] decodeMessage(byte[] buf) {
		String msgReceived = null;

		try {
			msgReceived = new String(buf, "US-ASCII");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		char CR = (char) 0x0D;
		char LF = (char) 0x0A;
		String crlf = "" + CR + LF;
		crlf = crlf+crlf;
		int index = msgReceived.indexOf(crlf);
		//String header = 
		//String body =
		

		msgReceived = msgReceived.trim();
		
		String[] components = msgReceived.split(crlf);
		String[] header = components[0].split(" ");
		String body;

		
		
		try{
		//byte[] fid_b = header[3].getBytes("US-ASCII");
			byte[] fid = Base64.getDecoder().decode(header[3].getBytes());
	
			header[3] = new String(fid);
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}
		int size = header.length;
		String[] msgDecoded = new String[size+1];
		for(int i=0; i<size;i++){
			msgDecoded[i]=header[i];
		}
		if(components.length>1){
			body = components[1];
			msgDecoded[size]=body;
		}
		

		System.out.println("UTILS - Message received: " + components[0]);
		System.out.println("UTILS - Message received: " + header[3]);
		System.out.println("UTILS - Message received parts: " + components.length);
		/*para o caso de haver um ou mais " "s no meio dos dados*/
		/*if(components[0].equals("PUTCHUNK") && components.length > 7){
			String temp = components[6];
			for(int i=7; i<components.length; i++){
				temp = temp + components[i] + " ";
			}
			components[6] = temp;
		} else if(components[0].equals("CHUNK") && components.length > 6){
			String temp = components[5];
			for(int i=6; i<components.length; i++){
				temp = temp + components[i] + " ";
			}
			components[5] = temp;
		}*/

		return msgDecoded;
	}

	static byte[] stringToByte(String body) {

		try {
			char CR = (char) 0x0D;
			char LF = (char) 0x0A;
			String crlf = "" + CR + LF;
			crlf = crlf + crlf;

			if (body.startsWith(crlf)) {
				String chunkData = body.substring(crlf.length());
				byte[] chunk = chunkData.getBytes("US-ASCII");

				return chunk;
			} else {
				System.out.println("UTILS - Message was badly split");
				return null;
			}

		} catch (UnsupportedEncodingException e) {
			System.err.println("UTILS - Server exception: " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
}
