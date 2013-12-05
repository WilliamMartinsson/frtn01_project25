package regulator;
public class PIParameters implements Cloneable {
	public double K;
	public double Ti;
	public double Tr;
	public double Beta;
	public double H;
	public boolean integratorOn;
	
	public Object clone() {
		try {
			return super.clone();
		} catch (Exception x) {
			return null;
		}
	}
	
    public String toString(){
    	return ("K: " + String.valueOf(K) + " Ti: " + String.valueOf(Ti) + " Tr: "
    			+  String.valueOf(Tr) + " Beta: " +  String.valueOf(Beta) 
    			+ " H: " +  String.valueOf(H));
    }
}

