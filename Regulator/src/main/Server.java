package main;

import regulatorsocket.RegulatorSocket;

public class Server {
    public static void main(String[] args) {
        RegulatorSocket.main(new String[] {"-server", "12345"});
    }
}
