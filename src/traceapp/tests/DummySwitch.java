package traceapp.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import traceapp.core.Packet;

/**
 * Represents a simple virtual switch with a set of ports and a flow table.
 * 
 * Needs to have user defined flows in order to route core.Packet objects.
 * 
 * @author Aaron Pabst
 *
 */
public class DummySwitch implements Comparable<DummySwitch>{
	private List<Port> ports;
	
	private long dpid;
	
	private long mac;
	
	private long ipAddress;
	
	private List<Flow> flowTable;
	
	/**
	 * TODO fill-in
	 * 
	 * @param dpid
	 * @param numPorts
	 * @param mac
	 * @param ipV4
	 */
	public DummySwitch(long dpid, int numPorts, long mac, long ipV4){
		this.dpid = dpid;
		this.mac = mac;
		ipAddress = ipV4;
		
		ports = new ArrayList<Port>();
		for(int i = 1; i <= numPorts; i++){
			Port p = new Port(i, true, this);
			ports.add(p);
		}
		
		flowTable = new ArrayList<Flow>();
	}
	
	/**
	 * Add a new flow to this switch
	 * 
	 * @param m
	 * @param outPort
	 */
	public void AddFlow(Match m, int outPort, int priority){
		Flow f = new Flow(outPort, priority);
		f.AddMatch(m);
		
		flowTable.add(f);
		
		Collections.sort(flowTable); // Maintain a table sorted by priority
	}
	
	/**
	 * "Plug" a cable into the specified port.
	 * 
	 * Throws IllegalArgumentException if port is null or out of range
	 * @param port
	 * @param w
	 */
	public Port plug(int port, Wire w){
		
		if(port <= 0 || port > ports.size())
			throw new IllegalArgumentException();
		
		int i = Collections.binarySearch(ports, new Port(port, false, this));
		ports.get(i).plug(w);
		return ports.get(i);
	}

	@Override
	public int compareTo(DummySwitch sw) {
		if(dpid == sw.dpid)
			return 0;
		else if(dpid > sw.dpid)
			return 1;
		else
			return -1;
	}
	
	public Port getPort(int portNumber){
		if(portNumber < 0)
			return null;
		int i = Collections.binarySearch(ports, new Port(portNumber, false, this));
		return ports.get(i);
	}
	
	public long getDpid(){
		return dpid;
	}
	
	public void packetIn(Packet p, Port in){
		// Determine if there is a flow that matches the packet
			// If yes, take the action defined by that flow
			//Else flood to all ports
		
		for(Flow f : flowTable){
			if(f.IsMatch(p, in.getNumber())){
				Port out = getPort(f.getOut());
				out.packetOut(p);
				return;
			}
		}
		
		for(Port out : ports){
			if(out.getNumber() == in.getNumber()) // Don't send the packet back out the port it came in on. 
				continue;
			out.packetOut(p);
		}
	}
}
