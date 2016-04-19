package traceapp.tests;

import traceapp.core.Packet;

/**
 * Represents a virtual port on a switch.
 * 
 * The port's number should be immutable after initialization.
 * 
 * @author Aaron Pabst
 */
public class Port implements INetNode, Comparable<Port>{
	private int number;
	private boolean isEnabled;
	private Wire plugged; // null if no wire is connected. Reference to the connected wire otherwise.
	private DummySwitch parent;
	
	Port(int number, boolean isEnabled, DummySwitch parent){
		this.number = number;
		this.isEnabled = isEnabled;
		this.parent = parent;
	}
	
	public void enable(){
		isEnabled = true;
	}
	
	public void disable(){
		isEnabled = false;
	}

	@Override
	public String getType() {
		return "switchport";
	}
	
	public void plug(Wire w){
		plugged = w;
	}

	@Override
	public void packetIn(Packet p, long dpid, int port) {
		if(isEnabled)
			parent.packetIn(p, this);
	}

	public void packetOut(Packet p){
		if(plugged == null || !isEnabled)
			return;
		
		if(plugged.right == this)
			plugged.left.packetIn(p, parent.getDpid(), number);
		else
			plugged.right.packetIn(p, parent.getDpid(), number);
	}
	
	public INetNode getPlugged(){
		if(plugged == null)
			return null;
		
		if(plugged.left == this)
			return plugged.right;
		else
			return plugged.left;
	}
	
	@Override
	public int compareTo(Port p) {
		if(p.parent == parent && p.getNumber() == number)
			return 0;
		else if(number > p.getNumber())
			return 1;
		else 
			return -1;
	}

	public int getNumber() {
		return number;
	}

	public DummySwitch getParent() {
		// TODO Auto-generated method stub
		return parent;
	}
}
