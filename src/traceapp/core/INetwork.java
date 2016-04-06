package traceapp.core;


/**
 * Specifies actions and events that the underlying infrastructure must provide in order to 
 * utilize the trace app. The middle man between the trace controller and the opendflow network.
 * 
 * An INetwork is aware of the existence of its TraceAppController and as such has access to methods
 * to notify it of an incoming packet.
 * 
 * @author Aaron Pabst
 */
public interface INetwork {
	
	/**
	 * Used to push a new flow out to the network
	 * 
	 * @param dpid
	 * @param inPort
	 * @param dstAddress
	 * @param actions
	 * @param priority
	 */
	void AddFlow(long dpid, int inPort, long dstAddress, int ethType, int priority, int outPort, String proto);
	
	/**
	 * Used to send a packet out on the specified datapath
	 * 
	 * TODO: Need to pack more information about the destination. Make sure that the information
	 * TODO: Being provided is universal across all controllers/hardware
	 * 
	 * Need source mac, destination mac, ip protocol, source IP, destination IP, TTL, and some data in
	 * the form a a byte array.
	 * 
	 * @param dpid
	 * @param msg
	 * @param outPort
	 */
	void SendPacket(long dpid, long srcMac, long dstMac, long srcIP, long dstIP, String proto, int TTL,
			int portNum);
}
