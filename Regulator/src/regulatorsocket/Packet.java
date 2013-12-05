package regulatorsocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Packet {

    private static final byte DATA_PACKET = 0;
    private static final byte OUTGOING_PING_PACKET = 1;
    private static final byte RETURNING_PING_PACKET = 2;

    private static final byte PACKET_IDENTIFIER = 0;
    private static final byte TIMESTAMP = 1;
    private static final byte DATA1 = 9;
    private static final byte DATA2 = 17;
    private static final byte PING_DATA = 9;
    private static final byte PADDING = 17;

    private static final int PACKET_SIZE = 25;

	private InetAddress address;
	private int port;
	private boolean ping;
	private long timestamp;
	private long pingtime;
	private double data1;
	private double data2;
	private boolean returningping = false;

	public Packet(InetAddress address, int port, long timestamp, double data1, double data2) {
		this.address = address;
		this.port = port;
		this.ping = false;
		this.timestamp = timestamp;
		this.data1 = data1;
		this.data2 = data2;
	}

	public Packet(InetAddress address, int port, long timestamp, long pingtime) {
		this.address = address;
		this.port = port;
		this.ping = true;
		this.timestamp = timestamp;
		this.pingtime = pingtime;
	}

	public boolean isPing() {
		return ping;
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public double getData1() {
		return data1;
	}

	public double getData2() {
		return data2;
	}

	public long getPing() {
		return pingtime;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setReturningPing() {
		returningping = true;
	}

	public boolean isReturningPing() {
		return returningping;
	}

	public DatagramPacket toDatagramPacket() {
		ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
		if (this.ping) {
			if (this.returningping) {
				buffer.put(RETURNING_PING_PACKET);
			} else {
				buffer.put(OUTGOING_PING_PACKET);
			}
			buffer.putLong(TIMESTAMP, this.timestamp);
			buffer.putLong(PING_DATA, this.pingtime);
			buffer.putLong(PADDING,   this.pingtime);
		} else {
			buffer.put(DATA_PACKET);
			buffer.putLong(TIMESTAMP, this.timestamp);
			buffer.putDouble(DATA1,   this.data1);
			buffer.putDouble(DATA2,   this.data2);
		}
        byte[] sendBuffer = buffer.array();
		return new DatagramPacket(sendBuffer, sendBuffer.length, this.address, this.port);
	}

	public static Packet fromDatagramPacket(DatagramPacket dp) {
		ByteBuffer buffer = ByteBuffer.wrap(dp.getData());
		if (buffer.get(PACKET_IDENTIFIER) == DATA_PACKET) {
			return new Packet(dp.getAddress(), dp.getPort(), buffer.getLong(TIMESTAMP), buffer.getDouble(DATA1), buffer.getDouble(DATA2));
		} else {
			Packet pingPacket = new Packet(dp.getAddress(), dp.getPort(), buffer.getLong(TIMESTAMP), buffer.getLong(PING_DATA));
			if (buffer.get(PACKET_IDENTIFIER) == RETURNING_PING_PACKET) {
				pingPacket.setReturningPing();
			}
			return pingPacket;
		}
	}

	public void send(DatagramSocket ds) throws IOException {

		ds.send(this.toDatagramPacket());
	}

	public static Packet recieve(DatagramSocket ds) throws IOException {
		long s1 = 0;
		long t1 = 0;
		long s2 = 0;
		long t2 = 0;
		long s3 = 0;
		long t3 = 0;
		
		s1 = System.currentTimeMillis();
		byte[] dataArray = new byte[PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(dataArray, dataArray.length);
		t1 = System.currentTimeMillis();
		s2 = System.currentTimeMillis();
		ds.receive(packet);
		t2 = System.currentTimeMillis();
		s3 = System.currentTimeMillis();
		Packet pkt = Packet.fromDatagramPacket(packet);
		t3 = System.currentTimeMillis();
		System.out.println("[Returning ping packet time]: ("
				+ (t1 - s1) + ", " + (t2 - s2) + ", " + (t3 - s3) + ")");
		return pkt;
	}

}
