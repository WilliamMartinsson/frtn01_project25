package regulatorsocket;

import java.net.InetAddress;

public abstract class SocketMonitor {

	protected double sendData1;
	protected double sendData2;
	protected double receiveData1;
	protected double receiveData2;
	protected InetAddress address;
	protected int port;
	protected long ping;
	protected long lastPacket;
	protected long lastPingPacket;
	protected Packet returningPingPacket = null;

	public synchronized double getReceiveData1() {
		return receiveData1;
	}

	public synchronized double getReceiveData2() {
		return receiveData2;
	}

	public synchronized void setSendData1(double data1) {
		this.sendData1 = data1;
	}

	public synchronized void setSendData2(double data2) {
		this.sendData2 = data2;
	}

	public synchronized Packet getPacket() {
		if (address == null || port < 0)
			return null;
		long time = System.currentTimeMillis();
		// Util.print("Sending  (Monitor): " + time + " " + transferData);
		return new Packet(address, port, time, sendData1, sendData2);
	}

	public synchronized Packet getPingPacket() {
		if (address == null || port < 0)
			return null;
		long time = System.currentTimeMillis();
		Util.print("Sending  (Monitor): " + time + " Ping");
		return new Packet(address, port, time, time);
	}

	public synchronized Packet getReturningPingPacket() {
		Packet packet = returningPingPacket;
		returningPingPacket = null;
		if (packet != null) {
			Util.print("Sending  (Monitor): " + System.currentTimeMillis()
					+ " Returning ping");
		}
		return packet;
	}

	public synchronized void setPacket(Packet packet) {
		long time = System.currentTimeMillis();
		if (packet.isPing()) {
			if (packet.getTimestamp() > lastPingPacket) {
				if (packet.isReturningPing()) {
					ping = time - packet.getPing();
					Util.print("Received (Monitor): " + time + " Ping [" + ping
							+ " ms]");

					// long time1 = System.currentTimeMillis();
					// WebMonitor web = new WebMonitor("localhost:3000");
					// web.send(2, 2, (int) this.ping);
					// Util.print("[WEB_DIFF]: " +
					// String.valueOf(System.currentTimeMillis() - time1) +
					// "ms");

				} else {
					packet.setReturningPing();
					returningPingPacket = packet;
					Util.print("Received (Monitor): " + time + " Ping packet ["
							+ packet.isReturningPing() + "]");
				}
			} else {
				Util.print("Received (Monitor): " + time
						+ " discarded old ping packet");
			}

		} else {

			if (packet.getTimestamp() > lastPacket) {
				receiveData1 = packet.getData1();
				receiveData2 = packet.getData2();
				// Util.print("Received (Monitor): " + time + " " + localData);
			} else {
				Util.print("Received (Monitor): " + time
						+ " discarded old data");
			}

		}
	}

	public synchronized InetAddress getAddress() {
		return address;
	}

	public synchronized long getPing() {
		return ping;
	}
}
