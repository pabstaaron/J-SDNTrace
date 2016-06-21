package traceapp.core;

import java.util.Arrays;

import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.TCP;

public class TraceClient {

	private static final int TRACE_PORT = 6654;
	
//	static{
//		System.loadLibrary("libjpcap.so");
//	}
	
	/**
	 * Arg0 = destination mac address
	 * arg1 = interface
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Step 1: Construct a TracePacket to addressed to the destination
		// Step 2: Send the packet out the specified interface
		// Step 3: Wait x number of seconds for a reply
		
		try {
			long mac = Long.parseLong(args[0]);
			
			TracePacket toSend = new TracePacket();
			toSend.setDestination(MacAddress.of(mac));
			toSend.setProtocol(IpProtocol.TCP);		
			TCP tcp = new TCP();
			tcp.setDestinationPort(TRACE_PORT);
			tcp.setSourcePort(TRACE_PORT);
			java.net.NetworkInterface ni = java.net.NetworkInterface.getByName(args[1]);
			toSend.setSource(MacAddress.of(ni.getHardwareAddress())); 
			toSend.setTTL((byte)255);
			toSend.setType(true);
			
//			byte[] data = toSend.serialize();
			
//			System.out.println(System.getProperty("java.library.path"));
			
			jpcap.NetworkInterface[] interfaces = JpcapCaptor.getDeviceList();
		
			jpcap.NetworkInterface ji = null;
			for(int i = 0; i < interfaces.length; i++){
				jpcap.NetworkInterface curr = interfaces[i];
				if(curr.name.equals(args[1])){
					ji = curr;
					break;
				}
			}
			
			JpcapCaptor captor = JpcapCaptor.openDevice(ji, 65535, false, 5000);
			JpcapSender sender = captor.getJpcapSenderInstance();
			jpcap.packet.Packet jTrace = translateToPcap(toSend);
			
			System.out.println(Arrays.toString(jTrace.data));
			sender.sendPacket(jTrace);
			System.out.println("Torpedoes away!");
			
			// Wait for reply
			captor.processPacket(-1, new TraceReceiver());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static jpcap.packet.Packet translateToPcap(TracePacket toSend) {
		jpcap.packet.Packet pkt = new jpcap.packet.Packet();
		pkt.data = toSend.serialize();
		
		jpcap.packet.EthernetPacket eth = new jpcap.packet.EthernetPacket();
		
		eth.dst_mac = toSend.getDestination().getBytes();
		eth.src_mac = toSend.getSource().getBytes();
		eth.frametype = (short)toSend.TRACE_TYPE; // XXX
		
		pkt.datalink = eth;
		pkt.len = toSend.serialize().length;
		
		return pkt;
	}

	private static byte[] ipToBytes(String ipAddress) {
		byte[] raw = new byte[4];
		String[] split = ipAddress.split("[.]+");
		
		if(split.length != 4)
			throw new IllegalArgumentException();
		
		for(int i = 0; i < 4; i++){
			raw[i] = (byte)Integer.parseInt(split[i]);
		}
		
		return raw;
	}

	/**
	 * Callback for when Jpcap receives a packet
	 * 
	 * @author Aaron Pabst
	 */
	static class TraceReceiver implements PacketReceiver{
		
		@Override
		public void receivePacket(Packet p) {
			// TODO Auto-generated method stub
			if(p.datalink instanceof EthernetPacket && ((EthernetPacket)p.datalink).frametype == 0x8220){ // p is a trace packet
				TracePacket response = new TracePacket();
				try {
					response.deserialize(p.data, 0, p.data.length);
					System.out.println("Trace response received");
				} catch (PacketParsingException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
