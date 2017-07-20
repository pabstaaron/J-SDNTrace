package core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.projectfloodlight.openflow.types.*;

/**
 * Bundles all info about a switch needed for a trace
 * 
 * @author Aaron Pabst
 *
 */
public class SwitchInfo {
	private DatapathId dpid; // The DPID of the switch
	private InetSocketAddress ipAddr; // The IPv4 address of the switch
	private U64 linkLatency; // The latency between the switch and the openflow controller, in ms
	private OFPort ingress;
	
	public static final int len = 24;
	
	public SwitchInfo(){
		dpid = DatapathId.NONE;
		ipAddr = new InetSocketAddress(0);
		linkLatency = U64.of(0);
		ingress = OFPort.ANY;
	}
	
	public DatapathId getDpid(){
		return dpid;
	}
	
	public InetSocketAddress getAddr(){
		return ipAddr;
	}
	
	public U64 getLatency(){
		return linkLatency;
	}
	
	public void setDpid(DatapathId id){
		dpid = id;
	}
	
	public void setIpAddr(InetSocketAddress addr){
		ipAddr = addr;
	}
	
	public void setLinkLatency(U64 latency){
		linkLatency = latency;
	}
	
	public void setIngress(OFPort ingress){
		this.ingress = ingress;
	}

	/**
	 * Packs this object into a transmissable format
	 * 
	 * @return byte[]
	 */
	public byte[] serialize(){
		
        byte[] data = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(data);
        
        bb.putLong(dpid.getLong()); // 8 bytes
        byte[] addr = ipAddr.getAddress().getAddress();
        bb.put(addr[0]); // 4 bytes
        bb.put(addr[1]);
        bb.put(addr[2]);
        bb.put(addr[3]);
        bb.putLong(linkLatency.getValue()); // 8 bytes
        bb.putInt(ingress.getPortNumber()); // 4 bytes
        
        return bb.array();
	}
	
	/**
	 * Translates a transmissable data array into a SwitchInfo object
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @return
	 */
	public SwitchInfo deserialize(byte[] data, int offset, int length){
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		
		dpid = DatapathId.of(bb.getLong());
		byte[] addr = new byte[4];
		addr[0] = bb.get();
		addr[1] = bb.get();
		addr[2] = bb.get();
		addr[3] = bb.get();
		linkLatency = U64.of(bb.getLong());
		ingress = OFPort.of(bb.getInt());
		
		return this;
	}
}
