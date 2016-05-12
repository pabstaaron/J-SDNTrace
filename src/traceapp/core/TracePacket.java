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
    
    protected MacAddress destinationMAC; // 32 bits = 4 bytes
    protected MacAddress sourceMAC; // 32 bits = 4 bytes
    protected IpProtocol protocol; // 1 byte
    protected IPacket payload; // TCP or UDP payload
    protected byte version;
    protected byte headerLength;
    protected byte ttl;
    protected List<Hop> hops;
    
    /**
     * Indicates whether the packet is a request a reply. True for request, false for reply.
     */
    protected boolean request; 
	
    public TracePacket(){
    	super();
    	version = 1;
    }
    
    // Define various setters and getters //
    
    public void appendHop(Hop hop){
    	if(hops == null)
    		hops = new ArrayList<Hop>();
    	
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
    
    public MacAddress getDestination(){
    	return destinationMAC;
    }
    
    public MacAddress getSource(){
    	return sourceMAC;
    }
    
    public byte getTTL(){
    	return ttl;
    }
    
    public byte getHeaderLength(){
    	return headerLength;
    }
    
    public boolean getType(){
    	return request;
    }
    
    public List<Hop> getHops(){
    	return hops;
    }
    
	@Override
	public byte[] serialize() {
		// TODO Auto-generated method stub
		
		int length = 13 + (hops.size() * 4);
		
		byte[] payloadData = null;
        if (payload != null) {
            payload.setParent(this);
            payloadData = payload.serialize();
            length += payloadData.length + 2;
        }
		
        
        byte[] data = new byte[length];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putLong(destinationMAC.getLong());
        bb.putLong(sourceMAC.getLong());
        if(protocol.equals(IpProtocol.TCP))
        	bb.put((byte)0);
        else
        	bb.put((byte)1);
        bb.put(version);
        bb.put(ttl);
        
        if(payloadData != null){
        	bb.putInt(payloadData.length);
        	bb.put(payloadData);
        }
        else
        	bb.putInt(0);
        
        // Need to transmit the number of hop entries as well
        bb.putInt(hops.size());
        
        for(Hop h : hops){
        	bb.putLong(h.getDpid());
        }
        
		return data;
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		// TODO Auto-generated method stub
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		
		// Pull data out in the same order you put it in
		
		this.destinationMAC = MacAddress.of(bb.getLong());
		this.sourceMAC = MacAddress.of(bb.getLong());
		
		byte proto = bb.get();
		if(proto == 0)
			this.protocol = IpProtocol.TCP;
		else
			this.protocol = IpProtocol.UDP;
		this.version = bb.get();
		this.ttl = bb.get();
	
		int payloadLength = bb.getInt();
		
		if(payloadLength > 0){
			if(protocol.equals(IpProtocol.TCP))
				payload = new TCP();
			else
				payload = new UDP();
			
			payload = payload.deserialize(data, bb.position(), payloadLength);
		}
		
		int hopLength = bb.getInt();
		
		for(int i = 0; i < hopLength; i++){
			if(hops == null)
				hops = new ArrayList<Hop>();
			long dpid = bb.getLong();
			hops.add(new Hop(dpid));
		}
		
		return this;
	}

}
