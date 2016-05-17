package traceapp.tests;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import traceapp.core.Packet;
import traceapp.core.TracePacket;

/**
 * Represents a mechanism for matching packets to flows.
 * 
 * @author Aaron Pabst
 */
public class Match {
	private int inPort;
	private long srcMac;
	private long dstMac;
	private int ethType;
	private String protocol;
	
	public Match(int inPort, long srcMac, long dstMac, int ethType, String protocol){
		this.inPort = inPort;
		this.srcMac = srcMac;
		this.dstMac = dstMac;
		this.ethType = ethType;
		this.protocol = protocol;
	}
	
	/**
	 * Determines if this packet is a match. Default values are assumed to be don't-cares.
	 * 
	 * @param p
	 * @param inPort
	 * @return A value indicating whether or not the specified packet is a match.
	 */
	public boolean isMatch(IPacket p, int inPort){
		if((Integer)inPort != null && this.inPort != inPort && this.inPort > 0){
			return false;
		} 
//		else if((Long)srcMac != null && srcMac != p.getSource()){
//			return false;
//		}
//		else if((Long)dstMac > 0 && dstMac != p.getDestination().getLong() && dstMac > 0){
//			return false;
//		}
//		else if((Integer)ethType > 0 && ethType != p.getEtherType() && ethType > 0){
//			return false;
//		}
//		else if(protocol != null && protocol != p.getProtocol().toString() && protocol != ""){
//			return false;
//		}
		
		else if(p instanceof PingPacket){
			if(ethType != 0)
				return false;
		}
		else if(p instanceof PingReply){
			if(ethType != 0)
				return false;
		}
		else if(p instanceof TracePacket){
			// Check source and destination mac addresses
			if(ethType != 0x8220)
				return false;
//			TracePacket tp = (TracePacket)p;
//			if(tp.getDestination() != dstMac)
		}
		
		return true;
	}
}
