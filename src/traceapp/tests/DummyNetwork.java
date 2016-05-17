package traceapp.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.floodlightcontroller.packet.IPacket;
import traceapp.core.INetwork;
import traceapp.core.Packet;
import traceapp.core.TraceAppController;
import traceapp.core.TracePacket;

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
	private List<Wire> plugged;
	private TraceAppController cont;
	private final static int TRACE_REQUEST = 0x8820;
	private final static int PORT_STATUS = 10;
	private String messages;
	
	public DummyNetwork(long ip, long mac){
		switches = new ArrayList<DummySwitch>();
		hosts = new ArrayList<DummyHost>();
		cont = new TraceAppController(this);
		plugged = new ArrayList<Wire>();
		messages = "";
	}
	
	public int size(){
		return switches.size() + hosts.size();
	}
	
	public TraceAppController getCont(){
		return cont;
	}
	
	@Override
	public void AddFlow(long dpid, int inPort, long dstAddress, int ethType, int priority, String action, String proto) {
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(dpid, 0, 0, 0));
		if(i < 0)
			return;
		
		DummySwitch sw = switches.get(i);
		Match m = new Match(inPort, -1, dstAddress, ethType, proto);
		if(action == "controller"){
			// Loop through all of the connections to this object until the switch is found
			for(Wire w : plugged){
				if(w.left == this){
					if(((Port)w.right).getParent().getDpid() == dpid){ // Found it 
						sw.AddFlow(m, ((Port)w.right).getNumber(), priority);
						return;
					}
				}
				else{
					if(((Port)w.left).getParent().getDpid() == dpid){ // Found it 
						sw.AddFlow(m, ((Port)w.left).getNumber(), priority);
						return;
					}
				}
			}
		}
		else
			sw.AddFlow(m, Integer.parseInt(action), priority);
	}

	
	@Override
	public void SendPacket(long dpid, IPacket pkt) {
		Collections.sort(switches);
		int i = Collections.binarySearch(switches, new DummySwitch(dpid, 0, 0, 0));
		if(i < 0)
			return;
		
		DummySwitch sw = switches.get(i);
		
		sw.packetInWoController(pkt, sw.getPort(1));
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
		Wire w = new Wire();
		w.left = this;
		w.right = sw.plug(1, w, true);
		plug(w);
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
		plugged.add(w);
	}

	@Override
	public void packetIn(IPacket p, long dpid, int port) {
		if(p instanceof PingPacket && ((PingPacket)p).getDest() == mac){ // A ping request sent to this object
			
		}
		else if(p instanceof PortStatus){
			PortStatus ps = (PortStatus)p;
			cont.OnPortStatusChange(ps.getDpid(), ps.getPort(), ps.getMac());
			return;
		}
		else if(p instanceof TracePacket){ // This is a trace packet
			messages += "\nReceived trace packet";
			cont.PacketIn((TracePacket)p, dpid);
		}
	}

	public String getMessages(){
		return messages;
	}

	/**
	 * Return the port the controller is on
	 */
//	@Override
//	public int ControllerPort() {
//		// TODO Auto-generated method stub
//		return 0;
//	}

	public int numSwitches() {
		return switches.size();
	}
	
	public void PortStatusChange(long dpid, int portNum, long mac){
		cont.OnPortStatusChange(dpid, portNum, mac);
	}
}
