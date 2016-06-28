package traceapp.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import traceapp.core.Hop;

public class TracePacket extends BasePacket{

	public static Map<IpProtocol, Class<? extends IPacket>> protocolClassMap;

    static {
        protocolClassMap = new HashMap<IpProtocol, Class<? extends IPacket>>();
        protocolClassMap.put(IpProtocol.TCP, TCP.class);
        protocolClassMap.put(IpProtocol.UDP, UDP.class);
    }
    
    private MacAddress destinationMAC; // 64 bits = 8 bytes
    private MacAddress sourceMAC; // 64 bits = 8 bytes
    private IpProtocol protocol; // 1 byte
    //private IPacket payload; // TCP or UDP payload
    private byte version;
    private byte headerLength;
    private byte ttl;
    private List<Hop> hops;
    
    protected static final int TRACE_REQUEST = 0x8220;
    
    protected static final int TRACE_REPLY = 0x8221;
    
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
    
    public void setTTL(byte ttl){
    	this.ttl = ttl;
    }
    
    public void setHeaderLength(byte headerLength){
    	this.headerLength = headerLength;
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
    
    public byte getTTL(){
    	return ttl;
    }
    
//    public byte getHeaderLength(){
//    	return headerLengthpayload;
//    }
    
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
    
	@Override
	public byte[] serialize() {
		// TODO Auto-generated method stub
		
		int length = 21;
		
		if(hops != null)
			length += (hops.size() * 8);
		
        byte[] data = new byte[length];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put((byte)length);
        bb.putLong(destinationMAC.getLong()); // 8 bytes
        bb.putLong(sourceMAC.getLong()); // 8 bytes
        bb.put(version); // 1 byte
        bb.put(ttl); // 1 byte
        
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

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		// TODO Auto-generated method stub
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		
		int len = bb.get(); // XXX - Length is used elsewhere in the program. It's only stored in a variable here for debugging.
		this.destinationMAC = MacAddress.of(bb.getLong());
		this.sourceMAC = MacAddress.of(bb.getLong());
		
		this.version = bb.get();
		this.ttl = bb.get();
		
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

	public boolean equals(TracePacket p2) {
		if(!destinationMAC.equals(p2.getDestination()))
			return false;
		else if(!sourceMAC.equals(p2.getSource()))
			return false;
		else if(!protocol.equals(p2.getProtocol()))
			return false;
		else if(version != p2.getVersion())
			return false;
		else if(ttl != p2.getTTL())
			return false;
//		else if(!payload.equals(p2.getPayload()))
//			return false;
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
		// TODO Auto-generated method stub
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
		ret += "\nHops:";
		
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
