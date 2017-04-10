import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Channel implements Runnable {

	boolean running;

	MulticastSocket socket;
	Channel(MulticastSocket socket){
		this.socket = socket;
		this.running = true;
	}

	public void run(){
		while(running){
			
			/*receive message*/
			byte[] buf = new byte[64256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			
			try {
				socket.setSoTimeout(400);
				socket.receive(packet);
			} catch (SocketTimeoutException ste) {
				/* Do nothing pls */
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/*decode message*/
			String[] message = Utils.getHeader(buf);
			/*handle message*/
			if(message != null && message.length > 4 && message[0] != null && message[0].length() != 0){
				if(!message[2].equals(Peer.peerID)){
					System.out.println("CHANNEL: protocol received: " + message[0]);
					try {
						(new Thread(new ChannelThread(packet))).start();
					} catch (IOException e) {
						System.out.println("CHANNEL: Error starting thread");
						e.printStackTrace();
					}
				}
			}
		}
	}	
}
