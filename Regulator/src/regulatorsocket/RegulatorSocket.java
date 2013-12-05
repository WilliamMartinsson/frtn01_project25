package regulatorsocket;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;

public class RegulatorSocket {

	private int sendPeriod = 150;
	private DatagramSocket datagramSocket;
	private SocketMonitor monitor;

	public RegulatorSocket(int port, String host) throws IOException {
		monitor = new Client(InetAddress.getByName(host), port);
		Util.print("Starting client");
		Util.print("Connecting to: " + host + ":" + port);

		datagramSocket = new DatagramSocket();
	}

	public RegulatorSocket(int port) throws IOException {
		monitor = new Server();
		Util.print("Starting server");
		Util.print("Open for connections to port " + port);

		datagramSocket = new DatagramSocket(port);
	}

	// DUBUG
	public void push() {
		while (true) {
			monitor.setData(Math.sin(System.currentTimeMillis()));
		}
	}

	public void open() {
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

	/*
	 * Server mode java RegulatorSocket -server 12345
	 *
	 * Client mode java RegulatorSocket -client 12345 olivetti.control.lth.se
	 */
	public static void main(String[] args) {
		// Map containing all the parameters sent to the program at startup.
		HashMap<String, LinkedList<String>> argMap = new HashMap<String, LinkedList<String>>();

		// Moving all parameters from args[] to argMap
		String currentArg = "general";
		LinkedList<String> currentParameters = new LinkedList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				argMap.put(currentArg, currentParameters);
				currentParameters = new LinkedList<String>();
				currentArg = arg;
			} else {
				currentParameters.add(arg);
			}
		}
		argMap.put(currentArg, currentParameters);

		// Changing the workflow based on the choice parameters
		if (argMap.containsKey("-server") && argMap.containsKey("-client")) {
			throw new IllegalArgumentException(
					"Can't set both the parameters server and client at same time.");
		} else if (!argMap.containsKey("-server")
				&& !argMap.containsKey("-client")) {
			throw new IllegalArgumentException(
					"Need to set at least one of the parameters server or client.");
		} else if (argMap.containsKey("-server")) {
			LinkedList<String> param = argMap.get("-server");
			if (param != null && param.size() == 1) {
				try {
					int port = Integer.parseInt(param.getFirst());
					try {
						RegulatorSocket server = new RegulatorSocket(port);
						server.open();
					} catch (Exception e) {
						System.out.println("Exception caught when trying to setup regulator server on port: " + port);
						e.printStackTrace();
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The server port ("
							+ param.getFirst() + ") has to be numerical.");
				}
			} else {
				throw new IllegalArgumentException(
						"The parameter server takes one argument, which is the server port.");
			}
		} else {
			LinkedList<String> param = argMap.get("-client");
			if (param != null && param.size() == 2) {
				try {
					int port = Integer.parseInt(param.getFirst());
					try {
						RegulatorSocket client = new RegulatorSocket(port,
								param.getLast());
						client.open();
						client.push();
					} catch (Exception e) {
                        System.out.println("Exception caught when trying to setup regulator client connection to server [" + param.getLast() + ":" + port + "]");
						e.printStackTrace();
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The server port ("
							+ param.getFirst() + ") has to be numerical.");
				}
			} else {
				throw new IllegalArgumentException(
						"The parameter server takes one argument, which is the server port.");
			}
		}
	}

	private class Sender extends Thread {

		public void run() {
			try {
				long i = 0;
				while (!Thread.interrupted()) {
					if (i++ % 10 == 0) {
						Packet packet = monitor.getPingPacket();
						if (packet != null) {
							packet.send(datagramSocket);
						}
					}
					Packet returningPingPacket = monitor.getReturningPingPacket();
					if (returningPingPacket != null) {
						returningPingPacket.send(datagramSocket);
					}
					// Send packet

					Packet packet = monitor.getPacket();
					if (packet != null) {
						packet.send(datagramSocket);
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
					packet = Packet.recieve(datagramSocket);
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
			this.value = 0;
			this.localData = 0;
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

		public synchronized Packet getPacket() {
			value = localData;
			return super.getPacket();
		}
	}

	private class Client extends SocketMonitor {

		public Client(InetAddress address, int port) {
			this.value = 0;
			this.localData = 0;
			this.address = address;
			this.port = port;
			this.ping = 0;
			this.lastPacket = 0;
		}
	}

}
