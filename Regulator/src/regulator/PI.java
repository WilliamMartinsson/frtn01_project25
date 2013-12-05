package regulator;
public class PI {
    private PIParameters p;

    private double error, v, I;

    /** Constructor. */
    public PI(String name){
        p = new PIParameters();
        p.K    = 1;
        p.Ti   = 0;
        p.Tr   = 10;
        p.Beta = 1;
        p.H    = 0.1;
        p.integratorOn = false;
        this.setParameters(p);
        I = 0;
        error = 0;
        v = 0;
    }

    /** Calculates the controller output. */
    public synchronized double calculateOutput(double y, double yref){
        error = yref - y;
        v = p.K * (p.Beta * yref - y) + I;
        return v;
    }

    /** Updates the internal states of the controller. */
    public synchronized void updateState(double u){
        if (p.integratorOn == true)
                I = I + (p.K * p.H/p.Ti) * error + (p.H/p.Tr) * (u-v);
        else
                I = 0.0;
    }

    /** Returns the sample time in milliseconds. */
    public synchronized long getHMillis(){
       return (long)(p.H * 1000);
    }

    /** Updates the parameters of the controller. */
    public synchronized void setParameters(PIParameters newParameters){
        p = (PIParameters) newParameters.clone();
        if(!p.integratorOn)
            I = 0;
        
    }

    /** Returns the parameters of the controller. */
    public synchronized PIParameters getParameters(){
        return p;
    }

    /** Reset the states of the controller, called when changing mode in Regul. */
    public synchronized void reset(){
        I = 0;
    }
}

