package main;

import pi2avr.TwoWaySerialComm;
import regulatorsocket.RegulatorSocket;
import regulatorsocket.SocketMonitor;
import util.IOMonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Client extends Thread {

	private SocketMonitor socketMonitor = null;
	private IOMonitor angle = null;
	private IOMonitor pos = null;
	private IOMonitor y = null;

	private RegulatorSocket rs = null;
	private TwoWaySerialComm comm = null;

	public Client() {
		angle = IOMonitor.getIO(IOMonitor.ANGLE);
		pos = IOMonitor.getIO(IOMonitor.POSITION);
		y = IOMonitor.getIO(IOMonitor.Y);

		try {
			
			rs = new RegulatorSocket(Main.REGULATOR_PORT, Main.REGULATOR_HOST);
			socketMonitor = rs.getMonitor();

		} catch (IOException e) {
			e.printStackTrace();
		}

		comm = new TwoWaySerialComm(new String[] {}, angle, pos, y,
				"/dev/ttyUSB0", 57600, 1024);
	}

	public void start() {
		rs.open();
		comm.start();
		super.start();
	}

	public void run() {
		try {
			int i = 0;
			while (!Thread.interrupted()) {
				if (i++ == Main.WEB_RATE) {
					System.out.println("At leat it does something! ("
							+ angle.getValue() + ", " + pos.getValue() + ", "
							+ socketMonitor.getReceiveData1() + ")");
					i = 0;
				}
				socketMonitor.setSendData1(angle.getValue());
				socketMonitor.setSendData2(pos.getValue());
				y.setValue(socketMonitor.getReceiveData1());
				Thread.sleep(Main.MAIN_PERIOD);
			}
		} catch (InterruptedException e) {
			System.out.println("Client was force closed!");
		}
	}

	public static void main(String[] args) {
		new Client();
	}

}
