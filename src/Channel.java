import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Channel implements Runnable {

	boolean running;

	protected ArrayList<ChannelThread> interruptableThreads = new ArrayList<ChannelThread>();
	MulticastSocket socket;
	Channel(MulticastSocket socket){
		this.socket = socket;
		this.running = true;
	}

	public void run(){
		while(running){
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
				socket.setSoTimeout(400);
				socket.receive(packet);
			} catch (SocketTimeoutException ste) {
				/* Do nothing pls */
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/*decode message*/
			String[] message = Utils.getHeader(buf);
			//System.out.println(message.length);
			/*handle message*/
			if(message != null && message.length > 4 && message[0] != null && message[0].length() != 0){
				if(!message[2].equals(Peer.peerID)){
					System.out.println("CHANNEL: protocol received: "+message[0]);
					try {
						Thread thread;
						ChannelThread channelThread;
						switch(message[0]){
						case "DELETE":
							System.out.println("CHANNEL: Received DELETE message");
							(new Thread(new ChannelThread(buf))).start();
							break;
						case "GETCHUNK":
							System.out.println("CHANNEL: Received GETCHUNK message");
							channelThread = new ChannelThread(buf);
							new Thread(channelThread).start();
							interruptableThreads.add(channelThread);
							break;
						case "REMOVED":
							System.out.println("CHANNEL: Received REMOVED message");
							channelThread = new ChannelThread(buf);
							new Thread(channelThread).start();
							interruptableThreads.add(channelThread);
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
							(new Thread(new ChannelThread(buf))).start();
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
							(new Thread(new ChannelThread(buf))).start();
							break;
						default:
							System.out.println("CHANNEL: Unrecognized message received");
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	public void setRunning(boolean s){
		socket.close();
		this.running=s;
	}
	
}
