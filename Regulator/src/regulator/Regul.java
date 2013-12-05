package regulator;

import se.lth.control.realtime.Semaphore;
import util.IOMonitor;
import webmonitor.WebMonitor;

import java.util.HashMap;

public class Regul extends Thread {
	public static final int OFF = 0;
	public static final int BEAM = 1;
	public static final int BALL = 2;

	private PI inner = new PI("PI");
	private PID outer = new PID("PID");

	private IOMonitor analogInAngle;
	private IOMonitor analogInPosition;
	private IOMonitor analogOut;

    private long runCount;

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
        this.setRefGen(new ReferenceGenerator(1));
        this.runCount = 0;
	}

	public void setRefGen(ReferenceGenerator referenceGenerator) {
		this.referenceGenerator = referenceGenerator;
        this.referenceGenerator.start();
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

                referenceGenerator.setMode(webMonitor.getRefMode());
                double webRef = webMonitor.getRefValue();
//                System.out.println("WEBREF: " + webRef);
                referenceGenerator.setAmplitude(webRef);

                double ref = referenceGenerator.getRef();
//                System.out.println("REF:    " + ref);


                double uOuter = limit(outer.calculateOutput(position, ref), -512, 511);
				double u =      limit(inner.calculateOutput(angle, uOuter), -512, 511);
                double controlOutput = u;
				try {
					analogOut.setValue(controlOutput);
				} catch (Exception e) {
					System.out.println("Failed to write to analog output");
				}
				outer.updateState(uOuter);
				inner.updateState(u);

                if (runCount%5 == 0)
                    this.asyncPostToWebMonitor(angle, position, 0, controlOutput);
                if (runCount%10 == 0)
                    this.asyncSetOfreference();
                if (runCount%50 == 0)
                    this.asyncSetOfconfig();

                // Always sets new parameters for both PID and PI
                this.setParameters();



                runCount++;
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

    private void setParameters() {
        // WARNING:  If these values are **** then the process will be ****
        HashMap<String, Double> PIconfig = webMonitor.getConfiguration(false);
        PIParameters piParameters = new PIParameters();
        piParameters.K    = PIconfig.get("k");
        piParameters.Ti   = PIconfig.get("ti");
        piParameters.Tr   = PIconfig.get("tr");
        piParameters.Beta = PIconfig.get("beta");
        piParameters.H    = PIconfig.get("h");
        piParameters.integratorOn = false;

        this.setInnerParameters(piParameters);

        // WARNING:  If these values are **** then the process will be ****
        HashMap<String, Double> PIDconfig = webMonitor.getConfiguration(true);
        PIDParameters pidParameters = new PIDParameters();
        pidParameters = new PIDParameters();
        pidParameters.Beta = PIDconfig.get("beta");
        pidParameters.H = PIDconfig.get("h");
        pidParameters.integratorOn = false;
        pidParameters.K = PIDconfig.get("k");
        pidParameters.Ti = PIDconfig.get("ti");
        pidParameters.Tr = PIDconfig.get("tr");
        pidParameters.Td = PIDconfig.get("td");
        pidParameters.N = PIDconfig.get("n");

        this.setOuterParameters(pidParameters);
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


    // Asynchronously set data to WebMonitor
    public void asyncSetOfconfig(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    webMonitor.setConfiguration(true);
                    webMonitor.setConfiguration(false);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        new Thread(task, "WebMonitorThread1").start();
    }

    // Asynchronously sends data to WebMonitor
    public void asyncSetOfreference(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    webMonitor.setReference();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        new Thread(task, "WebMonitorThread2").start();
    }


}