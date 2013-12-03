package main;

import pi2avr.TwoWaySerialComm;
import regulator.Regul;
import util.IOMonitor;

public class Client {
	
	public Client(){
		IOMonitor angle = IOMonitor.getIO(IOMonitor.ANGLE);
		IOMonitor pos = IOMonitor.getIO(IOMonitor.POSITION);
		IOMonitor y = IOMonitor.getIO(IOMonitor.Y);
		y.setValue(-10);
		Regul regul = new Regul(0, angle, pos, y);
		//regul.setBEAMMode();
		TwoWaySerialComm comm = new TwoWaySerialComm(new String[] {}, angle,
				pos, y,"/dev/ttyUSB0",57600,1024);
		comm.start();
		regul.start();
	}

	public static void main(String[] args) {
		new Client();
	}

}
