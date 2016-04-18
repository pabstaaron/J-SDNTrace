package traceapp.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import traceapp.core.INetwork;
import traceapp.core.Packet;
import traceapp.core.TraceAppController;

/**
 * Implements a mock network for testing purposes.
 * 
 * This class serves the role of an OpenFlow controller in the test harness. As such, it has an
 * ip address and a mac address. It connects to a switch in a UnitTest in the same manner as a host.
 * 
 * @author Aaron Pabst
 */
public class DummyNetwork implements INetwork, INetNode {

	private List<DummySwitch> switches;
	private List<DummyHost> hosts;
	private long ip, mac;
	private Wire plugged;
	private TraceAppController cont;
	private final static int TRACE_REQUEST = 0x8820;
	private String messages;
	
	public DummyNetwork(long ip, long mac){
		switches = new ArrayList<DummySwitch>();
		hosts = new ArrayList<DummyHost>();
		cont = new TraceAppController(this);
		messages = "";
	}
	
	public int size(){
		return switches.size() + hosts.size();
	}
	
	@Override
	public void AddFlow(long dpid, int inPort, long dstAddress, int ethType, int priority, int outPort, String proto) {
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(dpid, 0, 0, 0));
		if(i < 0)
			return;
		
		DummySwitch sw = switches.get(i);
		Match m = new Match(inPort, -1, dstAddress, ethType, proto);
		sw.AddFlow(m, outPort, priority);
	}

	
	@Override
	public void SendPacket(long dpid, long srcMac, long dstMac, long srcIP, long dstIP, String proto, int TTL,
			int portNum, byte[] data) {
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(dpid, 0, 0, 0));
		if(i < 0)
			return;
		
		DummySwitch sw = switches.get(i);
		
		sw.packetIn(new Packet(dstMac, dstIP, srcIP, srcMac, proto, i, data), sw.getPort(portNum));
	}

	/**
	 * Add a new switch to the network
	 * 
	 * @param dpid
	 * @param numPorts
	 * @param mac
	 * @param ipV4
	 */
	public DummySwitch AddSwitch(long dpid, int numPorts, long mac, long ipV4){
		DummySwitch sw = new DummySwitch(dpid, numPorts, mac, ipV4);
		switches.add(sw);
		cont.SwitchInfoReceived(dpid);
		return sw;
	}
	
	/**
	 * Pull a random switch from the network.
	 * 
	 * @param seed
	 * @return
	 */
	public DummySwitch randomSwitch(int seed){
		Random rand = new Random(seed);
		int i = rand.nextInt(switches.size());
		return switches.get(i);
	}
	
	/**
	 * Add a new host to the network
	 * 
	 * @param ip
	 * @param maclist
	 */
	public DummyHost AddHost(long ip, long mac){
		DummyHost h = new DummyHost(ip, mac);
		hosts.add(h);
		return h;
	}
	
	/**
	 * Retrieves the host with the provided mac address. 
	 * 
	 * Throws IllegalArgumentException if the mac address is not present in this network
	 * 
	 * @param mac
	 * @return
	 */
	public DummyHost getHost(long mac){
		Collections.sort(hosts);
		int i = Collections.binarySearch(hosts, new DummyHost(0, mac));
		if(i < 0)
			throw new IllegalArgumentException("The mac " + mac + " is not a current address in the network");
		return hosts.get(i);
	}
	
	/**
	 * Retrieves the switch with the provided mac address. 
	 * 
	 * Throws IllegalArgumentException if the mac address is not present in this network
	 * 
	 * @param mac
	 * @return
	 */
	public DummySwitch getSwitch(long mac){
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(0, 0, mac, 0));
		return switches.get(i);
	}

	@Override
	public String getType() {
		return "dummynetwork";
	}

	@Override
	public void plug(Wire w) {
		plugged = w;
	}

	@Override
	public void packetIn(Packet p, long dpid, int port) {
		if(p.getEtherType() == 8 && p.getDstIp() == mac){ // A ping request sent to this object
			
		}
		else if(p.getEtherType() == TRACE_REQUEST){ // This is a trace packet
			messages += "\nReceived trace packet";
		}
		
		cont.PacketIn(p, dpid, port);
	}

	public String getMessages(){
		return messages;
	}

	public int getControllerPort(long dpid) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int numSwitches() {
		return switches.size();
	}
}
