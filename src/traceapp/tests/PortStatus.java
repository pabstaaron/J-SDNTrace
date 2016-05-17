package traceapp.tests;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;

public class PortStatus extends BasePacket{

	long dpid, mac;
	int port;
	
	public PortStatus(long dpid, int port, long mac){
		this.dpid = dpid;
		this.port = port;
		this.mac = mac;
	}
	
	public long getDpid(){
		return dpid;
	}
	
	public long getMac(){
		return mac;
	}
	
	public int getPort(){
		return port;
	}
	
	@Override
	public byte[] serialize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException {
		// TODO Auto-generated method stub
		return null;
	}

}
