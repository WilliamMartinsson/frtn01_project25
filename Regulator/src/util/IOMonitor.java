package util;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class IOMonitor {
	
	public static final int ANGLE = 0;
	public static final int POSITION = 1;
	public static final int Y = 2;

	protected double value;
	private int id;
	private static HashMap<Integer, IOMonitor> ids = new HashMap<Integer, IOMonitor>();

	private IOMonitor(int id) {
		this.id = id;
		value = 0;
	}

	public synchronized void setValue(double value) {
		this.value = value;
	}

	public synchronized double getValue() {
		return this.value;
	}

	public synchronized byte getByteHigh() {
		return (byte) ((((short) this.value)>>8) & 0xff);
	}

	public synchronized byte getByteLow() {
		return (byte) (((short) this.value) & 0xff);
	}

	public synchronized int getId() {
		return id;
	}

	public static synchronized IOMonitor getIO(int id) {
		if (ids.containsKey(id)) {
			return ids.get(id);
		} else {
			IOMonitor io = new IOMonitor(id);
			ids.put(id, io);
			return io;
		}
	}


}
