package main;

import java.io.IOException;
import java.util.HashMap;

import regulator.PIDParameters;
import regulator.PIParameters;
import regulator.Regul;
import regulatorsocket.RegulatorSocket;
import regulatorsocket.SocketMonitor;
import util.IOMonitor;
import webmonitor.WebMonitor;

public class Server extends Thread {
	private SocketMonitor socketMonitor = null;
	private IOMonitor angle = null;
	private IOMonitor pos = null;
	private IOMonitor y = null;

	private WebMonitor webMonitor = null;

	private RegulatorSocket rs = null;
	private Regul regul = null;

	public Server() {
		angle = IOMonitor.getIO(IOMonitor.ANGLE);
		pos = IOMonitor.getIO(IOMonitor.POSITION);
		y = IOMonitor.getIO(IOMonitor.Y);

		try {
			RegulatorSocket rs = new RegulatorSocket(Main.REGULATOR_PORT);
			socketMonitor = rs.getMonitor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		WebMonitor webMonitor = new WebMonitor(Main.WEBMONITOR_HOST);

		Regul regul = new Regul(0, angle, pos, y, webMonitor);

		webMonitor.setConfiguration(false);
		HashMap<String, Double> PIconfig = webMonitor.getConfiguration(false);

		// WARNING: If these values are **** then the process will be ****
		PIParameters piParameters = new PIParameters();
		piParameters.K = PIconfig.get("k");
		piParameters.Ti = PIconfig.get("ti");
		piParameters.Tr = PIconfig.get("tr");
		piParameters.Beta = PIconfig.get("beta");
		piParameters.H = PIconfig.get("h");
		piParameters.integratorOn = false;
		regul.setInnerParameters(piParameters);

		webMonitor.setConfiguration(true);
		HashMap<String, Double> PIDconfig = webMonitor.getConfiguration(true);
		// WARNING: If these values are **** then the process will be ****
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
		HashMap<String, String> mmmn = webMonitor.getReference();

		regul = new Regul(0, angle, pos, y, webMonitor);

		regul.setBALLMode();
	}

	public void start() {
		rs.open();
		regul.start();
		super.start();
	}

	public void run() {
		try {
			int i = 0;
			while (!Thread.interrupted()) {
				if (i++ == Main.WEB_RATE) {
					webMonitor.send(angle.getValue(), pos.getValue(),
							socketMonitor.getPing(), y.getValue());
					i = 0;
				}
				angle.setValue(socketMonitor.getReceiveData1());
				pos.setValue(socketMonitor.getReceiveData2());
				socketMonitor.setSendData1(y.getValue());
				Thread.sleep(Main.MAIN_PERIOD);
			}
		} catch (InterruptedException e) {
			System.out.println("Client was force closed!");
		}
	}

	public static void main(String[] args) {
		new Server();
	}

}
