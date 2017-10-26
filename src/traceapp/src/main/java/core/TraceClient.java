package core;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.TCP;

import java.io.FileWriter;
import java.util.ArrayList;

public class TraceClient {

	private static final int TRACE_PORT = 6654;
	
	private static ArrayList<TracePacket> packets; 
	private static HashMap<Byte, Long> worstLatencies; // Latencies assuming packet is forwarded to controller @ every hop
	private static HashMap<Byte, Long> bestLatencies; // Latencies assuming packet is never forwarded to controller
	private static long startTime;
	private static String debugInfo;
	
	/**
	 * arg0 = destination mac address, TODO - Have mac address be inputed as a hex string and convert it to a long from there
	 * arg1 = interface
	 * arg2 = timeout in ms
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		
		try {
			packets = new ArrayList<TracePacket>();
			worstLatencies = new HashMap<Byte, Long>();
			bestLatencies = new HashMap<Byte, Long>();
			debugInfo = "";
			
			MacAddress mac = MacAddress.of(args[0]);
			
			TracePacket toSend = new TracePacket();
			toSend.setDestination(mac);
			TCP tcp = new TCP();
			tcp.setDestinationPort(TRACE_PORT);
			tcp.setSourcePort(TRACE_PORT);
			java.net.NetworkInterface ni = java.net.NetworkInterface.getByName(args[1]);
			toSend.setSource(MacAddress.of(ni.getHardwareAddress())); 
			toSend.setMaxHops((byte)255);
			toSend.setType(true);
			
			debugInfo = "Debug Info for trace packet:\n\n";
			debugInfo = debugInfo.concat(toSend.toString());
			
			jpcap.NetworkInterface[] interfaces = JpcapCaptor.getDeviceList();
		
			jpcap.NetworkInterface ji = null;
			for(int i = 0; i < interfaces.length; i++){
				jpcap.NetworkInterface curr = interfaces[i];
				if(curr.name.equals(args[1])){
					ji = curr;
					break;
				}
			}
			
			int timeout;
			if(args.length < 3)
				timeout = 10000;
			else
				timeout = Integer.parseInt(args[2]);
			
			debugInfo = debugInfo.concat("\n\nSending probe on interface " + ji.datalink_name + " with timeout " + timeout + "\n\n");
			
			JpcapCaptor captor = JpcapCaptor.openDevice(ji, 65535, false, timeout);
			JpcapSender sender = captor.getJpcapSenderInstance();
			jpcap.packet.Packet jTrace = translateToPcap(toSend);
			
//			System.out.println(Arrays.toString(jTrace.data));
			Date start = new Date();
			startTime = start.getTime();
			sender.sendPacket(jTrace);
			System.out.println("Torpedoes away!");
			
			debugInfo = debugInfo.concat("Trace began at " + startTime + "\n\n");
			
			// Wait for reply
			captor.processPacket(-1, new TraceReceiver());
			
//			Thread.sleep(timeout);
//			
//			System.out.println("Timeout reached.");
//			System.out.println("Packet status: " + packets);
//			System.out.println(packets.size());
			TracePacket[] arr = new TracePacket[packets.size()];
			int i = 0;
			for(TracePacket tp : packets){
				arr[i] = tp;
				i++;
			}
			Arrays.sort(arr);
			
			debugInfo = debugInfo.concat("\nTrimmed and sorted reply list:\n\n");
			
			int lastHop = -1; 
			
			for(TracePacket tp : arr){
				if(lastHop == tp.getHop())
					continue;
				
				debugInfo = debugInfo.concat(tp + "\n\n");
				System.out.println("Hop " + tp.getHop());
				System.out.println("\tDPID: " + tp.getSwInfo().getDpid());
				System.out.println("\tWorst Case Latency: " + worstLatencies.get(tp.getHop()) + "ms");
				System.out.println("\tBest Case Latency: " + bestLatencies.get(tp.getHop()) + "ms");
				System.out.println("\tTimestamp: " + tp.getTimestamp());
				System.out.println("\tIP address: " + tp.getSwInfo().getAddr().getAddress());
				System.out.println("\tIngress Port: " + tp.getSwInfo().getIngress());
				System.out.println();
				
				lastHop = tp.getHop();
			}
			
			FileWriter fw = new FileWriter("debug.txt");
			fw.write(debugInfo);
			fw.close();
			
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
		eth.frametype = (short)TracePacket.TRACE_REQUEST; // XXX
		
		pkt.datalink = eth;
		pkt.len = toSend.serialize().length;
		
		return pkt;
	}

	/**
	 * Callback for when Jpcap receives a packet
	 * 
	 * @author Aaron Pabst
	 */
	static class TraceReceiver implements PacketReceiver{
		
		//@Override
		public void receivePacket(Packet p) {
//			System.out.println("Received a packet!\n\tData: " + Arrays.toString(p.data));
//			System.out.println(((EthernetPacket)p.datalink).frametype);
//			System.out.println((short)0x8220);
//			System.out.println();
			
			// FIXME - We're overflowing frametype. We need to find a way to pull it out as an int.
			//	Less attractive option: Cast ethType to a short
			if(p.datalink instanceof EthernetPacket && ((EthernetPacket)p.datalink).frametype == (short)0x8220){ //if p is a trace packet
				TracePacket response = new TracePacket();
				try {
					response.deserialize(p.data, 0, p.data.length);
					// XXX - Multiple reply packets can be spawned due to packet flooding
					packets.add(response); // Collect the packet
					
					// perform latency calculations
					long latency = response.getTimestamp().getTime() - startTime;
					//System.out.println(response.getSwInfo().getLatency());
					long bestLatency = response.getTimestamp().getTime() - startTime - response.getSwInfo().getLatency().getValue();
					worstLatencies.put(response.getHop(), latency);
					bestLatencies.put(response.getHop(), bestLatency);
					
					debugInfo = debugInfo.concat("Reply Received:\n" + response + "\n");
				} catch (PacketParsingException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
}
