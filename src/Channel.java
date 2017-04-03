import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class Channel {

	protected ArrayList<ChannelThread> interruptableThreads = new ArrayList<ChannelThread>();

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
			String[] message = Utils.decodeMessage(buf);
			
			/*handle message*/
			if(message != null){
				try {
					ChannelThread thread;
					switch(message[0]){
					case "DELETE":
						System.out.println("CHANNEL: Received DELETE message");
						new ChannelThread(buf).start();
						break;
					case "GETCHUNK":
						System.out.println("CHANNEL: Received GETCHUNK message");
						thread =  new ChannelThread(buf);
						interruptableThreads.add(thread);
						thread.start();
						break;
					case "REMOVED":
						System.out.println("CHANNEL: Received REMOVED message");
						thread =  new ChannelThread(buf);
						interruptableThreads.add(thread);
						thread.start();
						break;
					case "PUTCHUNK":
						System.out.println("CHANNEL: Received CHUNK message");

						/*find if there is a REMOVED thread that needs interruption & interrupt it*/
						for (int i=0; i<this.interruptableThreads.size(); i++){
							if(this.interruptableThreads.get(i).isRelated("REMOVED", message[2], message[3])){
								this.interruptableThreads.get(i).stopThread();
								this.interruptableThreads.remove(i);
								break;
							}
						}
						
						/*start PUTCHUNK thread*/
						new ChannelThread(buf).start();
						break;
					case "CHUNK":
						System.out.println("CHANNEL: Received CHUNK message");

						/*find if there is a GETCHUNK thread that needs interruption & interrupt it*/
						for (int i=0; i<this.interruptableThreads.size(); i++){
							if(this.interruptableThreads.get(i).isRelated("GETCHUNK", message[2], message[3])){
								this.interruptableThreads.get(i).stopThread();
								this.interruptableThreads.remove(i);
								break;
							}
						}
						
						/*TODO: tambem precisamos de interromper os CHUNK threads*/
						
						/*start CHUNK thread*/
						new ChannelThread(buf).start();
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
