import java.io.*;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.attribute.*;
import java.nio.file.Files;
import java.security.*;
//import java.nio.file.attribute;

public class MyFile {
	String fileID;
	String ownerID;
	int replication;
	int size;
	byte[] content;
	String path;
	ArrayList<Chunk> chunks = new ArrayList<Chunk>();

	public MyFile(String fileID, String ownerID, int replication, int size, byte[] content) {
		this.fileID = fileID;
		this.ownerID = ownerID;
		this.replication = replication;
		this.size = size;
		this.content = content;
		this.splitter();
	}

	public String getFileID() {
		return this.fileID;
	}

	public String getownerID() {
		return this.ownerID;
	}

	public int getSize() {
		return this.size;
	}

	public byte[] getContent() {
		return this.content;
	}

	public int getReplication() {
		return this.replication;
	}

	public void splitter() {
		int max_size = 64000;
		int counter = 0;
		int num_of_chunks = this.size / 64000;
		int remainder = this.size % 64000;
		if (remainder != 0) {
			num_of_chunks++;
		}

		System.out.println("TESTE: " + num_of_chunks);

		Chunk c = null;
		byte[] chunk = new byte[max_size];
		int chunkNo = 1;

		if (num_of_chunks == 1) {
			chunk = new byte[this.size];
			for (int i = 0; i < this.size; i++) {
				chunk[counter] = this.content[i];
				counter++;
			}
			c = new Chunk(this.fileID, chunkNo, this.replication, chunk.length, chunk);
			this.chunks.add(c);

		} else {

			// int bytes_left = this.size;

			for (int i = 0; i < this.size; i++) {
				if (chunkNo == num_of_chunks) {
					// chunk = new byte[this.size-i];
					// bytes_left = this.size - i;
					chunk[counter] = this.content[i];
					counter++;

				} else {

					if (counter < max_size) {
						chunk[counter] = this.content[i];
						counter++;
					} else {

						c = new Chunk(this.fileID, chunkNo, this.replication, chunk.length, chunk);
						this.chunks.add(c);
						counter = 0;
						chunk[counter] = this.content[i];
						counter++;
						chunkNo++;
						if (chunkNo == num_of_chunks) {
							chunk = new byte[this.size - i];
						}

					}
				}
			}
			c = new Chunk(this.fileID, chunkNo, this.replication, chunk.length, chunk);
			this.chunks.add(c);
		}
		if (remainder == 0) {
			chunkNo++;
			c = new Chunk(this.fileID, chunkNo, this.replication, 0, null);
			this.chunks.add(c);
		}

	}

	public static String makeFileID(File f) {
		String fileID = "";

		try {
			FileTime ft = Files.getLastModifiedTime(f.toPath());
			UserPrincipal up = Files.getOwner(f.toPath());
			String owner = up.getName();
			String file_name = f.toString();
			long modified = ft.toMillis();
			System.out.println("File Name: " + file_name);
			System.out.println("Owner: " + owner);
			System.out.println("Last Modified: " + modified);

			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				String metadata = file_name + Long.toString(modified) + owner;

				md.update(metadata.getBytes("UTF-8"));
				byte[] digest = md.digest();
				fileID = new String(digest);
			} catch (NoSuchAlgorithmException e) {
				System.err.println("Server exception: " + e.toString());
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

		return fileID;
	}

}
