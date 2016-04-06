package traceapp.tests;

import traceapp.core.Packet;

public interface INetNode {
	/**
	 * Return whether this node is a 
	 * @return
	 */
	public String getType();
	
	public void plug(Wire w);
	
	public void packetIn(Packet p);
}
