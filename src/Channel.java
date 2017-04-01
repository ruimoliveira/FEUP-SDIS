import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class Channel {

	protected static ArrayList<ChannelThread> interruptableThreads = new ArrayList<ChannelThread>();

	public Channel(MulticastSocket socket) {
		while(true){
			/*remove completed threads*/
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for(int i=0; i<interruptableThreads.size(); i++){
				if(!interruptableThreads.get(i).running){
					indexes.add(i);
				}
			}
			for(int i=0; i<indexes.size(); i++){
				interruptableThreads.remove(indexes.get(i));
			}
			
			/*receive message*/
			byte[] buf = new byte[64256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/*decode message*/
			String messageType = Utils.decodeMessage(buf)[0];
			
			/*handle message*/
			if(messageType != null){
				try {
					switch(messageType){
					case "DELETE":
						System.out.println("CHANNEL: Received DELETE message");
						new ChannelThread(buf).start();
						break;
					case "GETCHUNK":
						System.out.println("CHANNEL: Received GETCHUNK message");
						ChannelThread thread =  new ChannelThread(buf);
						interruptableThreads.add(thread);
						thread.start();
						break;
					case "REMOVED":
						System.out.println("CHANNEL: Received REMOVED message");
						ChannelThread thread2 =  new ChannelThread(buf);
						interruptableThreads.add(thread2);
						thread2.start();
						break;
					case "PUTCHUNK":
						System.out.println("CHANNEL: Received CHUNK message");
						/*TODO: find if there is a REMOVED thread that needs interruption & interrupt it*/
						new ChannelThread(buf).start();
						break;
					case "CHUNK":
						System.out.println("CHANNEL: Received CHUNK message");
						/*TODO: handle chunk
						 * 1. check if there is a thread that should be interrupted
						 * 2. if so, interrupt & ignore
						 * 3. else, execute a thread
						 * */
						break;
					default:
						System.out.println("CHANNEL: Unrecognized message received safdasfa");
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
