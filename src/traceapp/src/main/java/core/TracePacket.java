package core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import core.Hop;

/**
 * Represents a deserialized trace packet.
 * 
 * TODO - Remove protocol variable
 * TODO - Change the hop field from a list of hops to a single object
 * TODO - Add a field for a timestamp
 * TODO - Add a field for ingress port
 * 
 * @author Aaron Pabst
 */
public class TracePacket extends BasePacket implements Comparable<TracePacket>{

    private MacAddress destinationMAC; //  64 bits = 8 bytes
    private MacAddress sourceMAC; // 64 bits = 8 bytes
    private byte version; // The traceapp version the trace originated from. Presently not used.
    private byte maxHops; // The maximum number of hops in a trace
    private long probeId; // The unique identifier of this packet
    
    // Reply fields
    private Date timestamp;
//    private long dpid;
    private byte hop; // The hop number

    private SwitchInfo swInfo;
    
    private final int packSize = 37 + SwitchInfo.len; // The size, in bytes, of a TracePacket
    
    /**
     * The ethernet type for trace packets
     */
    public static final int TRACE_REQUEST = 0x8220;
    
    /**
     * Indicates whether the packet is a request or a reply. True for request, false for reply.
     */
    private boolean request; 
	
    public TracePacket(){
    	super();
    	version = 1;
    	request = true;
    	hop = 0;
    	
    	// Compute a probe id
    	Random rand = new Random();
    	probeId = rand.nextLong();
    	
    	timestamp = new Date();
    	swInfo = new SwitchInfo();
    }
    
    // Define various setters and getters //
    public void setTimestamp(Date d){
    	timestamp = d;
    }
    
    public void setHop(byte hop){
    	this.hop = hop;
    }
    
    public void setDestination(MacAddress dstMac){
    	destinationMAC = dstMac;
    }
    
    public void setSwInfo(SwitchInfo inf){
    	swInfo = inf;
    }
    
    public void setSource(MacAddress srcMac){
    	sourceMAC = srcMac;
    }
    
    public void setMaxHops(byte ttl){
    	this.maxHops = ttl;
    }
    
    public void setType(boolean request){
    	this.request = request;
    }
    
    public Date getTimestamp(){
    	return timestamp;
    }
    
    public MacAddress getDestination(){
    	return destinationMAC;
    }
    
    public MacAddress getSource(){
    	return sourceMAC;
    }
    
    public byte getMaxHops(){
    	return maxHops;
    }
    
    public boolean getType(){
    	return request;
    }
    
    public byte getHop(){
    	return hop;
    }
    
    public byte getVersion(){
    	return version;
    }
    
   public SwitchInfo getSwInfo(){
	   return swInfo;
   }
    
    public long getProbeId(){
    	return probeId;
    }
    
    /**
     * Convert this TracePacket into raw data that is ready to be sent across the network.
     */
	//@Override
	public byte[] serialize() {
		
		// Compute the length of the packet in bytes
		int length = packSize;
		
        byte[] data = new byte[length];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put((byte)length);
        bb.putLong(destinationMAC.getLong()); // 8 bytes
        bb.putLong(sourceMAC.getLong()); // 8 bytes
        bb.put(version); // 1 byte
        bb.put(maxHops); // 1 byte
        bb.putLong(probeId); // 4 bytes
        bb.put(hop);
        bb.putLong(timestamp.getTime());
        
        if(request) // 1 byte
        	bb.put((byte)1);
        else
        	bb.put((byte)0);
      
        byte[] swinf = swInfo.serialize();
		bb.put(swinf, 0, swinf.length);
        
		return data;
	}

	/**
	 * Read a raw byte array into this TracePacket object, if possible.
	 * 
	 * @param data - The raw data
	 * @param offset - index to start reading at
	 * @param length - How far to read into the array
	 * 
	 * @throws PacketParsingException if the specified data is not a valid trace packet.
	 */
	//@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		
		int len = bb.get(); // Length is used elsewhere in the program. It's only stored in a variable here for debugging purposes.
		
		this.destinationMAC = MacAddress.of(bb.getLong());
		this.sourceMAC = MacAddress.of(bb.getLong());
		
		this.version = bb.get();
		this.maxHops = bb.get();
		
		this.probeId = bb.getLong();
		
		this.hop = bb.get();
		
		this.timestamp = new Date(bb.getLong());
		
		byte req = bb.get();
		if(req == (byte)1)
			this.request = true;
		else
			this.request = false;
		
		this.swInfo = new SwitchInfo().deserialize(data, 37, SwitchInfo.len);
		
		return this;
	}

	/**
	 * Determines if the specified trace packet is equal to this object.
	 * 
	 * @param p2
	 * @return
	 */
	public boolean equals(TracePacket p2) {
		if(!destinationMAC.equals(p2.getDestination()))
			return false;
		else if(!sourceMAC.equals(p2.getSource()))
			return false;
		else if(version != p2.getVersion())
			return false;
		else if(maxHops != p2.getMaxHops())
			return false;
		else if(probeId != p2.getProbeId())
			return false;
		else if(hop != p2.getHop())
			return false;
		return true;
	}

	/**
	 * Convert this packet to a reply.
	 * 
	 * @return
	 */
	public TracePacket ConvertToReply() {
		if(request == false)
			return null; 
		
		request = false;
		MacAddress temp = sourceMAC;
		sourceMAC = destinationMAC;
		destinationMAC = temp;
		
		return this;
	}
	
	@Override
	public String toString(){
		String ret = "Source: " + sourceMAC.toString() +
				"\nDestingation: " + destinationMAC.toString() +
				"\nprobeId: " + probeId +
				"\nHop: " + Byte.toString(hop) +
				"\nRequest: " + Boolean.toString(request);
		return ret;
	}

	public boolean isRequest() {
		return request;
	}

	@Override
	public int compareTo(TracePacket o) {
		if(this.hop < o.hop)
			return -1;
		else if(this.hop == o.hop)
			return 0;
		else
			return 1;
	}

}
