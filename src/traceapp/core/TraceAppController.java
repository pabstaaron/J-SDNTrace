package traceapp.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.packet.PacketParsingException;

/**
 * Implements the SDNTrace protocol.
 * 
 * This class should be able to be integrated into most any OpenFlow controller.
 * 
 * @author Aaron Pabst
 */
public class TraceAppController {
	
	// A reference back to the underlying infrastructure.
	// Used to send packets and push flows.
	private INetwork network;
	
	// The Ethernet type used to define trace packets
	private static final int TRACE_PACKET = 0x8220;
	
	// Used for keeping track of what mac address belongs to what port
	// keys on protocol (tcp/upd), DPID, and MAC with a port number for a value
	private HashMap<String, HashMap<Long, HashMap<Long, Integer>>> netMap;
	
	
	/**
	 * Starts up a new instance of TraceApp to control the specified INetwork.
	 * 
	 * @param network
	 */
	public TraceAppController(INetwork network){
		this.network = network;
		
		netMap = new HashMap<String, HashMap<Long, HashMap<Long, Integer>>>();
	}
	
	/**
	 * Writes the present network map out to a text file.
	 */
	private void writeMapToFile(){
		String s = "--TraceApp Map Info--\n";
		HashMap<Long, HashMap<Long, Integer>> map = netMap.get("tcp");
		for(Long id : map.keySet()){
			s += "Switch: " + Long.toString(id) + "\n";
			for(Long mac : map.get(id).keySet()){
				s+="\t" + Long.toString(mac) + " : " + Integer.toString(map.get(id).get(mac)) + "\n";
			}
		}
		
		try {
			FileWriter out = new FileWriter("TraceMap.txt");
			out.write(s);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Used to notify the App that a new switch has been connected
	 * 
	 * @param dpid - The id for the switch
	 * @param mp - The initial state of the ports on the switch
	 */
	public void SwitchInfoReceived(Long dpid, Collection<PortMacPair> mp){
		// Add the new switch to the map
		HashMap<Long, Integer> portMap = new HashMap<Long, Integer>();
		for(PortMacPair p: mp){
			portMap.put(p.getMac(), p.getPort());
		}
		if(netMap.size() == 0){
			HashMap<Long, HashMap<Long, Integer>> m1 = new HashMap<Long, HashMap<Long, Integer>>();
			m1.put(dpid, portMap);
			netMap.put("tcp", m1);
			
			HashMap<Long, HashMap<Long, Integer>> m2 = new HashMap<Long, HashMap<Long, Integer>>();
			m2.put(dpid, portMap);
			netMap.put("udp", m2);
		}
		else{
			HashMap<Long, HashMap<Long, Integer>> m1 = netMap.get("tcp");
			m1.put(dpid, portMap);
			
			HashMap<Long, HashMap<Long, Integer>> m2 = netMap.get("udp");
			m2.put(dpid, portMap);
		}
		
		// Push the Trace flow to the new switch
		network.AddFlow(dpid, network.buildTraceMatch(dpid), OFPort.CONTROLLER);
		writeMapToFile();
	}
	
	/**
	 * Control the trace process according to the SDNTrace specification.
	 */
	private void HandleTrace(TracePacket p, long dpid, OFPort ingress){
		System.out.println("TraceApp received a trace request...");
		
		HashMap<Long, HashMap<Long, Integer>> switches = netMap.get("tcp");
		
		HashMap<Long, Integer> sw = switches.get(dpid);
	
		/**
		 * Try to find the switch and port to send the reply out of. If the switch and port are not present in the map,
		 * Flood the reply out all ports on the switch the request came in on.
		 * 
		 * FIXME - This is a thousand miles short of ideal/robust...
		 */
		long returnSw = searchForSwitch(p.getSource().getLong(), switches);
		
		
		
		Hop h = new Hop(dpid);
		h.setTimestamp(new Date());
		h.setIngress(ingress);
		try{
			int returnPort = switches.get(returnSw).get(p.getSource());
			System.out.println("Sending packet out sw: " + Long.toString(returnSw) + " on port: " + Integer.toString(returnPort));
			sendReply(h, returnSw, p, OFPort.of(returnPort));
		}catch(NullPointerException e){
			System.out.println("Sending packet out all ports on sw: " + Long.toString(dpid));
			sendReply(h, dpid, p, OFPort.FLOOD);
		}
	}
	
	/**
	 * Converts a request packet into a reply and instructs the network to send the reply
	 * back to the originator.
	 * 
	 * @param h
	 * @param dpid
	 * @param request
	 * @param p
	 */
	private void sendReply(Hop h, long dpid, TracePacket request, OFPort p) {
		TracePacket reply = new TracePacket();
		reply.setDestination(request.getSource());
		reply.setSource(request.getDestination());
		reply.setMaxHops((byte)255);
		reply.setProtocol(IpProtocol.TCP); // The protocol field is still a part of the trace packet, but it's meaningless at this point.
		reply.setType(false); // Toggle packet type to request
		reply.appendHop(h); 
		
		network.SendTracePacket(dpid, reply, p);
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
	 * TODO - Should be called on ALL packetIn messages.
	 * 
	 * @param pkt - A network packet as defined in traceapp.core
	 * @param dpid - The DPID of the switch the message was generated on
	 * @param ingress - The port the packet came in on. 
	 */
	public void PacketIn(byte[] data, long dpid, OFPort ingress){
//		System.out.println("TraceApp received a packet: " + pkt);
//		HandleTrace(pkt, dpid, ingress);
		
		TracePacket pkt = new TracePacket();
		
		try{
			pkt.deserialize(data, 14, data[14]);
			HandleTrace(pkt, dpid, ingress);
		}catch(Exception e){
			
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
		writeMapToFile();
	}
	
	/**
	 * Wrapper class for associating ports with mac addresses.
	 */
	public class PortMacPair{
		private long mac;
		private int port;
		
		public void setMac(long mac){
			this.mac = mac;
		}
		
		public void setPort(int port){
			this.port = port;
		}
		
		public long getMac(){
			return mac;
		}
		
		public int getPort(){
			return port;
		}
	}
}

