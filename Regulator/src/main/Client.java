package main;

import pi2avr.TwoWaySerialComm;
import regulator.PIDParameters;
import regulator.PIParameters;
import regulator.Regul;
import regulatorsocket.Util;
import util.IOMonitor;
import webmonitor.WebMonitor;

import java.util.HashMap;

public class Client {
	
	public Client(){
		IOMonitor angle = IOMonitor.getIO(IOMonitor.ANGLE);
		IOMonitor pos = IOMonitor.getIO(IOMonitor.POSITION);
		IOMonitor y = IOMonitor.getIO(IOMonitor.Y);


        long time = System.currentTimeMillis();
        WebMonitor webMonitor = new WebMonitor(Main.WEBMONITOR_HOST);
        HashMap<String, Double> constants = webMonitor.getConfiguration();
        Util.print("[GET] Constants: " + (System.currentTimeMillis() - time) + "ms");


		Regul regul = new Regul(0, angle, pos, y, webMonitor);
        PIParameters piParameters = new PIParameters();
        piParameters.K    = constants.get("k");
        piParameters.Ti   = constants.get("ti");
        piParameters.Tr   = constants.get("tr");
        piParameters.Beta = constants.get("beta");
        piParameters.H    = constants.get("h");
        piParameters.integratorOn = false;
        regul.setInnerParameters(piParameters);


        PIDParameters pidParameters = new PIDParameters();
        pidParameters = new PIDParameters();
        pidParameters.Beta = constants.get("beta");
        pidParameters.H = constants.get("h");
        pidParameters.integratorOn = false;
        pidParameters.K = constants.get("k");
        pidParameters.Ti = constants.get("ti");
        pidParameters.Tr = constants.get("tr");
        pidParameters.Td = constants.get("td");
        pidParameters.N = constants.get("n");
        regul.setOuterParameters(pidParameters);

		regul.setBALLMode();


		TwoWaySerialComm comm = new TwoWaySerialComm(new String[] {}, angle,
				pos, y,"/dev/ttyUSB0",57600,1024);
		comm.start();
		regul.start();
	}

	public static void main(String[] args) {
		new Client();
	}

}
