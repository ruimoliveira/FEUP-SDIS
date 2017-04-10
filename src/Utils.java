import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Formatter;
import java.util.stream.IntStream;
import java.io.IOException;

public interface Utils {

	public static byte[] codeMessage(String msg_type, String given_fileID, int given_chunkNo, Chunk chunk) {

		String protocol = msg_type;
		String version = Peer.protocolV;
		String senderID = Peer.peerID;
		String chunkNo = null;

		String repDegree = null;
		char white = ' ';
		char CR = (char) 0x0D;
		char LF = (char) 0x0A;
		String crlf = "" + CR + LF;
		byte[] result = null;

		// String fileID64 = null;

		// final IntStream protocol_is = protocol.chars();
		// final IntStream version_is = version.chars();
		// final IntStream senderID_is = senderID.chars();
		// final IntStream chunkNo_is = null;
		// final IntStream repDegree_is = null;
		if (!msg_type.equals("DELETE")) {
			chunkNo = "" + given_chunkNo;
			// final IntStream chunkNo_is = chunkNo.chars();

		}
		if (msg_type.equals("PUTCHUNK"))
			repDegree = "" + chunk.getReplication();

		/*
		 * if (msg_type.equals("PUTCHUNK")) { repDegree =
		 * ""+chunk.getReplication(); //final IntStream repDegree_is =
		 * repDegree.chars(); fileID64 =
		 * bytesToHexString(given_fileID.getBytes()); //fileID64 = given_fileID;
		 * }else{ fileID64 = given_fileID; }
		 */

		// char[] fileID_is = fileID64.chars();

		ByteBuffer bb = null;
		if (msg_type.equals("PUTCHUNK")) {
			/*
			 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg>
			 * <CRLF><CRLF><Body>
			 */
			long size = protocol.length() + version.length() + senderID.length() + given_fileID.length()
					+ chunkNo.length() + repDegree.length() + 6 + crlf.getBytes().length + crlf.getBytes().length
					+ chunk.getSize();
			bb = ByteBuffer.allocate((int) size);
		} else if (msg_type.equals("CHUNK")) {
			/*
			 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
			 */
			long size = protocol.length() + version.length() + senderID.length() + given_fileID.length()
					+ chunkNo.length() + 6 + 4 + chunk.getSize();
			bb = ByteBuffer.allocate((int) size);
		} else if (msg_type.equals("DELETE")) {
			/* DELETE <Version> <SenderId> <FileId> <CRLF><CRLF> */
			long size = protocol.length() + version.length() + senderID.length() + given_fileID.length() + 6 + 4;
			bb = ByteBuffer.allocate((int) size);
		} else {
			/*
			 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
			 * GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
			 * REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
			 */
			long size = protocol.length() + version.length() + senderID.length() + given_fileID.length()
					+ chunkNo.length() + 6 + 4;
			bb = ByteBuffer.allocate((int) size);
		}
		for (int i = 0; i < protocol.length(); i++) {
			char c = protocol.charAt(i);
			bb.put((byte) c);
		}
		// protocol_is.forEach( c -> bb.put((byte)c));
		bb.put((byte) white);
		for (int i = 0; i < version.length(); i++) {
			char c = version.charAt(i);
			bb.put((byte) c);
		}
		// version_is.forEach( c -> bb.put((byte)c));
		bb.put((byte) white);
		for (int i = 0; i < senderID.length(); i++) {
			char c = senderID.charAt(i);
			bb.put((byte) c);
		}
		// senderID_is.forEach( c -> bb.put((byte)c));
		bb.put((byte) white);
		for (int i = 0; i < given_fileID.length(); i++) {
			char c = given_fileID.charAt(i);
			bb.put((byte) c);
		}
		// fileID_is.forEach( c -> bb.put((byte)c));
		bb.put((byte) white);
		if (!msg_type.equals("DELETE")) {
			chunkNo = "" + given_chunkNo;
			for (int i = 0; i < chunkNo.length(); i++) {
				char c = chunkNo.charAt(i);
				bb.put((byte) c);
			}
			// final IntStream chunkNo_is = chunkNo.chars();
			// chunkNo_is.forEach( c -> bb.put((byte)c));
			bb.put((byte) white);
		}
		if (msg_type.equals("PUTCHUNK")) {
			repDegree = "" + chunk.getReplication();
			for (int i = 0; i < repDegree.length(); i++) {
				char c = repDegree.charAt(i);
				bb.put((byte) c);
			}
			// final IntStream repDegree_is = repDegree.chars();
			// repDegree_is.forEach( c -> bb.put((byte)c));
			bb.put((byte) white);
		}
		/*
		 * bb.put((byte)CR); bb.put((byte)LF); bb.put((byte)CR);
		 * bb.put((byte)LF);
		 */
		bb.put(crlf.getBytes());
		bb.put(crlf.getBytes());

		if (msg_type.equals("PUTCHUNK") || msg_type.equals("CHUNK")) {
			if (!(chunk.getData() == null)) {
				bb.put(chunk.getData());
			} else {
				bb.put(new byte[0]);
			}
		}

		result = bb.array();

		return result;

	}

	static String[] getHeader(byte[] msg) {
		int header_end = 0;
		char CR = (char) 0x0D;
		char LF = (char) 0x0A;

		for (int i = 0; i < msg.length; i++) {
			if ((msg[i] == (byte) CR) && (msg[i + 1] == (byte) LF)) {
				header_end = i;
				break;
			}
		}
		byte[] h = new byte[header_end + 1];
		for (int i = 0; i < header_end; i++) {
			h[i] = msg[i];
		}

		String header = new String(h);
		String[] header_parts = header.split(" ");

		return header_parts;
	}

	static byte[] getBody(byte[] msg, int size) {

		int body_start = 0;
		char CR = (char) 0x0D;
		char LF = (char) 0x0A;
		for (int i = 0; i < size; i++) {
			if ((msg[i] == (byte) CR) && (msg[i + 1] == (byte) LF)) {
				body_start = i + 4;
				break;
			}
		}

		byte[] body = null;
		if (!(body_start == size)) {

			body = new byte[size - body_start];

			for (int j = body_start; j < size; j++) {

				body[j - body_start] = msg[j];

			}

		}

		return body;
	}

	public static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);

		Formatter formatter = new Formatter(sb);
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		formatter.close();
		return sb.toString();
	}

	/*
	 * static byte[] stringToByte(String body) {
	 * 
	 * try { char CR = (char) 0x0D; char LF = (char) 0x0A; String crlf = "" + CR
	 * + LF; crlf = crlf + crlf;
	 * 
	 * if (body.startsWith(crlf)) { String chunkData =
	 * body.substring(crlf.length()); byte[] chunk =
	 * chunkData.getBytes("US-ASCII");
	 * 
	 * return chunk; } else {
	 * System.out.println("UTILS - Message was badly split"); return null; }
	 * 
	 * } catch (UnsupportedEncodingException e) {
	 * System.err.println("UTILS - Server exception: " + e.toString());
	 * e.printStackTrace(); } return null; }
	 */
}
