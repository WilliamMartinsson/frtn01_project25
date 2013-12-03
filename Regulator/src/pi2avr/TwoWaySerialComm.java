package pi2avr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import regulatorsocket.Packet;
import regulatorsocket.Util;

import util.IO;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

// Dependencies:
// $ sudo apt-get install librxtx-java

// Compile and run with:
// $ javac -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm.java
// $ java -Djava.library.path=/usr/lib/jni -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm
// or with optional main arguemnts
// $ java -Djava.library.path=/usr/lib/jni -cp /usr/share/java/RXTXcomm.jar:. TwoWaySerialComm "/dev/tty/USB0" baudRate bufferSize # (baudRate and bufferSize musts be ints)
public class TwoWaySerialComm {

	IO signal;

	public TwoWaySerialComm(String[] args, IO angle, IO pos, IO signal) {
		this.signal = signal;
		// Defaults
		String serialPort = "/dev/ttyUSB0";
		// int baudRate = 38400;
		int baudRate = 57600;
		int bufferSize = 1024; // TODO: Find an appropriate buffer size

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

		// TODO: Potentially not necessary to set it explicitly with
		// System.setProperty
		// Set SerialPorts property for gnu.io.rxtx
		System.setProperty("gnu.io.rxtx.SerialPorts", serialPort);
		try {
			System.out.println("\n\n[CONNECT]: serial=" + serialPort
					+ ", baudRate=" + baudRate + ", bufferSize=" + bufferSize);
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
				(new Thread(new SerialReader(in, bufferSize))).start();
				(new Thread(new SerialWriter(out, signal))).start();

			} else {
				System.out.println("[ERROR] Only serial ports are implemented");
			}
		}
	}

	public static class SerialReader implements Runnable {

		InputStream in;
		int bufferSize;

		public SerialReader(InputStream in, int bufferSize) {
			this.in = in;
			this.bufferSize = bufferSize;
		}

		public void run() {
			// byte[] buffer = new byte[this.bufferSize];
			byte[] buffer = new byte[16];
			int len = -1;
			try {
				while ((len = this.in.read(buffer)) > -1) {
					// TODO: Implement
					/*
					 * TODO: Outputs everything that is printed to terminal i.e
					 * if you write "HEJ" and press ENTER output will be:
					 * [OUTPUT]='H' [OUTPUT]='EJ '
					 */
					// ByteBuffer bb = ByteBuffer.wrap(buffer);
					System.out.print("[OUTPUT]=");
					for (int i = 0; i < buffer.length; i++) {
						// System.out.print(bb.getChar());
						System.out.println(Integer
								.toBinaryString((int) buffer[i]));
					}
					System.out.println();
					// System.out.println("[OUTPUT]='" + new String(buffer, 0,
					// len) + "'");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class SerialWriter implements Runnable {

		OutputStream out;
		IO signal;

		public SerialWriter(OutputStream out, IO signal) {
			this.out = out;
			this.signal = signal;
		}

		public void run() {
			try {
				long i = 0;
				while (!Thread.interrupted()) {
					ByteBuffer bb = ByteBuffer.allocate(2);
					bb.putShort(signal.getShortValue());
/*
					System.out.print("[DEBUG]: (");
					System.out.print(Integer.toBinaryString((int) bb.array()[0]));
					System.out.print(", ");
					System.out.print(Integer.toBinaryString((int) bb.array()[1]));
					System.out.println(")");
*/
					this.out.write(bb.array());
					// Sleep
					Thread.sleep(10);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Util.print("Thread stopped!");

			try {
				int c = 0;
				while ((c = System.in.read()) > -1) {
					// TODO: Implement
					this.out.write(c);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}