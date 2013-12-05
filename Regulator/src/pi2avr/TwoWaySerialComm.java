package pi2avr;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import regulatorsocket.Util;
import util.IOMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

// Dependencies:
// $ sudo apt-get install librxtx-java

// Compile and run with:
// $ javac -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm.java
// $ java -Djava.library.path=/usr/lib/jni -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm
// or with optional main arguemnts
// $ java -Djava.library.path=/usr/lib/jni -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm "/dev/tty/USB0" baudRate bufferSize # (baudRate and bufferSize musts be ints)
public class TwoWaySerialComm {

	IOMonitor monitor;
	private String serialPort;
	private int baudRate;
	private int bufferSize;

	public TwoWaySerialComm(String[] args, IOMonitor angle, IOMonitor pos,
			IOMonitor signal, String serialPort, int baudRate, int bufferSize) {
		this.monitor = signal;
		// Defaults
		// String serialPort = "/dev/ttyUSB0";
		// int baudRate = 38400;
		// int baudRate = 57600;
		// int bufferSize = 1024;

		for (int i = 0; i < args.length; i++) {
			switch (i) {
			case 0:
				try {
					baudRate = Integer.parseInt(args[0]);
					System.out.println("[ARGUMENT] baud rate: " + baudRate);
				} catch (NumberFormatException e) {
					System.out
							.println("[ERROR] Baud rate must be an integer. Was: '"
									+ args[1] + "'");
					System.out.println("[SYSTEM EXIT]");
					System.exit(0);
				}
				break;
			case 1:
				try {
					bufferSize = Integer.parseInt(args[1]);
					System.out.println("[ARGUMENT] buffer size: " + bufferSize);
				} catch (NumberFormatException e) {
					System.out
							.println("[ERROR] Buffer size must be an integer. Was: '"
									+ args[2] + "'");
					System.out.println("[SYSTEM EXIT]");
					System.exit(0);
				}
				break;
			case 2:
				serialPort = args[2];
				System.out.println("[ARGUMENT] port: " + serialPort);
				break;

			}
		}

		System.out.println("[USING] port:        " + serialPort);
		System.out.println("[USING] baud rate:   " + baudRate);
		System.out.println("[USING] buffer size: " + bufferSize);

		System.setProperty("gnu.io.rxtx.SerialPorts", serialPort);
		this.serialPort = serialPort;
		this.baudRate = baudRate;
		this.bufferSize = bufferSize;
	}

	public void start() {
		try {
			System.out.println("\n\n[CONNECT]: serial=" + serialPort
					+ ", baudRate=" + baudRate + ", busignalfferSize="
					+ bufferSize);
			this.connect(serialPort, baudRate, bufferSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void connect(String portName, int baudRate, int bufferSize)
			throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("[ERROR] Port '" + portName
					+ "' is currently in use");
		} else {
			int timeout = 2000;
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					timeout);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				System.out.println("[SET] Serial port params");
				serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				System.out.println("[GET] Input stream");
				System.out.println("[GET] Output stream");
				InputStream in = serialPort.getInputStream();
				OutputStream out = serialPort.getOutputStream();

				System.out.println("[START] Serial reader");
				System.out.println("[START] Serial writer");
				(new Thread(new SerialReader(in, out, bufferSize, this)))
						.start();
				// (new Thread(new SerialWriter(out, monitor))).start();

			} else {
				System.out.println("[ERROR] Only serial ports are implemented");
			}
		}
	}

	public static class SerialReader implements Runnable {

		private static final byte SEND_DATA = (byte) 'S';

		private static final int IDENTIFIER = 0;
		private static final int CHANNEL = 1;
		private static final int DATA = 2;

		private static final byte READ_REQUEST = (byte) 'R';
		private static final byte WRITE_DATA = (byte) 'W';

		private static final byte CHANEL_0 = (byte) 0;
		private static final byte CHANEL_1 = (byte) 1;

		private static final byte PADDING_DATA = (byte) 0xff;

		private static final int PADDING = 2;
		private static final int LOW = 1;
		private static final int HIGH = 2;

		InputStream in;
		int bufferSize;
		TwoWaySerialComm comm;
		int channel;
		short data;
		OutputStream out;
		IOMonitor monitor;
		ByteBuffer sendBuffer;
		int count = 2;

		public SerialReader(InputStream in, OutputStream out, int bufferSize,
				TwoWaySerialComm comm) {
			this.in = in;
			this.out = out;
			this.bufferSize = bufferSize;
			this.comm = comm;
			this.sendBuffer = ByteBuffer.allocate(3);
		}

		public void run() {
			byte[] receiveBuffer = new byte[4];
			try {
				short tmp = 0;

				while (!Thread.interrupted()) {
					count = (count + 1) % 3;
					switch (count) {
					case (0):
						sendBuffer.put(IDENTIFIER, READ_REQUEST);
						sendBuffer.put(CHANNEL, CHANEL_0);
						sendBuffer.put(PADDING, PADDING_DATA);
						break;
					case (1):
						sendBuffer.put(IDENTIFIER, READ_REQUEST);
						sendBuffer.put(CHANNEL, CHANEL_1);
						sendBuffer.put(PADDING, PADDING_DATA);
						break;
					case (2):
						sendBuffer.put(IDENTIFIER, WRITE_DATA);
						sendBuffer.put(LOW, IOMonitor.getIO(2).getByteLow());
						sendBuffer.put(HIGH, IOMonitor.getIO(2).getByteHigh());
						
						break;
					}
					out.write(sendBuffer.array());
					Thread.sleep(5);
					in.read(receiveBuffer);
					

					ByteBuffer bb = ByteBuffer.wrap(receiveBuffer);

					
					if (bb.get(IDENTIFIER) == SEND_DATA) {
						channel = bb.get(CHANNEL);
						data = bb.getShort(DATA);

//						System.out.println("[RECEIVE][" + channel + "]: "
//								+ data);
						IOMonitor.getIO(channel).setValue(data);
					}else{
					//	System.out.println("HIGH:   " + IOMonitor.getIO(2).getByteHigh());
						//System.out.println("LOW:    " + IOMonitor.getIO(2).getByteLow());
					//	System.out.println("Double: " + IOMonitor.getIO(2).getValue());
					}

					/*
					if (count == 1) {
						WebMonitor wm = new WebMonitor(Main.WEBMONITOR_HOST);
						wm.send(tmp, bb.getShort(DATA), 0);

					} else if (count == 0) {
						tmp = bb.getShort(DATA);
					}
					*/
				//	Thread.sleep(500);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

//	public static class SerialWriter implements Runnable {
//
//		private static final byte READ_REQUEST = (byte) 'R';
//		private static final byte WRITE_DATA = (byte) 'W';
//
//		private static final byte CHANEL_0 = (byte) 0;
//		private static final byte CHANEL_1 = (byte) 1;
//
//		private static final byte PADDING_DATA = (byte) 0xff;
//
//		private static final int IDENTIFIER = 0;
//		private static final int CHANNEL = 1;
//		private static final int PADDING = 2;
//		private static final int LOW = 1;
//		private static final int HIGH = 2;
//
//		OutputStream out;
//		IOMonitor monitor;
//		ByteBuffer buffer;
//
//		public SerialWriter(OutputStream out, IOMonitor monitor) {
//			this.out = out;
//			this.monitor = monitor;
//			this.buffer = ByteBuffer.allocate(3);
//		}
//
//		public void run() {
//			try {
//				while (!Thread.interrupted()) {
//
//					switch (monitor.getId()) {
//					case (IOMonitor.ANGLE):
//						buffer.put(IDENTIFIER, READ_REQUEST);
//						buffer.put(CHANNEL, CHANEL_0);
//						buffer.put(PADDING, PADDING_DATA);
//						break;
//					case (IOMonitor.POSITION):
//						buffer.put(IDENTIFIER, READ_REQUEST);
//						buffer.put(CHANNEL, CHANEL_1);
//						buffer.put(PADDING, PADDING_DATA);
//						break;
//					case (IOMonitor.Y):
//						buffer.put(IDENTIFIER, WRITE_DATA);
//						buffer.put(LOW, monitor.getByteLow());
//						buffer.put(HIGH, monitor.getByteHigh());
//						break;
//					}
//					out.write(buffer.array());
//					// Sleep
//					Thread.sleep(10);
//				}
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			Util.print("Thread stopped!");
//
//		}
//	}
}
