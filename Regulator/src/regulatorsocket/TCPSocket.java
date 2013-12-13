package regulatorsocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class TCPSocket implements ConnectionSocket {

	Socket socket;
	ServerSocket serverSocket;
	InputStream in;
	OutputStream out;
	String host;
	int port;

	public TCPSocket(String host, int port) throws IOException {
		System.out.println("CLIENT HOST/PORT: " + host + "/" + port);
		socket = new Socket(host, port);
//		NetworkInterface nif = NetworkInterface.getByName("wwan0");
//		Enumeration<InetAddress> nifAddresses = nif.getInetAddresses();
//		socket.bind(new InetSocketAddress(nifAddresses.nextElement(), 0));
		
	}

	public TCPSocket(int port) throws IOException {
		System.out.println("SERVER PORT: " + port);
		serverSocket = new ServerSocket(port);
	}

	public void send(Packet packet) throws IOException {
		out.write(packet.getPacket());
	}

	public Packet receive(int size) throws IOException {
		byte[] data = new byte[size];
		in.read(data);
		return Packet.createPacket(data, null, 0);
	}

	public void setReady() throws IOException {
		if (serverSocket != null) {
			socket = serverSocket.accept();
		}else{
			socket.setTcpNoDelay(true);
		}
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}

}
