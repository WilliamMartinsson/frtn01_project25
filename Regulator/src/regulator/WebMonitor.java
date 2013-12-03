package regulator;

import java.net.URL;
import java.net.URLConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


public class WebMonitor {
    private String url;
    private static final String CHARSET = "UTF-8";

	public WebMonitor(String url) {
		this.url = url;
        System.setProperty("http.keepAlive", "false"); // Don't keep connections alive
	}
	
	public void send(String data) {
        String query = String.format("sensor1=%d&sensor2=%d", 10, 200); // TODO: Should use data arg

        URLConnection connection = null;
        try {
            connection = new URL(url).openConnection();
        } catch (IOException e) { e.printStackTrace(); }

        connection.setDoOutput(true); // Use HTTP POST
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

        OutputStream output = null;
        try {
            output = connection.getOutputStream();
        } catch (IOException e) { e.printStackTrace(); }

        try {
            output.write(query.getBytes(CHARSET));
        } catch (IOException e) { e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
	}


    public String getConfiguration() {
        InputStream response = null;
        String config = null;

        try {
            response = new URL(url).openStream();
        } catch (IOException e) { e.printStackTrace(); }

        try {
            config = String.valueOf(response.read()); // TODO: properly get and parse the response
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static void main(String[] args) {
        WebMonitor wm = new WebMonitor("http://localhost:3000");
        wm.getConfiguration();
    }


}
