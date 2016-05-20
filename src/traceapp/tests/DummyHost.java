package traceapp.tests;

import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.TCP;
import traceapp.core.Packet;
import traceapp.core.TracePacket;

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
		
		PingPacket p = new PingPacket((int)ip, (int)dstIp);
		
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
		PingReply p = new PingReply((int)ip, (int)srcIp);
		
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
	public TracePacket sendTrace(String protocol, long dstMac){
		TracePacket p = new TracePacket();
		p.setDestination(MacAddress.of(dstMac));
		p.setSource(MacAddress.of(mac));
		
		if(protocol == "tcp")
			p.setProtocol(IpProtocol.TCP);
		else
			p.setProtocol(IpProtocol.UDP);
		
		p.setTTL((byte)255);
		p.setType(true);
		TCP tcp = new TCP();
		tcp.setDestinationPort(8080);
		tcp.setSourcePort(8081);
		p.setPayload(tcp);
		
		if(plugged == null) // There's nowhere for the packet to go
			return p;
		
		if(this == plugged.left)
			plugged.right.packetIn(p, -1, -1);
		else 
			plugged.left.packetIn(p, -1, -1);
		
		return p;
	}
	
	public void packetIn(IPacket p, long dpid, int port)
	{
//		if(p instanceof TracePacket && ((TracePacket)p).get)
//			return;
		
		if(p instanceof PingPacket){ // This packet is a ping request
			if(((PingPacket)p).getDest() == this.ip)
				sendPingReply(((PingPacket)p).getSource());
		}
		else if(p instanceof PingReply){ // This packet is a ping reply
			messages += "\nPing reply received from: " + ((PingReply) p).getSource();
		}
		else if(p instanceof TracePacket){ // This packet is a trace reply
			messages += "\nReceived trace reply\n";
			// TODO The data field of the packet will contain the trace results
					// Not sure if its worth it to deserialize the data for testing
			messages += ((TracePacket)p).getHops().toString();
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
