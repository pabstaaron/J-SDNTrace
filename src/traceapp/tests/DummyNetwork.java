package traceapp.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import traceapp.core.INetwork;
import traceapp.core.Packet;

/**
 * Implements a mock network for testing purposes.
 * 
 * Keeps a mock flow table, defines ports, and generates mock traffic.
 * Provide
 * 
 * @author Aaron Pabst
 */
public class DummyNetwork implements INetwork {

	private List<DummySwitch> switches;
	private List<DummyHost> hosts;
	
	public DummyNetwork(){
		switches = new ArrayList<DummySwitch>();
		hosts = new ArrayList<DummyHost>();
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
			int portNum) {
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(dpid, 0, 0, 0));
		if(i < 0)
			return;
		
		DummySwitch sw = switches.get(i);
		
		sw.packetIn(new Packet(dstMac, dstIP, srcIP, srcMac, proto, i), sw.getPort(portNum));
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
		return sw;
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

	
}
