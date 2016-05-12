package traceapp.core;


/**
 * Used to internally represent packets in the TraceApp.
 * 
 * Should have fields for source mac, destination mac, protocol (tcp/udp), 
 * 
 * @author Aaron Pabst
 *
 */
public class Packet {
	
	private long dst, src; // dst and source mac addresses
	private long dstIp, srcIp; // The ip address that this packet is being sent to.
	int etherType; 
	String protocol;
	byte[] data;
	
	public Packet(long dst, long dstIp, long srcIp, long src, String protocol, int etherType, byte[] data){
		this.dst = dst;
		this.src = src;
		this.protocol = protocol;
		this.etherType = etherType;
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.data = data;
	}
	
	/**
	 * 
	 * @return This packet's protocol as a string (tcp/udp)
	 */
	public String getProtocol(){
		return protocol;
	}
	
	/**
	 * 
	 * @return The source address of this packet as a string
	 */
	public long getSource(){
		return src;
	}
	
	/**
	 * 
	 * @return The destination address of this packet as a string
	 */
	public long getDestination(){
	   return dst;
	}
	
	/**
	 * 
	 * @return The Ethernet type of this packet as a string.
	 */
	public int getEtherType(){
		return etherType;
	}
	
	public long getSourceIp(){
		return srcIp;
	}
	
	public long getDstIp(){
		return dstIp;
	}
	
	public byte[] getData(){
		return data;
	}
	
	@Override
	public String toString(){
		String result = "";
		result += "\nSource: " + ((Long)src).toString() + "\n";
		result += "Destination: " + ((Long)dst).toString() + "\n";
		result += "Protocol: " + protocol + "\n";
		result += "EtherType: " + ((Integer)etherType).toString() + "\n";
		
		return result;
	}

	public long getSourceMac() {
		return src;
	}

}
