package traceapp.tests;

import java.util.ArrayList;
import java.util.Random;

import org.junit.*;

import traceapp.core.TraceAppController;

/**
 * Provides automatic tests for TraceApp that are run independent of any sort of physical network.
 * 
 * @author Aaron Pabst
 */
public class TraceAppTester {

	// Test a trace over 1 network hop. 
	// We expect a trace reply that contains only a hop for the dpid of the lone switch.
	@Test
	public void oneHop(){
		// Set up a network with one switch and two hosts.
		DummyNetwork net = new DummyNetwork(1, 1);
		DummySwitch s1 = net.AddSwitch(2, 2, 2, 2);
		DummyHost h1 = net.AddHost(3, 3);
		DummyHost h2 = net.AddHost(4, 4);
		
		Wire w1 = new Wire();
		w1.left = h1;
		h1.plug(w1);
		w1.right = s1.plug(1, w1);
		
		Wire w2 = new Wire();
		w2.left = s1.plug(2, w2);
		w2.right = h2;
		h2.plug(w1);
		
		TraceAppController cont = new TraceAppController(net);
		
		// Send a trace request from h1 to h2.
		h1.sendPingRequest(4); // TODO shouldn't need to ping ahead before a trace.
		Assert.assertTrue(h1.getMessages().contains("Ping reply received from:"));
		h1.sendTrace("tcp", 4);
		Assert.assertTrue(h1.getMessages().contains("Received trace reply"));
	}
	
	@Test
	public void longLinerTest(){
		DummyNetwork net = generateRandomNetwork(10, "linear", 6, false);
		
		DummySwitch s1 = net.randomSwitch(5);
		DummySwitch s2 = net.randomSwitch(11);
		
		DummyHost h1 = (DummyHost)s1.getPort(3).getPlugged();
		DummyHost h2 = (DummyHost)s2.getPort(3).getPlugged();
		
		h1.sendPingRequest(h2.getIp());
		Assert.assertTrue(h1.getMessages().contains("Ping reply received from:"));
		
		h1.sendTrace("tcp", h2.getMac());
		Assert.assertTrue(h1.getMessages().contains("Received trace reply"));
	}
	
	/**
	 * Generates a random pseudo-network of the specified topology.
	 * The generated network is garunteed to be traceable.
	 * 
	 * Valid topology selections are single, linear, star, mesh, and unspecified
	 * 
	 * If the makeLoops parameter is set to true, loops may appear in the topology
	 * 
	 * @param seed
	 * @param topo
	 * @param maxNodes
	 * @return
	 */
	public DummyNetwork generateRandomNetwork(int seed, String topo, int maxNodes, boolean makeLoops){
		Random rand = new Random(seed);
		DummyNetwork net = new DummyNetwork(1, 1);
		
		if(maxNodes < 0){
			maxNodes = rand.nextInt(1000);
			maxNodes += 3; // Ensure that there are at least three nodes, the minimum required for traceablity
		}
		
		ArrayList<DummySwitch> switches = new ArrayList<DummySwitch>();
		
		if(topo == "linear"){
			// Generate a string of switches, then connect random hosts up to those switches
			while(maxNodes % 2 != 0 && maxNodes < 4) // The number of nodes must be even for a linear topology.
				maxNodes++;
			int maxSwitches = (int)((1.0/2)*maxNodes);
			int i = 0;
			do{
				switches.add(net.AddSwitch(rand.nextInt(100000), 100, rand.nextInt(100000), rand.nextInt(100000)));
				i++;
				if(switches.size() >= 2){
					Wire w = new Wire();
					w.left = switches.get(i-2).plug(1, w);
					w.right = switches.get(i-1).plug(2, w);
				}
			}while(net.numSwitches() < maxSwitches);
			
			for(DummySwitch s : switches){
				DummyHost h = net.AddHost(rand.nextInt(100000), rand.nextInt(100000));
				Wire w = new Wire();
				w.left = h;
				h.plug(w);
				w.right = s.plug(3, w);
			}
		}
		
		for(DummySwitch s : switches){
			Wire w = new Wire();
			w.left = net;
			w.right = s.plug(4, w);
			net.plug(w);
		}
		
		return net;
	}
	
	//The following tests verify the correct operation of the fake network objects and provide examples
	//for using the testing structure
	@Test
	public void testTester() {
		DummyNetwork n = new DummyNetwork(0, 0);
		DummyHost h1 = n.AddHost(1, 1);
		DummyHost h2 = n.AddHost(2, 2);
		DummySwitch s1 = n.AddSwitch(3, 10, 3, 3);
		
		Wire b1 = new Wire();
		b1.left = h1;
		Port p1 = s1.plug(1, b1);
		b1.right = p1;
		
		Wire b2 = new Wire();
		b2.right = h2;
		b2.left = s1.plug(2, b2);
		
		h1.plug(b1);
		h2.plug(b2);
		
		h1.sendPingRequest(2);
		Assert.assertTrue(h1.getMessages().length() != 0);
		Assert.assertEquals("\nSending ping request to 2...\nPing reply received from: 2", h1.getMessages());
	}

	@Test
	public void twoSwitches(){
		DummyNetwork n = new DummyNetwork(0, 0);
		DummyHost h1 = n.AddHost(1, 1);
		DummyHost h2 = n.AddHost(2, 2);
		DummySwitch s1 = n.AddSwitch(3, 10, 3, 3);
		DummySwitch s2 = n.AddSwitch(11, 100, 4, 4);
		
		Wire b1 = new Wire();
		b1.left = h1;
		b1.right = s1.plug(1, b1);
		
		Wire b2 = new Wire();
		b2.left = s1.plug(2, b2);
		b2.right = s2.plug(100, b2);
		
		Wire b3 = new Wire();
		b3.left = s2.plug(10, b3);
		b3.right = h2;
		
		h1.plug(b1);
		h2.plug(b3);
		
		h1.sendPingRequest(2);
		Assert.assertTrue(h1.getMessages().length() != 0);
		Assert.assertEquals("\nSending ping request to 2...\nPing reply received from: 2", h1.getMessages());
	}
	
	@Test
	public void addFlowTest(){
		DummyNetwork net = new DummyNetwork(0, 0);
		DummySwitch s1 = net.AddSwitch(1, 1000, 1, 1);
		DummyHost h1 = net.AddHost(2, 2);
		
		Wire b1 = new Wire();
		b1.left = h1;
		b1.right = s1.plug(1, b1);
		h1.plug(b1);
		
		
		// Populate the rest of the ports
		for(int i = 0; i < 4; i++){
			DummyHost h = net.AddHost(i+3, i+3);
			
			Wire w = new Wire();
			w.left = h;
			w.right = s1.plug(i+2, w);
			
			h.plug(w);
		}
		
		net.AddFlow(1, 1, -1, -1, 100, "4", "tcp");
		
		h1.sendPingRequest(5);
		
		Assert.assertTrue(h1.getMessages().length() != 0);
		Assert.assertEquals("\nSending ping request to 5...\nPing reply received from: 5", h1.getMessages());
	}
	
	@Test
	public void TraceHitsControllerTest(){
		DummyNetwork net = new DummyNetwork(0, 0);
		DummyHost h1 = net.AddHost(1, 1);
		DummySwitch s1 = net.AddSwitch(2, 100, 2, 2);
		DummyHost h2 = net.AddHost(3, 3);
		
		Wire w1 = new Wire();
		w1.left = h1;
		Port p = s1.plug(1, w1);
		w1.right = p;
		h1.plug(w1);
		
		Wire w2 = new Wire();
		Port p1 = s1.plug(2, w2);
		w2.left = p1;
		w2.right = h2;
		h2.plug(w2);
		
		Wire w3 = new Wire();
		w3.left = net;
		net.plug(w3);
		Port p2 = s1.plug(3, w3);
		w3.right = p2;
		
		net.AddFlow(2, -1, -1, 0x8820, 1000, Integer.toString(p2.getNumber()), "");
		h1.sendTrace("tcp", 3);
		Assert.assertEquals("\nReceived trace packet", net.getMessages());
	}
}
