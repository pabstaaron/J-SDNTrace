package traceapp.tests;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;

public class PingPacket extends BasePacket{

	private static final int ethType = 9;
	
	// IP information
	private int sourceAddress;
	private int destAddress;
	
	PingPacket(int src, int dest){
		sourceAddress = src;
		destAddress = dest;
	}
	
	public int getSource(){
		return sourceAddress;
	}
	
	public int getDest(){
		return destAddress;
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
