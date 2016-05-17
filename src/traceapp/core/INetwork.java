package traceapp.core;

import net.floodlightcontroller.packet.IPacket;

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
	 * Action is expected to be a string the corresponds to a flow action.
	 * 	"controller" - Send the packet to the controller
	 *  "<integer>"- Send the packet out a specific port
	 *   
	 * @param dpid
	 * @param inPort
	 * @param dstAddress
	 * @param action
	 * @param priority
	 */
	void AddFlow(long dpid, int inPort, long dstAddress, int ethType, int priority, String action, String proto);
	
	/**
	 * Used to send a packet out on the specified datapath
	 * 
	 * Need source mac, destination mac, ip protocol, source IP, destination IP, TTL, and some data in
	 * the form a a byte array.
	 * 
	 * It is expected that the data array will be serialized in the OF controller.
	 * 
	 * @param dpid
	 * @param msg
	 * @param outPort
	 */
	void SendPacket(long dpid, IPacket pkt);

	/**
	 * @return The port on any given switch the controller is connected to. Most OF controllers provide
	 * 			something like this. 
	 * 
	 * TODO There is a specific action provided by OF to send a packet to the controller. We need to 
	 * 		find a way to leverage that. We won't be able to get an integer that corresponds to the 
	 * 		controller port in real life.
	 */
//	int ControllerPort();
}
