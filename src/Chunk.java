public class Chunk {
	String fileID;
	int chunkNo;
	int replication;
	int size;
	byte[] data;

	public Chunk(String fileID, int chunkNo, int replication, int size, byte[] data) {
		this.fileID = fileID;
		this.chunkNo = chunkNo;
		this.replication = replication;
		this.size = size;
		this.data = data;
	}

	public String getFileID() {
		return this.fileID;
	}

	public int getChunkNo() {
		return this.chunkNo;
	}

	public int getSize() {
		return this.size;
	}

	public byte[] getData() {
		return this.data;
	}

	public int getReplication() {
		return this.replication;
	}
}
