package traceapp.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import traceapp.core.Hop;

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
public class TracePacket extends BasePacket{

    private MacAddress destinationMAC; // 64 bits = 8 bytes
    private MacAddress sourceMAC; // 64 bits = 8 bytes
    private IpProtocol protocol; // 1 byte
    private byte version;
    private byte maxHops;
    private List<Hop> hops;
    private OFPort ingress;

    /**
     * The ethernet type for trace packets
     */
    protected static final int TRACE_REQUEST = 0x8220;
    
    /**
     * Indicates whether the packet is a request a reply. True for request, false for reply.
     */
    private boolean request; 
	
    public TracePacket(){
    	super();
    	version = 1;
    	request = true;
    	hops = new ArrayList<Hop>();
    }
    
    // Define various setters and getters //
    
    public void appendHop(Hop hop){
    	hops.add(hop);
    }
    public void setDestination(MacAddress dstMac){
    	destinationMAC = dstMac;
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
    
    public void setProtocol(IpProtocol p){
    	protocol = p;
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
    
    public List<Hop> getHops(){
    	return hops;
    }
    
    public IpProtocol getProtocol(){
    	return protocol;
    }
    
    public byte getVersion(){
    	return version;
    }
    
    /**
     * Convert this TracePacket into raw data that is ready to be sent across the network.
     */
	@Override
	public byte[] serialize() {
		
		// Compute the length of the packet in bytes
		int length = 21;
		if(hops != null)
			length += (hops.size() * 8);
		
        byte[] data = new byte[length];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put((byte)length);
        bb.putLong(destinationMAC.getLong()); // 8 bytes
        bb.putLong(sourceMAC.getLong()); // 8 bytes
        bb.put(version); // 1 byte
        bb.put(maxHops); // 1 byte
        
        if(request) // 1 byte
        	bb.put((byte)1);
        else
        	bb.put((byte)0);
       
        // Need to transmit the number of hop entries as well
	    bb.put((byte)hops.size()); // 4 bytes
	        
	    for(Hop h : hops){
	        bb.putLong(h.getDpid());
	    }
      
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
	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		
		int len = bb.get(); // Length is used elsewhere in the program. It's only stored in a variable here for debugging purposes.
		this.destinationMAC = MacAddress.of(bb.getLong());
		this.sourceMAC = MacAddress.of(bb.getLong());
		
		this.version = bb.get();
		this.maxHops = bb.get();
		
		byte req = bb.get();
		if(req == (byte)1)
			this.request = true;
		else
			this.request = false;
		
		byte hopLength = bb.get();
		
		for(int i = 0; i < hopLength; i++){
			long dpid = bb.getLong();
			hops.add(new Hop(dpid));
		}
		
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
		else if(!protocol.equals(p2.getProtocol()))
			return false;
		else if(version != p2.getVersion())
			return false;
		else if(maxHops != p2.getMaxHops())
			return false;
		List<Hop> hops1 = p2.getHops();
		int i = 0;
		for(Hop h : hops){
			if(h.dpid != hops1.get(i).dpid)
				return false;
		}
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
		String ret = "Destination: " + destinationMAC.toString();
		ret += "\n Source: " + sourceMAC.toString();
		ret += "\nSwitch ID: ";
		
		if(hops != null)
			for(Hop h : hops){
				ret += h.toString() + "\n"; 
			}
		
		return ret;
	}

	public boolean isRequest() {
		return request;
	}

}
