package regulator;
public class PIDParameters implements Cloneable {
	public double K;
	public double Ti;
	public double Tr;
	public double Td;
	public double N;
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
    			+  String.valueOf(Tr) + " Td: " +  String.valueOf(Td)
    			+ " N: " +  String.valueOf(N) + " Beta: " +  String.valueOf(Beta) 
    			+ " H: " +  String.valueOf(H));
    }
    
}

