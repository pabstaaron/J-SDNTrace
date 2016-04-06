package traceapp.core;

import java.util.HashMap;

public class TraceAppController {
	
	// A reference back to the underlying infrastructure.
	// Used to send packets and push flows.
	private INetwork network;
	
	// The Ethernet type used to define trace packets
	private final int traceEtherType = 0x8820;
	
	// Used for keeping track of what mac address belongs to what port
	// keys on protocol (tcp/upd), DPID, and MAC with a port number for a value
		// TODO What if MAC is on multiple ports on the same DPID?
	private HashMap<String, HashMap<Long, HashMap<Long, Integer>>> netMap;
	
	/**
	 * Starts up a new instance of TraceApp to control the specified INetwork.
	 * 
	 * @param network
	 */
	public TraceAppController(INetwork network){
		this.network = network;
		
		// Tell network what a trace packet is
		
		// Setup flow for trace packets
		
		// Initialize network map
	}
	
	/**
	 * Control the trace process according to SDNTrace specification.
	 */
	private void HandleTrace(){
		// TODO Unimplemented
	}
	
	// TODO We're going to also need to know the in-port and DPID the packet came in on.
		// Bundled in the packet object or passed separately?
	public void PacketIn(Packet pkt, long dpid, int port){
		System.out.println("TraceApp received a packet: " + pkt);
		// TODO Unimplemented
	}
}