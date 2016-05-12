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
	
	private int controllerPort;
	
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
		
		controllerPort = -1;
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
	 * TODO - Port status change should be signaled here
	 * 
	 * Throws IllegalArgumentException if port is null or out of range
	 * @param port
	 * @param w
	 */
	public Port plug(int port, Wire w){
		
		if(port <= 0 || port > ports.size())
			throw new IllegalArgumentException();
		
		// Send a message out the controller port indicating that the port status was changed
		if(w.right instanceof DummyHost){
			Object[] statParams = new Object[3];
			statParams[0] = dpid;
			statParams[1] = port;
			statParams[2] = ((DummyHost)w.right).getMac();
			
			Packet p = new Packet(-1, -1, -1, -1, "", 10, statParams);
			getPort(controllerPort).packetOut(p);
		}
		else if(w.left instanceof DummyHost){
			Object[] statParams = new Object[3];
			statParams[0] = dpid;
			statParams[1] = port;
			statParams[2] = ((DummyHost)w.left).getMac();
			
			Packet p = new Packet(-1, -1, -1, -1, "", 10, statParams);
			getPort(controllerPort).packetOut(p);
		}
		
		int i = Collections.binarySearch(ports, new Port(port, false, this));
		ports.get(i).plug(w);
		return ports.get(i);
	}
	
	public Port plug(int port, Wire w, boolean isController){
		if(isController)
			controllerPort = port;
		return plug(port, w);
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
		
		for(Flow f : flowTable){ // TODO - Problem here when trace response is sent from controller. This will match the trace criteria and kick off an infinite loop
			if(f.IsMatch(p, in.getNumber())){
				Port out = getPort(f.getOut());
				out.packetOut(p);
				return;
			}
		}
		
		// TODO - The controller needs to get notified here
		Port conn = getPort(controllerPort);
		conn.packetOut(p);
		
		for(Port out : ports){
			if((in != null && out.getNumber() == in.getNumber()) || out.getNumber() == controllerPort) // Don't send the packet back out the port it came in on. 
				continue;
			out.packetOut(p);
		}
	}
	
	public void packetInWoController(Packet p, Port in){
		for(Port out : ports){
			if((in != null && out.getNumber() == in.getNumber()) || out.getNumber() == controllerPort) // Don't send the packet back out the port it came in on. 
				continue;
			out.packetOut(p);
		}
	}
}
