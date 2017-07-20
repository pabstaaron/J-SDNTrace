package core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
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
	// keys on DPID and MAC with a port number for a value
	private HashMap<Long, HashMap<Long, Integer>> netMap;
	
	// Keeps a record of the current hop number for a probe
	// Maps a probe id to a hop count
	private HashMap<Long, Byte> hopRec;
	
	/**
	 * Starts up a new instance of TraceApp to control the specified INetwork.
	 * 
	 * @param network
	 */
	public TraceAppController(INetwork network){
		this.network = network;
		
		netMap = new HashMap<Long, HashMap<Long, Integer>>();
		
		hopRec = new HashMap<Long, Byte>(); 
	}
	
	/**
	 * Writes the present network map out to a text file.
	 */
	private void writeMapToFile(){
		String s = "--TraceApp Map Info--\n";
		//HashMap<Long, HashMap<Long, Integer>> map = netMap.get("tcp");
		
		for(Long id : netMap.keySet()){
			s += "Switch: " + Long.toString(id) + "\n";
			for(Long mac : netMap.get(id).keySet()){
				s+="\t" + Long.toString(mac) + " : " + Integer.toString(netMap.get(id).get(mac)) + "\n";
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
			netMap.put(dpid, portMap);
		}
		else{
			netMap.put(dpid, portMap);
		}
		
		// Push the Trace flow to the new switch
		network.AddFlow(dpid, network.buildTraceMatch(dpid), OFPort.CONTROLLER);
		writeMapToFile();
	}
	
	/**
	 * Control the trace process according to the SDNTrace specification.
	 * @param swAddr 
	 */
	private void HandleTrace(TracePacket p, SwitchInfo info){
		System.out.println("TraceApp received a trace request...");
		
		HashMap<Long, Integer> sw = netMap.get(info.getDpid());
	
		/**
		 * Try to find the switch and port to send the reply out of. If the switch and port are not present in the map,
		 * Flood the reply out all ports on the switch the request came in on.
		 * 
		 * FIXME - This is a thousand miles short of ideal/robust...
		 */
		long returnSw = searchForSwitch(p.getSource().getLong(), netMap);
		
		try{
			int returnPort = netMap.get(returnSw).get(p.getSource());
			System.out.println("Sending packet out sw: " + Long.toString(returnSw) + " on port: " + Integer.toString(returnPort));
			sendReply(returnSw, p, OFPort.of(returnPort), info);
		}catch(NullPointerException e){
			System.out.println("Sending packet out all ports on sw: " + info.getDpid());
			sendReply(info.getDpid().getLong(), p, OFPort.FLOOD, info);
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
	private void sendReply(long dpid, TracePacket request, OFPort p, SwitchInfo info) {
		TracePacket reply = new TracePacket();
		reply.setDestination(request.getSource());
		reply.setSource(request.getDestination());
		reply.setMaxHops((byte)255);
		reply.setType(false); // Toggle packet type to reply
		reply.setTimestamp(new Date());
		reply.setSwInfo(info);
		
		if(!hopRec.containsKey(request.getProbeId())){ // If we haven't seen this probeId before
			hopRec.put(request.getProbeId(), (byte)0);
		}
		reply.setHop(hopRec.get(request.getProbeId()));
		hopRec.put(request.getProbeId(), (byte)(hopRec.get(request.getProbeId()) + 1)); // Increment the hop counter
		
		network.SendTracePacket(dpid, reply, p); // Blast out the reply
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
	 * Used to notify this TraceAppController of a packet in. 
	 * TODO - Should be called on ALL packetIn messages.
	 * 
	 * @param pkt - A network packet as defined in traceapp.core
	 * @param dpid - The DPID of the switch the message was generated on
	 * @param ingress - The port the packet came in on. 
	 */
	public void PacketIn(byte[] data, SwitchInfo info){
//		System.out.println("TraceApp received a packet: " + pkt);
//		HandleTrace(pkt, dpid, ingress);
		
		TracePacket pkt = new TracePacket();
		
		try{
			pkt.deserialize(data, 0, data[0]);
			HandleTrace(pkt, info);
		}catch(PacketParsingException e){
			System.err.println("ERROR: Trace packet failed to deserialize!");
			e.printStackTrace();
		}
	}
	
	/**
	 * To be called whenever OF Receives a PortStatusChanged message
	 * 
	 * Pass -1 for mac address to signal that there is no longer an address on this port
	 */
	public void OnPortStatusChange(long dpid, int portNum, long mac){
		
		// Determine whether or not the port is already registered on the switch
			// If yes, update the mac address and return
			// If no, add a new entry with the port and the mac
		
		if(!netMap.containsKey(dpid)){
			HashMap<Long, Integer> newMap = new HashMap<Long, Integer>();
			newMap.put(mac, portNum);
			netMap.put(dpid, newMap);
		}
		else{
			HashMap<Long, Integer> map = netMap.get(dpid);
			if(!map.containsKey(mac))
				map.put(mac, portNum);
			else{
				map.remove(mac);
				map.put(mac, portNum);
			}
		}
		writeMapToFile();
	}
	
	
}

