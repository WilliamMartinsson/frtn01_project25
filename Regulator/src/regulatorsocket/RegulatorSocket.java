package regulatorsocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

public class RegulatorSocket {

	private int sendPeriod = 50;
	ConnectionSocket socket;
	// private DatagramSocket datagramSocket;
	private SocketMonitor monitor;

	public RegulatorSocket(int port, String host, boolean udp)
			throws IOException {
		monitor = new Client(InetAddress.getByName(host), port);
		Util.print("Starting client");
		Util.print("Connecting to: " + host + ":" + port);

		/*
		 * NetworkInterface nif = NetworkInterface.getByName("wwan0");
		 * Enumeration<InetAddress> nifAddresses = nif.getInetAddresses();
		 * datagramSocket = new DatagramSocket(new InetSocketAddress(
		 * nifAddresses.nextElement(), 23456));
		 */
		if (udp) {
			socket = new UDPSocket();
		} else {
			socket = new TCPSocket(host, port);
		}
		// datagramSocket = new DatagramSocket();
	}

	public RegulatorSocket(int port, boolean udp) throws IOException {
		monitor = new Server();
		Util.print("Starting server");
		Util.print("Open for connections to port " + port);

		if (udp) {
			socket = new UDPSocket(port);
		} else {
			socket = new TCPSocket(port);
		}
		// datagramSocket = new DatagramSocket(port);
	}

	// DUBUG
	public void push() {
		while (true) {
			monitor.setSendData1(Math.sin(System.currentTimeMillis()));
			monitor.setSendData2(Math.sin(System.currentTimeMillis()));
		}
	}

	public void open() throws IOException {
		socket.setReady();
		openSender();
		openReceiver();
	}

	public void openSender() {
		Sender s = new Sender();
		s.start();
	}

	public void openReceiver() {
		Receiver r = new Receiver();
		r.start();
	}

	public SocketMonitor getMonitor() {
		return monitor;
	}

	public void setSendPeriod(int period) {
		this.sendPeriod = period;
	}

	private class Sender extends Thread {

		public void run() {
			try {
				long i = 0;
				while (!Thread.interrupted()) {
					if (i++ % 10 == 0) {
						Packet packet = monitor.getPingPacket();
						if (packet != null) {
							socket.send(packet);
							// packet.send(datagramSocket);
						}
					}
					Packet returningPingPacket = monitor
							.getReturningPingPacket();
					if (returningPingPacket != null) {
						socket.send(returningPingPacket);
						// returningPingPacket.send(datagramSocket);
					}
					// Send packet

					Packet packet = monitor.getPacket();
					if (packet != null) {
						socket.send(packet);
						// packet.send(datagramSocket);
					}

					// Sleep
					Thread.sleep(sendPeriod);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Util.print("Thread stopped!");
		}
	}

	private class Receiver extends Thread {

		public void run() {
			Packet packet;
			try {
				while (!Thread.interrupted()) {
					packet = socket.receive(Packet.PACKET_SIZE);
					// packet = Packet.recieve(datagramSocket);
					monitor.setPacket(packet);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			Util.print("Thread stopped!");
		}
	}

	private class Server extends SocketMonitor {

		public Server() {
			this.sendData1 = 0;
			this.sendData2 = 0;
			this.receiveData1 = 0;
			this.receiveData2 = 0;
			this.address = null;
			this.port = -1;
			this.ping = 0;
			this.lastPacket = 0;
		}

		public synchronized void setPacket(Packet packet) {
			address = packet.getAddress();
			port = packet.getPort();
			super.setPacket(packet);
		}
	}

	private class Client extends SocketMonitor {

		public Client(InetAddress address, int port) {
			this.sendData1 = 0;
			this.sendData2 = 0;
			this.receiveData1 = 0;
			this.receiveData2 = 0;
			this.address = address;
			this.port = port;
			this.ping = 0;
			this.lastPacket = 0;
		}
	}

}
