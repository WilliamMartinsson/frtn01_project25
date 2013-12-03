package main;

import pi2avr.TwoWaySerialComm;
import regulator.Regul;
import util.IO;

public class Client {
	
	public Client(){
        IO angle = IO.getIO(0);
		IO pos = IO.getIO(1);
		IO signal = IO.getIO(2);
		signal.setValue(-10);
		Regul regul = new Regul(0, angle, pos, signal);
		TwoWaySerialComm comm = new TwoWaySerialComm(new String[] {}, angle,
				pos, signal,"/dev/ttyUSB0",57600,1024);
		comm.start();
		regul.start();
	}

	public static void main(String[] args) {
		new Client();
	}

}
