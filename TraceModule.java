package net.floodlightcontroller.traceapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.internal.DeviceManagerImpl;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;
import sun.util.logging.resources.logging;
import traceapp.core.*;
import traceapp.core.TraceAppController.PortMacPair;


/**
 * Plugs the TraceApp into Floodlight.
 * 
 * 
 * @author Aaron Pabst
 */
public class TraceModule implements IOFMessageListener, IFloodlightModule, INetwork, IOFSwitchListener {
	
	/**
	 * Internal class for detecting device changes and keeping track of active devices
	 * 
	 */
	public class DeviceListener implements IDeviceListener {
		
		/**
		 * Map for keeping track of various devices
		 */
		private HashMap<MacAddress, SwitchPort[]> devices;
		private TraceModule parent;
		
		public DeviceListener(TraceModule traceModule) {
			devices = new HashMap<MacAddress, SwitchPort[]>();
			parent = traceModule;
		}

		/**
		 * 
		 * @return A map of all known mac addresses and their AP's
		 */
		public HashMap<MacAddress, SwitchPort[]> getDevices(){
			return devices;
		}
		
		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "DeviceListener";
		}

		@Override
		public boolean isCallbackOrderingPrereq(String type, String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isCallbackOrderingPostreq(String type, String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void deviceAdded(IDevice device) {
			// TODO Auto-generated method stub
			MacAddress mac = device.getMACAddress();
			SwitchPort[] ports = device.getAttachmentPoints();
			devices.put(mac, ports);
			
			//TODO - Need to update the trace app map on this event too, as switch features comes in before this event
			if(ports.length == 1){
				parent.tracer.OnPortStatusChange(ports[0].getSwitchDPID().getLong(), ports[0].getPort().getPortNumber(), mac.getLong());
			}
		}

		@Override
		public void deviceRemoved(IDevice device) {
			devices.remove(device.getMACAddress());
		}

		@Override
		public void deviceMoved(IDevice device) {
			devices.remove(device);
			devices.put(device.getMACAddress(), device.getAttachmentPoints());
		}

		@Override
		public void deviceIPV4AddrChanged(IDevice device) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deviceIPV6AddrChanged(IDevice device) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void deviceVlanChanged(IDevice device) {
			// TODO Auto-generated method stub
			
		}

	}

	// Reference back to the trace app. For notifying the app of packet-ins.
	protected TraceAppController tracer;
	
	protected IFloodlightProviderService floodlightProvider;
	
	protected IOFSwitchService switchProvider;
	
	protected IDeviceService deviceManager;
	
	private TraceModule.DeviceListener dl;
	
	private static final int TRACE_ETHER = 0x8220;
	
	protected static Logger logger;
	
	@Override
	public String getName() {
		return TraceModule.class.getName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(IOFSwitchService.class);
		    l.add(IDeviceService.class);
		    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchProvider = context.getServiceImpl(IOFSwitchService.class);
		deviceManager = context.getServiceImpl(IDeviceService.class);
		dl = new TraceModule.DeviceListener(this);
		tracer = new TraceAppController(this);
		logger = LoggerFactory.getLogger(TraceModule.class);
		logger.info("TraceApp initilized");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchProvider.addOFSwitchListener(this);
		deviceManager.addListener(dl);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
				switch(msg.getType()){
		case PACKET_IN:
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, 
								IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
			logger.info("TraceEther: " + Integer.toString(TRACE_ETHER) + ", PktEther: " + Integer.toString(eth.getEtherType().getValue()));
			
			if(eth.getEtherType().getValue() == TRACE_ETHER){
				Data d = (Data)eth.getPayload();
				byte[] data = d.getData();
				TracePacket tp = new TracePacket();
				
				try {
					tp.deserialize(data, 14, data[14]);
				} catch (PacketParsingException e) {
					e.printStackTrace();
				}
				
				if(tp.isRequest())
					
					tracer.PacketIn(tp, sw.getId().getLong());
				else {
					logger.info("Saw a reply");
				}
			}
			else
				return Command.CONTINUE; // Ignore ARP for the time being
				default:
			break;
		}
		
		return Command.CONTINUE;
	}

	@Override
	public void AddFlow(long dpid, Match m, OFPort port) {
		IOFSwitch sw = switchProvider.getActiveSwitch(DatapathId.of(dpid));
	
		writeFlowMod(sw, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, m, port);
		
	}

	@Override
	public Match buildTraceMatch(long dpid){
		IOFSwitch sw = switchProvider.getActiveSwitch(DatapathId.of(dpid));
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.ETH_TYPE, EthType.of(TRACE_ETHER));
		
		logger.info("Switch added!");
		return mb.build();
	}
	
	/**
	 * FIXME - This method is not properly sending the packet
	 * 
	 * @param dpid
	 * @param pkt
	 */
	@Override
	public void SendTracePacket(long dpid, TracePacket pkt, OFPort p) {
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(pkt.getSource());
		l2.setDestinationMACAddress(((TracePacket)pkt).getDestination());
		l2.setEtherType(EthType.of(TRACE_ETHER));
		
		l2.setPayload(pkt);
		
		byte[] serialized = l2.serialize();

		IOFSwitch sw = switchProvider.getActiveSwitch(DatapathId.of(dpid));
		
		List<OFAction> act =  Collections.singletonList((OFAction)sw.getOFFactory().actions().buildOutput().setPort(p).build());
		
		OFPacketOut po = sw.getOFFactory().buildPacketOut()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setInPort(OFPort.CONTROLLER)
				.setActions(act)
				.setData(serialized)
				.build();
		
		sw.write(po);
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		IOFSwitch sw = switchProvider.getActiveSwitch(switchId);
		
		ArrayList<PortMacPair> pm = new ArrayList<PortMacPair>();
		for(OFPortDesc p : sw.getPorts()){
			if(p.getHwAddr().getLong() < 0)
				continue;
			PortMacPair portMac = tracer.new PortMacPair();
			
			portMac.setPort(p.getPortNo().getPortNumber());
		
			// FIXME - Need to look the port up in DeviceManager to get the contained mac address. 
			MacAddress addr = findMacOnPort(dl.getDevices(), p.getPortNo(), switchId); // Try to find the mac address on this port
			
			// Only add the port if it has exactly one MacAddress
			if(addr != MacAddress.NONE){ // XXX - Does floodlight define this operator?
				portMac.setMac(p.getHwAddr().getLong());
				pm.add(portMac);
			}
		}
		tracer.SwitchInfoReceived(switchId.getLong(), pm);
	}
	
	private MacAddress findMacOnPort(HashMap<MacAddress, SwitchPort[]> map, OFPort p, DatapathId dpid){
		SwitchPort sp = new SwitchPort(dpid, p); // Initialize a SwitchPort object to search with
		
		// We want to pull the key that maps to the below value 
		SwitchPort[] target = new SwitchPort[1];
		target[0] = sp;

		for(MacAddress addr : map.keySet()){
			SwitchPort[] current = map.get(addr);
			
			if(Arrays.equals(target, current))
				return addr;
		}
		return MacAddress.NONE;
	}
	
	@Override
	public void switchRemoved(DatapathId switchId) {
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
		logger.info("port changed");
		
		if(type.equals(PortChangeType.UP) || type.equals(PortChangeType.ADD))
			// FIXME - OFPortDesc.getHwAddr() returns the hardware address of the port, no the thing that's connected to it
			tracer.OnPortStatusChange(switchId.getLong(), port.getPortNo().getPortNumber(), port.getHwAddr().getLong());
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		
	}
	
	/**
	 * Writes a OFFlowMod to a switch.
	 * @param sw The switch to write the flowmod to.
	 * @param command The FlowMod actions (add, delete, etc).
	 * @param bufferId The buffer ID if the switch has buffered the packet.
	 * @param match The OFMatch structure to write.
	 * @param outPort The switch port to output it to.
	 */
	private void writeFlowMod(IOFSwitch sw, OFFlowModCommand command, OFBufferId bufferId, Match match, OFPort outPort) {
		
		OFFlowMod.Builder fmb;
		if (command == OFFlowModCommand.DELETE) {
			fmb = sw.getOFFactory().buildFlowDelete();
		} else {
			fmb = sw.getOFFactory().buildFlowAdd();
		}
		fmb.setMatch(match);
		fmb.setCookie(U64.ZERO);
		fmb.setIdleTimeout(0);
		fmb.setHardTimeout(0);
		fmb.setPriority(10);
		fmb.setBufferId(bufferId);
		fmb.setOutPort((command == OFFlowModCommand.DELETE) ? OFPort.ANY : outPort);
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		if (command != OFFlowModCommand.DELETE) {
			sfmf.add(OFFlowModFlags.SEND_FLOW_REM);
		}
		fmb.setFlags(sfmf);

		List<OFAction> al = new ArrayList<OFAction>();
		al.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
		fmb.setActions(al);


		// and write it out
		sw.write(fmb.build());
		
	}

}
