package regulator;
public class PID {
    private PIDParameters p;

    private double I;
    private double D;

    private double v;
    private double e;

    private double ad,bd, yOld;

    // Constructor
    public PID(String name) {
        p = new PIDParameters();
        p.Beta = 1.0;
        p.H = 0.01;
        p.integratorOn = false;
        p.K = -0.1;
        p.Ti = 0.0;
        p.Tr = 10.0;
        p.Td = 1.7;
        p.N = 7;

        setParameters(p);

        this.I = 0.0;
        this.D = 0.0;

        this.ad = p.Td / (p.Td + p.N*p.H);
        this.bd = p.K*this.ad*p.N;

        this.v = 0.0;
        this.e = 0.0;
    }

    // Calculates the control signal v. Called from BeamAndBallRegul.
    public synchronized double calculateOutput(double yball, double yref){
        this.e = yref - yball;
        this.D = this.ad * this.D - this.bd * (yball - this.yOld);
        this.v = p.K * (p.Beta * yref - yball) + I + D;
        this.yOld = yball;
        return this.v;
    }

    // Updates the controller state. Should use tracking-based anti-windup
    // Called from BeamAndBallRegul.
    public synchronized void updateState(double u){
        if (p.integratorOn) {
              I = I + (p.K * p.H / p.Ti) * e + (p.H / p.Tr) * (u - this.v);
        } else {
          I = 0.0;
        }
    }

    // Returns the sampling interval expressed as a long.
    public synchronized long getHMillis() {
        return (long) (p.H * 1000.0);
    }

    // Sets the parameters. Called from PIDGUI. Must clone newParameters.
    public synchronized void setParameters(PIDParameters newParameters){
        p = (PIDParameters)newParameters.clone();
        this.ad = p.Td / (p.Td + p.N*p.H);
        this.bd = p.K*this.ad*p.N;
    }

    /** Returns the parameters of the controller. */
    public synchronized PIDParameters getParameters(){
        return p;
    }

    public void reset() {
        D = 0;
        I = 0;

    }

}
