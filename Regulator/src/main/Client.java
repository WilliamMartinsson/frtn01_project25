package main;

import regulator.PIDParameters;
import regulator.PIParameters;
import regulator.Regul;
import regulatorsocket.RegulatorSocket;
import regulatorsocket.SocketMonitor;
import util.IOMonitor;
import webmonitor.WebMonitor;

import java.io.IOException;
import java.util.HashMap;

public class Client {
	
	public Client(){
		IOMonitor angle = IOMonitor.getIO(IOMonitor.ANGLE);
		IOMonitor pos = IOMonitor.getIO(IOMonitor.POSITION);
		IOMonitor y = IOMonitor.getIO(IOMonitor.Y);

		SocketMonitor socketMonitor = null;
		try {
			RegulatorSocket rs = new RegulatorSocket(Main.REGULATOR_PORT,Main.REGULATOR_HOST);
			socketMonitor = rs.getMonitor();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

        WebMonitor webMonitor = new WebMonitor(Main.WEBMONITOR_HOST);

		Regul regul = new Regul(0, angle, pos, y, webMonitor);

        webMonitor.setConfiguration(false);
        HashMap<String, Double> PIconfig = webMonitor.getConfiguration(false);

        // WARNING:  If these values are **** then the process will be ****
        PIParameters piParameters = new PIParameters();
        piParameters.K    = PIconfig.get("k");
        piParameters.Ti   = PIconfig.get("ti");
        piParameters.Tr   = PIconfig.get("tr");
        piParameters.Beta = PIconfig.get("beta");
        piParameters.H    = PIconfig.get("h");
        piParameters.integratorOn = false;
        regul.setInnerParameters(piParameters);

        webMonitor.setConfiguration(true);
        HashMap<String, Double> PIDconfig = webMonitor.getConfiguration(true);
        // WARNING:  If these values are **** then the process will be ****
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
        regul.setOuterParameters(pidParameters);

        webMonitor.setReference();
        HashMap<String, String> mmmn =  webMonitor.getReference();

		regul.setBALLMode();


//		TwoWaySerialComm comm = new TwoWaySerialComm(new String[] {}, angle,
//				pos, y,"/dev/ttyUSB0",57600,1024);
//		comm.start();
//		regul.start();
	}

	public static void main(String[] args) {
		new Client();
	}

}
