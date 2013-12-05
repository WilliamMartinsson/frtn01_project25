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
    private static final String REFERENCE_URI = "/reference";

    private HashMap<String, Double> PIconfig;
    private HashMap<String, Double> PIDconfig;

    private HashMap<String, String> reference;

    /**
     * Creates a WebMonitor with a specified URL.
     * @param url
     */
    public WebMonitor(String url) {
        this.url = "http://" + url;
        PIDconfig = new HashMap<String, Double>();
        PIconfig = new HashMap<String, Double>();
        reference = new HashMap<String, String>();
        System.setProperty("http.keepAlive", "false"); // Don't keep connections alive
    }

    /**
     * Sends a POST request to defined url.
     * @param angle
     * @param position
     * @param latency
     */
	public void send(double angle, double position, double latency, double controlOutput) {
        HttpURLConnection connection;
        OutputStream output;
        InputStream response;

        try {
            connection = (HttpURLConnection) new URL(url + SENSOR_OUTPUT_URI).openConnection();
            connection.setDoOutput(true); // Implicitly sets request method to HTTP POST

            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

            output = connection.getOutputStream();
            output.write(WebMonitor.postParams(angle, position, latency, controlOutput));

            response = connection.getInputStream(); // fires the POST request
            response.close();
            output.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Fetches the configuration in JSON-format from the specified URL.
     * @return
     */
    public void setConfiguration(boolean isPID) {
        String pid = "?pid=false";
        if (isPID){
            pid = "?pid=true";
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url + PROCESS_CONSTANTS_URI + pid).openStream()));
            String jsonString = WebMonitor.inputStreamToString(in);
            if (isPID){
                PIDconfig = WebMonitor.toHashConfig(jsonString);
            } else {
                PIconfig = WebMonitor.toHashConfig(jsonString);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void setReference() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url + REFERENCE_URI).openStream()));
            String jsonString = WebMonitor.inputStreamToString(in);
            reference = this.toHashReference(jsonString);
        } catch (IOException e) { e.printStackTrace(); }
    }


    public HashMap<String, Double> getConfiguration(boolean isPID) {
        return isPID ? PIDconfig : PIconfig;
    }

    public HashMap<String, String> getReference() {
        return reference;
    }


    private static byte[] postParams(double angle, double position, double latency, double controlOutput) {
        return String.format("angle=%f&position=%f&latency=%f&output=%f",  angle, position, latency, controlOutput).getBytes();
    }


    private static String inputStreamToString(BufferedReader in) {
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

    private HashMap<String, String> toHashReference(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String man = String.valueOf(json.getBoolean("manual_mode"));
            reference.put("manual", man);
            String ref = String.valueOf(json.getDouble("reference_value"));
            reference.put("reference", ref);
        } catch (JSONException e) { e.printStackTrace(); }

        return reference;
    }

}
