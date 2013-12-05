package regulator;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import se.lth.control.*;

public class ReferenceGenerator extends Thread {

    private static final int SQWAVE=0, MANUAL=1;
    private final int priority;

    private double amplitude;
    private double period;
    private double sign = -1.0;
    private double ref;
    private double manual;
    private int mode = SQWAVE;
    private boolean premature, ampChanged, periodChanged;

    public ReferenceGenerator(int refGenPriority) {
        priority = refGenPriority;
        amplitude = 4.0;
        period = 20.0*1000.0/2.0;
        manual = 0.0;
        ref = amplitude * sign;
    }

    private synchronized void wakeUpThread() {
        premature = true;
        notify();
    }

    private synchronized void sleepLight(long duration) {
        premature = false;
        try {
            wait(duration);
        } catch (Exception e) {}
    }

    private synchronized void setRef(double newRef) {
        ref = newRef;
    }

    private synchronized void setManual(double newManual) {
        manual = newManual;
    }

    private synchronized void setSqMode() {
        mode = SQWAVE;
    }

    private synchronized void setManMode() {
        mode = MANUAL;
    }

    public synchronized double getRef()
    {
        return (mode == SQWAVE) ? ref : manual;
    }

    public void run() {
        long h = (long) period;
        long duration;
        long t = System.currentTimeMillis();

        setPriority(priority);

        while (true) {
            synchronized (this) {
                sign = - sign;
                ref = amplitude * sign;
            }
            t = t + h;
            duration = t - System.currentTimeMillis();
            if (duration > 0) {
                sleepLight(duration);
                if (premature) {
                    // Woken up prematurely since the period was changed
                    h = (long) period;
                    // Reset t
                    t = System.currentTimeMillis();
                    // Keep current sign
                    sign = - sign;
                }
            }
        }
    }

}
