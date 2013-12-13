package regulatorsocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPSocket implements ConnectionSocket {

	DatagramSocket datagramSocket;

	public UDPSocket() throws IOException {
		datagramSocket = new DatagramSocket();
	}

	public UDPSocket(int port) throws IOException {
		datagramSocket = new DatagramSocket(port);
	}

	public void send(Packet packet) throws IOException {
		datagramSocket.send(new DatagramPacket(packet.getPacket(), packet
				.getPacket().length, packet.getAddress(), packet.getPort()));
	}

	public Packet receive(int size) throws IOException {
		DatagramPacket dp = new DatagramPacket(new byte[size], size);
		datagramSocket.receive(dp);
		return Packet.createPacket(dp.getData(), dp.getAddress(), dp.getPort());
	}

	public void setReady() throws IOException {
	}

}
