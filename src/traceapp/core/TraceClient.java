package traceapp.core;

import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;

import jpcap.JpcapSender;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.TCP;

import java.io.IOException;
import java.net.*;

public class TraceClient {

	private static final int TRACE_PORT = 6654;
	
	/**
	 * Arg1 = destination mac address
	 * arg2 = interface
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
			NetworkInterface ni = NetworkInterface.getByName(args[1]);
			toSend.setSource(MacAddress.of(ni.getHardwareAddress())); // Need to get this devices mac address somehow
			toSend.setTTL((byte)255);
			toSend.setType(true);
			
			byte[] data = toSend.serialize();
			
//			Socket s = new Socket();
//			s.bind(new InetSocketAddress(ni.getInetAddresses().nextElement(), 0));
			
			jpcap.NetworkInterface i = new jpcap.NetworkInterface(args[1], "", false, "", "", mac, jkb);
			JpcapSender sender = JpcapSender.openDevice(ni);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
