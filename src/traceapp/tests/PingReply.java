package traceapp.tests;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;

public class PingReply extends BasePacket{
	private static final int ethType = 0;
	
	private int source;
	private int dst;
	
	public PingReply(int source, int dst){
		this.source = source;
		this.dst = dst;
	}
	
	public int getSource(){
		return source;
	}
	
	public int getDst(){
		return dst;
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
