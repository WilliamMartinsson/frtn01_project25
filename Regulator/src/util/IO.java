package util;
import java.util.HashMap;

public class IO {

	private double value;
	private int id;
	private static HashMap<Integer, IO> ids = new HashMap<Integer, IO>();

	private IO(int id) {
		this.id = id;
		value = 0;
	}

	public synchronized void setValue(double value) {
		this.value = value;
	}

	public synchronized double getValue() {
		return this.value;
	}

	public synchronized short getShortValue() {
		return (short) this.value;
	}

	public synchronized int getId() {
		return id;
	}

	public static synchronized IO getIO(int id) {
		if (ids.containsKey(id)) {
			return ids.get(id);
		} else {
			IO io = new IO(id);
			ids.put(id, io);
			return io;
		}
	}
}
