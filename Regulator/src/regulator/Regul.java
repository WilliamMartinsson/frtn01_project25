package regulator;

import se.lth.control.realtime.Semaphore;
import util.IOMonitor;
import webmonitor.WebMonitor;

public class Regul extends Thread {
	public static final int OFF = 0;
	public static final int BEAM = 1;
	public static final int BALL = 2;

	private PI inner = new PI("PI");
	private PID outer = new PID("PID");

	private IOMonitor analogInAngle;
	private IOMonitor analogInPosition;
	private IOMonitor analogOut;

	private ReferenceGenerator referenceGenerator;
	// private OpComProxy opcom;

	private int priority;
	private boolean WeShouldRun = true;

	private Semaphore mutex; // used for synchronization at shut-down

	private ModeMonitor modeMon;

    private WebMonitor webMonitor;

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


	public Regul(int pri, IOMonitor angle, IOMonitor pos, IOMonitor ref, WebMonitor webMonitor) {
		this.priority = pri;
        this.mutex = new Semaphore(1);
        this.analogInAngle = angle;
		// analogInAngle = new AnalogIn(0);
        this.analogInPosition = pos;
		// analogInPosition = new AnalogIn(1);
        this.analogOut = ref;
		// analogOut = new AnalogOut(0);
        this.modeMon = new ModeMonitor();
        this.webMonitor = webMonitor;
	}

	public void setRefGen(ReferenceGenerator referenceGenerator) {
		this.referenceGenerator = referenceGenerator;
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
		long duration;
		long t = System.currentTimeMillis();
		double angle = 0;
		double position = 0;

		//this.setPriority(priority);
		mutex.take();
		while (WeShouldRun) {
			switch (modeMon.getMode()) {
			case OFF: {
				inner.reset();
				outer.reset();
                this.asyncPostToWebMonitor(0, 0, 0, 0); // Sends data to Web Monitoring service (async)
				try {
					angle = analogInAngle.getValue();
					position = analogInPosition.getValue();
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
				double u = limit(inner.calculateOutput(yAnalog, yref), -512, 511);
                this.asyncPostToWebMonitor(yAnalog, 0, 0, u); // Sends data to Web Monitoring service (async)
				inner.updateState(u);
				try {
					analogOut.setValue(u);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}
				break;
			}
			case BALL: {
				try {
					angle = analogInAngle.getValue();
					position = analogInPosition.getValue();
				} catch (Exception e) {
					System.out
							.println("Failed to get analog output: angle or position");
				}
				System.out.println("Angle: " + angle);
				System.out.println("===============Position: " + position);
                double ref = referenceGenerator.getRef();
                System.out.println("Ref: " + ref);
				double uOuter = limit(outer.calculateOutput(position, ref), -512, 511);
				double u = limit(inner.calculateOutput(angle, uOuter), -512, 511);
                double controlOutput = u;
				try {
					analogOut.setValue(controlOutput);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}
				System.out.println("====================================== U:" + u);
				outer.updateState(uOuter);
				inner.updateState(u);

				this.asyncPostToWebMonitor(angle, position, 0, controlOutput); // Sends data to Web Monitoring service (async)

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

    // Asynchronously sends data to WebMonitor
    public void asyncPostToWebMonitor(final double angle, final double position, final double latency, final double controlOutput){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    webMonitor.send(angle, position, latency, controlOutput);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        new Thread(task, "WebMonitorThread").start();
    }


}