package traceapp.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
		// TODO Do we really need two maps for TCP and UDP?
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
		network.AddFlow(dpid, -1, -1, TRACE_PACKET, 1000, "controller", null);
	}
	
	/**
	 * Control the trace process according to the SDNTrace specification.
	 */
	private void HandleTrace(Packet p, long dpid){
		System.out.println("TraceApp received a trace request...");
		
		// If the destination is directly attached to the present switch, we're on the last hop
		// If the destination is not directly attached to the present switch, append the hop info
		// 	to the data field of the packet, send the packet along to 
	    // 	the next hop as determined by this object's netMap.
		
		// TODO: How do we determine if the destination is directly attached?
		//	Could look at the number of nodes on each port, if there's more than one node on the destination's
		//	port, you know it's definitely not a direct attachment. If there's one node on a port though,
		//	you don't know for sure it's a direct attachment.
		// *If you build the map only on port status changes, your map for each switch will only 
		//  contain the nodes that are directly connected to that switch. However, this introduces
		//  a problem in figuring out where to send the packet next
		
		// If it's time to send the reply, look up the requester in the netMap and send the response
		// 	directly out the switch the requester is connected to, as opposed to sending the packet back
		//	down the chain.
		
		long dst = p.getDestination();
		
		List<Object>data = new ArrayList<Object>();
		Object hop = "DPID=" + Long.toString(dpid);
		for(int i = 0; i < p.getData().length; i++){
			if(p.getData()[i] == null)
				break;
			data.add(p.getData()[i]);
		}
		data.add(hop);
		
		Packet toSend = new Packet(dst, p.getDstIp(), p.getSourceIp(), p.getSourceMac(),
				p.getProtocol(), p.getEtherType(), data.toArray());
		
		HashMap<Long, Integer> swMap = netMap.get(p.getProtocol()).get(dpid);
		
		if(swMap.containsKey(dst)){ // If the node is directly connected to this switch
			// Trace complete. Return to sender.
			HashMap<Long, HashMap<Long, Integer>> map = netMap.get(p.getProtocol());
			long dp = searchForSwitch(p.getSource(), map);
			
			network.SendPacket(dp, p.getDestination(), p.getSource(), p.getDstIp(), 
					p.getSourceIp(), p.getProtocol(), 32, 2, TRACE_PACKET, data.toArray());
		}
		else{ // The node is not directly attached
			// Send probe back along its way
			network.SendPacket(dpid, p.getSource(), dst, p.getSourceIp(), 
					p.getDstIp(), p.getProtocol(), 32, 10, TRACE_PACKET, data.toArray());
		}
	}
	
	/**
	 * Find the dpid that the specified mac is on.
	 * 
	 * Return -1 if not present.
	 * 
	 * @param mac
	 * @param toSearch
	 * @return
	 */
	private long searchForSwitch(long mac, HashMap<Long, HashMap<Long, Integer>> toSearch){
		for(long l : toSearch.keySet()){
			HashMap<Long, Integer> m = toSearch.get(l);
			if(m.containsKey(mac))
				return l;
		}
		
		return -1;
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
			// TODO: This may not create a complete map depending on how the underlying OpenFlow learns.
			// TODO: Should be building the netMap on port status changes, as that is a more robust
			// 			method.
//		if(netMap.containsKey(pkt.getProtocol())){
//			HashMap<Long, HashMap<Long, Integer>> m = netMap.get(pkt.getProtocol());
//			if(netMap.containsKey(dpid)){
//				HashMap<Long, Integer> n = m.get(dpid);
//				if(!n.containsKey(mac)){
//					n.put(mac, port);
//				}
//			}
//			else{
//				HashMap<Long, Integer> toAdd = new HashMap<Long, Integer>();
//				toAdd.put(mac, port);
//				m.put(dpid, toAdd);
//			}
//		}
//		else{
//			HashMap<Long, Integer> toAdd1 = new HashMap<Long, Integer>();
//			toAdd1.put(mac, port);
//			HashMap<Long, HashMap<Long, Integer>> toAdd2 = new HashMap<Long, HashMap<Long, Integer>>();
//			toAdd2.put(dpid, toAdd1);
//			netMap.put(pkt.getProtocol(), toAdd2);
//		}
		
		
		if(pkt.getEtherType() == TRACE_PACKET){
			HandleTrace(pkt, dpid);
		}
	}
	
	/**
	 * To be called whenever OF Receives a PortStatusChanged message
	 * 
	 * Pass -1 for mac address to signal that there is no longer an address on this port
	 */
	public void OnPortStatusChange(long dpid, int portNum, long mac){
		HashMap<Long, HashMap<Long, Integer>> tcpMap = netMap.get("tcp");
		
		// Determine whether or not the port is already registered on the switch
			// If yes, update the mac address and return
			// If no, add a new entry with the port and the mac
		
		if(!tcpMap.containsKey(dpid)){
			HashMap<Long, Integer> newMap = new HashMap<Long, Integer>();
			newMap.put(mac, portNum);
			tcpMap.put(dpid, newMap);
		}
		else{
			HashMap<Long, Integer> map = tcpMap.get(dpid);
			if(!map.containsKey(mac))
				map.put(mac, portNum);
			else{
				map.remove(mac);
				map.put(mac, portNum);
			}
		}
	}
}