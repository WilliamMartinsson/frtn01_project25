package regulatorsocket;

import java.io.IOException;
import java.net.InetAddress;

public interface ConnectionSocket {
	
	public void send(Packet packet) throws IOException;
	public Packet receive(int size) throws IOException;
	public void setReady() throws IOException;

}
