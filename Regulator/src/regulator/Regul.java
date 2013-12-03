package regulator;

import se.lth.control.realtime.Semaphore;
import util.IO;

public class Regul extends Thread {
	public static final int OFF = 0;
	public static final int BEAM = 1;
	public static final int BALL = 2;

	private PI inner = new PI("PI");
	private PID outer = new PID("PID");

	private IO analogInAngle;
	private IO analogInPosition;
	private IO analogOut;

	private ReferenceGeneratorProxy referenceGenerator;
	// private OpComProxy opcom;

	private int priority;
	private boolean WeShouldRun = true;
	private long starttime;
	private Semaphore mutex; // used for synchronization at shut-down

	private ModeMonitor modeMon;

	// Inner monitor class
	class ModeMonitor {
		private int mode;

		// Synchronized access methods
		public synchronized void setMode(int newMode) {
			mode = newMode;
		}

		public synchronized int getMode() {
			return mode;
		}
	}

	public Regul(int pri, IO angle, IO pos, IO ref) {
		priority = pri;
		mutex = new Semaphore(1);
		analogInAngle = angle;
		// analogInAngle = new AnalogIn(0);
		analogInPosition = pos;
		// analogInPosition = new AnalogIn(1);
		analogOut = ref;
		// analogOut = new AnalogOut(0);
		modeMon = new ModeMonitor();
	}

	/*
	 * public void setOpCom(OpComProxy opcom) { this.opcom = opcom; }
	 */

	public void setRefGen(ReferenceGeneratorProxy referenceGenerator) {
		this.referenceGenerator = referenceGenerator;
	}

	// Called in every sample in order to send plot data to OpCom
	private void sendDataToOpCom(double yref, double y, double u) {
		// TODO: Implement send to gui server.
	}

	public synchronized void setInnerParameters(PIParameters p) {
		inner.setParameters(p);
	}

	public synchronized PIParameters getInnerParameters() {
		return inner.getParameters();
	}

	public synchronized void setOuterParameters(PIDParameters p) {
		outer.setParameters(p);
	}

	public synchronized PIDParameters getOuterParameters() {
		return outer.getParameters();
	}

	public void setOFFMode() {
		modeMon.setMode(OFF);

	}

	public void setBEAMMode() {
		modeMon.setMode(BEAM);

	}

	public void setBALLMode() {
		modeMon.setMode(BALL);
	}

	public int getMode() {
		return modeMon.getMode();
	}

	// Called from OpCom when shutting down
	public synchronized void shutDown() {
		WeShouldRun = false;
		mutex.take();
		analogOut.setValue(0);
	}

	private double limit(double v, double min, double max) {
		if (v < min) {
			v = min;
		} else {
			if (v > max) {
				v = max;
			}
		}
		return v;
	}

	public void run() {
		short aaaaaaaaaaaaaaaaaaa = -20;
		long duration;
		long t = System.currentTimeMillis();
		starttime = t;

		//this.setPriority(priority);
		mutex.take();
		while (WeShouldRun) {
			switch (modeMon.getMode()) {
			case OFF: {
				inner.reset();
				outer.reset();
				this.sendDataToOpCom(0, 0, 0);
				try {
					analogOut.setValue(aaaaaaaaaaaaaaaaaaa);
					aaaaaaaaaaaaaaaaaaa *= -1;
					Thread.sleep(5000);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}
				break;
			}
			case BEAM: {
				// double yref = referenceGenerator.getRef();
				double yref = 0.0;
				double yAnalog = 0;
				try {
					yAnalog = analogInAngle.getValue();
				} catch (Exception e) {
					System.out.println("Failed to get angle value");
				}
				double u = limit(inner.calculateOutput(yAnalog, yref), -10, 10);
				this.sendDataToOpCom(yref, yAnalog, u);
				inner.updateState(u);
				try {
					analogOut.setValue(u);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}
				break;
			}
			case BALL: {
				double angle = 0;
				double position = 0;
				try {
					angle = analogInAngle.getValue();
					position = analogInPosition.getValue();
				} catch (Exception e) {
					System.out
							.println("Failed to get analog output: angle or position");
				}
				// double ref = referenceGenerator.getRef();
				double ref = 0.0;
				double uOuter = limit(outer.calculateOutput(position, ref),
						-10, 10);
				double u = limit(inner.calculateOutput(angle, uOuter), -10, 10);
				try {
					analogOut.setValue(u);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}

				outer.updateState(uOuter);
				inner.updateState(u);
				System.out.println("Styr:" + u);
				this.sendDataToOpCom(ref, position, u);
				break;
			}
			default: {
				System.out.println("Error: Illegal mode.");
				break;
			}
			}
			// sleep
			t = t + inner.getHMillis();
			duration = t - System.currentTimeMillis();
			if (duration > 0) {
				try {
					sleep(duration);
				} catch (InterruptedException x) {
				}
			}
		}
		mutex.give();
	}
}