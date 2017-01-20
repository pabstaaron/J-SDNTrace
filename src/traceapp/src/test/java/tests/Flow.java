package traceapp.tests;

import java.util.*;

import net.floodlightcontroller.packet.IPacket;
import traceapp.core.Packet;
import traceapp.core.TracePacket;

/**
 * Represents a simple flow for testing purposes
 * 
 * @author Aaron Pabst
 */
public class Flow implements Comparable<Flow> {
	
	private int  outPort, priority;
	private List<Match> matchCriterea;
	
	public Flow(int outPort, int priority){
		this.outPort = outPort;
		this.priority = priority;
		
		matchCriterea = new ArrayList<Match>();
	}
	
	public void AddMatch(Match m){
		matchCriterea.add(m);
	}
	
	public int getOut(){
		return outPort;
	}
	
	/**
	 * Determine whether or not the specified packet matches to this flow.
	 * 
	 * @param p
	 * @param inPort
	 * @return
	 */
	public boolean IsMatch(IPacket p, int inPort){
		for(Match m : matchCriterea){
			if(!m.isMatch(p, inPort))
				return false;
		}
		return true;
	}

	public int getPriority(){
		return priority;
	}
	@Override
	public int compareTo(Flow f) {
		if(f.getPriority() == priority)
			return 0;
		else if(priority > f.getPriority())
			return 1;
		else
			return -1;
	}
	
}
