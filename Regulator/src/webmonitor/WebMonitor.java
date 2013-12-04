package webmonitor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;


public class WebMonitor {
    private String url;
    private static final String CHARSET = "UTF-8";
    private static final String SENSOR_OUTPUT_URI = "/sensor_outputs";
    private static final String PROCESS_CONSTANTS_URI = "/config";

    /**
     * Creates a WebMonitor with a specified URL.
     * @param url
     */
    public WebMonitor(String url) {
        this.url = "http://" + url;
        System.setProperty("http.keepAlive", "false"); // Don't keep connections alive
    }

    /**
     * Sends a POST request to defined url.
     * @param angle
     * @param position
     * @param latency
     */
	public void send(int angle, int position, int latency) {
        HttpURLConnection connection;
        OutputStream output;
        InputStream response;

        try {
            connection = (HttpURLConnection) new URL(url + SENSOR_OUTPUT_URI).openConnection();
            connection.setDoOutput(true); // Implicitly sets request method to HTTP POST

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

            output = connection.getOutputStream();
            output.write(WebMonitor.postParams(angle, position, latency));

            response = connection.getInputStream(); // fires the POST request
            response.close();
            output.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Fetches the configuration in JSON-format from the specified URL.
     * @return
     */
    public HashMap<String, Double> getConfiguration() {
        HttpURLConnection connection;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url + PROCESS_CONSTANTS_URI).openStream()));
            String jsonString = WebMonitor.inputStreamToString(in);
            return WebMonitor.toHashConfig(jsonString);
        } catch (IOException e) { e.printStackTrace(); }

        return null;
    }


    private static byte[] postParams(int angle, int position, int latency) {
        return String.format("angle=%d&position=%d&latency=%d",  angle, position, latency).getBytes();
    }


    private static String inputStreamToString(BufferedReader in) {
        byte[] b = new byte[1024];
        String result;
        try {
            String jsonString = in.readLine();
            in.close();
            return jsonString;

        } catch (IOException e) { e.printStackTrace(); }

        return null;
    }


    private static HashMap<String, Double> toHashConfig(String jsonString) {
        HashMap<String, Double> config = new HashMap<String, Double>();
        try {
            JSONObject json = new JSONObject(jsonString);
            config.put("k", json.getDouble("k_constant"));
            config.put("ti", json.getDouble("ti_constant"));
            config.put("tr", json.getDouble("tr_constant"));
            config.put("td", json.getDouble("td_constant"));
            config.put("n", json.getDouble("n_constant"));
            config.put("beta", json.getDouble("beta_constant"));
            config.put("h", json.getDouble("h_constant"));
        } catch (JSONException e) { e.printStackTrace(); }
        return config;
    }

}
