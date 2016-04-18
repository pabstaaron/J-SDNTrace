package traceapp.tests;

import traceapp.core.Packet;

/**
 * Represents a simple host that is capable of sending pings and trace packets
 * 
 * @author Aaron Pabst
 */
public class DummyHost implements INetNode, Comparable<DummyHost>{
	private long ip;
	private long mac;
	private Wire plugged;
	private String messages; // The equivalent of a console for messages to be dumped to.
	private final static int TRACE_REQUEST = 0x8820;
	
	public DummyHost(long ip, long mac){
		this.ip = ip;
		this.mac = mac;
		messages = "";
	}
	
	/**
	 * Send a simple ping packet
	 * 
	 * @param dstIp
	 * @return
	 */
	public void sendPingRequest(long dstIp){
		messages += "\nSending ping request to " + dstIp + "...";
		
		Packet p = new Packet(0, dstIp, ip, mac, "tcp", 8, new byte[32]);
		
		if(plugged == null){ // There's nowhere for the packet to go
			messages += "\nDestination unreachable";
			return;
		}
		
		if(this == plugged.left)
			plugged.right.packetIn(p, -1, -1);
		else 
			plugged.left.packetIn(p, -1, -1);
	}
	
	private void sendPingReply(long srcIp){
		Packet p = new Packet(0, srcIp, ip, mac, "tcp", 0, new byte[32]);
		
		if(plugged == null) // There's nowhere for the packet to go
			return;
		
		if(this == plugged.left)
			plugged.right.packetIn(p, -1, -1);
		else 
			plugged.left.packetIn(p, -1, -1);
	}
	
	/**
	 * Send a trace packet
	 * 
	 * @param protocol
	 * @param dstMac
	 * @return
	 */
	public Packet sendTrace(String protocol, long dstMac){
		Packet p = new Packet(dstMac, -1, ip, mac, "tcp", TRACE_REQUEST, new byte[32]);
		
		if(plugged == null) // There's nowhere for the packet to go
			return p;
		
		if(this == plugged.left)
			plugged.right.packetIn(p, -1, -1);
		else 
			plugged.left.packetIn(p, -1, -1);
		
		return p;
	}
	
	public void packetIn(Packet p, long dpid, int port)
	{
		if(p.getSourceIp() == ip)
			return;
		
		if(p.getEtherType() == 8){ // This packet is a ping request
			sendPingReply(p.getSourceIp());
		}
		else if(p.getEtherType() == 0){ // This packet is a ping reply
			messages += "\nPing reply received from: " + p.getSourceIp();
		}
		else if(p.getEtherType() == 0x8820){ // This packet is a trace reply
			messages += "\nReceived trace reply\n";
			// TODO The data field of the packet will contain the trace results
					// Not sure if its worth it to deserialize the data for testing
			messages += p.getData();
		}
	}

	@Override
	public String getType() {
		return "dummyhost";
	}

	@Override
	public void plug(Wire w) {
		plugged = w;
	}

	@Override
	public int compareTo(DummyHost o) {
		if(mac == o.mac)
			return 0;
		else if(mac > o.mac)
			return 1;
		else
			return -1;
	}
	
	public String getMessages(){
		return messages;
	}

	public long getIp() {
		return ip;
	}

	public long getMac() {
		return mac;
	}
}
