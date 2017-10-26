package core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
	private String contId; 
	
	public static final int len = 28;
	
	public SwitchInfo(){
		dpid = DatapathId.NONE;
		ipAddr = new InetSocketAddress(0);
		linkLatency = U64.of(0);
		ingress = OFPort.ANY;
		contId = "";
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
	
	public String getContId(){
		return contId;
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
	
	public void setContId(String swId){
		this.contId = swId;
	}

	/**
	 * Packs this object into a transmissable format
	 * 
	 * @return byte[]
	 */
	public byte[] serialize(){
		
        byte[] data = new byte[len + contId.length()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        
        bb.putLong(dpid.getLong()); // 8 bytes
        byte[] addr = ipAddr.getAddress().getAddress();
        bb.put(addr[0]); // 4 bytes
        bb.put(addr[1]);
        bb.put(addr[2]);
        bb.put(addr[3]);
        bb.putLong(linkLatency.getValue()); // 8 bytes
        bb.putInt(ingress.getPortNumber()); // 4 bytes
        
        bb.putInt(contId.length());
        bb.put(contId.getBytes());
        
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
		try {
			ipAddr = new InetSocketAddress(InetAddress.getByAddress(addr), 0);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		linkLatency = U64.of(bb.getLong());
		ingress = OFPort.of(bb.getInt());
		
		int len = bb.getInt();
//		contId = "";
//		for(int i = 0; i < len; i++){
//			contId = contId + (char)bb.get();
//		}
		
		return this;
	}
	
	public boolean equals(SwitchInfo b){
		if(!this.dpid.equals(b.getDpid()))
			return false;
		else if(this.ingress.getPortNumber() != b.getIngress().getPortNumber())
			return false;
		else if(!this.ipAddr.equals(b.getAddr()))
			return false;
		else if(!this.linkLatency.equals(b.getLatency()))
			return false;
		else if(!this.contId.equals(b.getContId()))
			return false;
		
		return true;
	}

	public OFPort getIngress() {
		// TODO Auto-generated method stub
		return ingress;
	}
}
