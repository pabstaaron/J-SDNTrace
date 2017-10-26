package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.packet.PacketParsingException;

public class UnitTests {

	@Test
	public void tracePackTest() {
		TracePacket tp = new TracePacket();
		tp.setDestination(MacAddress.of(1));
		tp.setSource(MacAddress.of(2));
		tp.setMaxHops((byte)100);
		tp.setHop((byte)-1);
		
		SwitchInfo inf = new SwitchInfo();
		inf.setDpid(DatapathId.NONE);
		inf.setIngress(OFPort.of(1));
		inf.setIpAddr(new InetSocketAddress(1));
		inf.setLinkLatency(U64.of(1));
		inf.setContId(" ");
		
		tp.setSwInfo(inf);
		
		TracePacket tp2 = new TracePacket();
		try {
			tp2.deserialize(tp.serialize(), 0, tp.serialize().length);
			assertTrue(tp.equals(tp2));
		} catch (PacketParsingException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void hopPacketTest(){
		Network net = new Network();
		TraceAppController tac = new TraceAppController(net);
		
		TracePacket tp = new TracePacket();
		tp.setDestination(MacAddress.of(1));
		tp.setSource(MacAddress.of(0));
		tp.setType(true);
		tp.setMaxHops((byte)255);
		byte[] data = tp.serialize();
		
//		tac.PacketIn(data, 1, OFPort.of(1));

		assertTrue(net.getHop() == 0);
		
//		tac.PacketIn(data, 2, OFPort.of(1));
		
		assertTrue(net.getHop() == 1);
		
//		tac.PacketIn(data, 2, OFPort.of(1));
		
		assertTrue(net.getHop() == 2);
	}
}

class Network implements INetwork{
	int lastHop;
	
	Network(){
		lastHop = 0;
	}
	
	public int getHop(){
		return lastHop;
	}
	
	@Override
	public void AddFlow(long dpid, Match m, OFPort port) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void SendTracePacket(long dpid, TracePacket pkt, OFPort p) {
		// TODO Auto-generated method stub
		lastHop = pkt.getHop();
	}

	@Override
	public Match buildTraceMatch(long dpid) {
		// TODO Auto-generated method stub
		return null;
	}
	
}