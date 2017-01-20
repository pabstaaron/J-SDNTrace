package traceapp.tests;

import net.floodlightcontroller.packet.IPacket;
import traceapp.core.Packet;
import traceapp.core.TracePacket;

public interface INetNode {
	/**
	 * Return whether this node is a 
	 * @return
	 */
	public String getType();
	
	public void plug(Wire w);
	
	public void packetIn(IPacket p, long dpid, int port);
}
