package main;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import regulatorsocket.RegulatorSocket;

public class Main {

	public static final long MAIN_PERIOD = 10;
	public static final long WEB_RATE = 100;

	public static final String WEBMONITOR_HOST = "cloudregulator.herokuapp.com";

	public static final String REGULATOR_HOST = "olivetti.control.lth.se";
	public static final int REGULATOR_PORT = 12345;

	public static void setUpCloud(int port) throws IOException {
		//RegulatorSocket server = new RegulatorSocket(port);
		//server.open();
		new Server().start();
	}

	public static void setUpPI(int port, String host) throws IOException {
		// OBS! Använder inte indata!!!!
		new Client().start();
		// client.push();
	}

	/*
	 * Server mode java RegulatorSocket -server 12345
	 * 
	 * Client mode java RegulatorSocket -client 12345 olivetti.control.lth.se
	 */
	public static void main(String[] args) {
		// Map containing all the parameters sent to the program at startup.
		HashMap<String, LinkedList<String>> argMap = new HashMap<String, LinkedList<String>>();

		// Moving all parameters from args[] to argMap
		String currentArg = "general";
		LinkedList<String> currentParameters = new LinkedList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) {
				argMap.put(currentArg, currentParameters);
				currentParameters = new LinkedList<String>();
				currentArg = arg;
			} else {
				currentParameters.add(arg);
			}
		}
		argMap.put(currentArg, currentParameters);

		// Changing the workflow based on the choice parameters
		if (argMap.containsKey("-server") && argMap.containsKey("-client")) {
			throw new IllegalArgumentException(
					"Can't set both the parameters server and client at same time.");
		} else if (!argMap.containsKey("-server")
				&& !argMap.containsKey("-client")) {
			throw new IllegalArgumentException(
					"Need to set at least one of the parameters server or client.");
		} else if (argMap.containsKey("-server")) {
			LinkedList<String> param = argMap.get("-server");
			if (param != null && param.size() == 1) {
				int port = 0;
				try {
					port = Integer.parseInt(param.getFirst());
					setUpCloud(port);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The server port ("
							+ param.getFirst() + ") has to be numerical.");
				} catch (Exception e) {
					System.out
							.println("Exception caught when trying to setup regulator server on port: "
									+ port);
					e.printStackTrace();
				}
			} else {
				throw new IllegalArgumentException(
						"The parameter server takes one argument, which is the server port.");
			}
		} else {
			LinkedList<String> param = argMap.get("-client");
			if (param != null && param.size() == 2) {
				int port = 0;
				try {
					port = Integer.parseInt(param.getFirst());
					setUpPI(port, param.getLast());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("The server port ("
							+ param.getFirst() + ") has to be numerical.");
				} catch (Exception e) {
					System.out
							.println("Exception caught when trying to setup regulator client connection to server ["
									+ param.getLast() + ":" + port + "]");
					e.printStackTrace();
				}
			} else {
				throw new IllegalArgumentException(
						"The parameter server takes one argument, which is the server port.");
			}
		}
	}

}
