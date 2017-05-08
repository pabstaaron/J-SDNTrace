package core;

/**
 * Wrapper class for associating ports with mac addresses.
 */
public class PortMacPair{
	private long mac;
	private int port;
	
	public void setMac(long mac){
		this.mac = mac;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public long getMac(){
		return mac;
	}
	
	public int getPort(){
		return port;
	}
}