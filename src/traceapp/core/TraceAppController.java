package traceapp.core;

import java.util.HashMap;

/**
 * Implements the SDNTrace protocol.
 * 
 * @author Aaron Pabst
 */
public class TraceAppController {
	
	// A reference back to the underlying infrastructure.
	// Used to send packets and push flows.
	private INetwork network;
	
	// The Ethernet type used to define trace packets
	private static final int TRACE_PACKET = 0x8820;
	
	// Used for keeping track of what mac address belongs to what port
	// keys on protocol (tcp/upd), DPID, and MAC with a port number for a value
		// TODO What if MAC is on multiple ports on the same DPID?
	private HashMap<String, HashMap<Long, HashMap<Long, Integer>>> netMap;
	
	private String debugMessages; // Used to store and retrieve debugging info.
	
	/**
	 * Starts up a new instance of TraceApp to control the specified INetwork.
	 * 
	 * @param network
	 */
	public TraceAppController(INetwork network){
		this.network = network;
		
		netMap = new HashMap<String, HashMap<Long, HashMap<Long, Integer>>>();
		
		debugMessages = "TraceApp initilized....\n";
	}
	
	/**
	 * Used to notify the App that a new switch has been connected
	 * 
	 * @param dpid
	 */
	public void SwitchInfoReceived(Long dpid){
		// Add the new switch to the map
		if(netMap.size() == 0){
			HashMap<Long, HashMap<Long, Integer>> m1 = new HashMap<Long, HashMap<Long, Integer>>();
			m1.put(dpid, new HashMap<Long, Integer>());
			netMap.put("tcp", m1);
			
			HashMap<Long, HashMap<Long, Integer>> m2 = new HashMap<Long, HashMap<Long, Integer>>();
			m2.put(dpid, new HashMap<Long, Integer>());
			netMap.put("udp", m2);
		}
		else{
			HashMap<Long, HashMap<Long, Integer>> m1 = netMap.get("tcp");
			m1.put(dpid, new HashMap<Long, Integer>());
			
			HashMap<Long, HashMap<Long, Integer>> m2 = netMap.get("udp");
			m2.put(dpid, new HashMap<Long, Integer>());
		}
		
		// Push the Trace flow to the new switch
			// TODO Need to know the port number that the controller is on
		network.AddFlow(dpid, -1, -1, TRACE_PACKET, 1000, -1, null);
	}
	/**
	 * Control the trace process according to the SDNTrace specification.
	 */
	private void HandleTrace(Packet p){
		System.out.println("TraceApp received a trace request...");
	}
	
	/**
	 * Used to notify this object of a packet in. 
	 * Should be called on ALL packetIn messages.
	 * 
	 * @param pkt - A network packet as defined in traceapp.core
	 * @param dpid - The DPID of the switch the message was generated on
	 * @param port - The port the packet came in on. For learning purposes.
	 */
	public void PacketIn(Packet pkt, long dpid, int port){
		System.out.println("TraceApp received a packet: " + pkt);
		long mac = pkt.getSource();
		
		// Register the mac address and port if necessary.
			// This may not create a complete map depending on how the underlying OpenFlow learns.
		if(netMap.containsKey(pkt.getProtocol())){
			HashMap<Long, HashMap<Long, Integer>> m = netMap.get(pkt.getProtocol());
			if(netMap.containsKey(dpid)){
				HashMap<Long, Integer> n = m.get(dpid);
				if(!n.containsKey(mac)){
					n.put(mac, port);
				}
			}
			else{
				HashMap<Long, Integer> toAdd = new HashMap<Long, Integer>();
				toAdd.put(mac, port);
				m.put(dpid, toAdd);
			}
		}
		else{
			HashMap<Long, Integer> toAdd1 = new HashMap<Long, Integer>();
			toAdd1.put(mac, port);
			HashMap<Long, HashMap<Long, Integer>> toAdd2 = new HashMap<Long, HashMap<Long, Integer>>();
			toAdd2.put(dpid, toAdd1);
			netMap.put(pkt.getProtocol(), toAdd2);
		}
		
		
		if(pkt.getEtherType() == TRACE_PACKET){
			HandleTrace(pkt);
		}
	}
}