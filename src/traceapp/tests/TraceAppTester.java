package traceapp.tests;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * Provides automatic tests for TraceApp that are run independent of any sort of physical network.
 * 
 * @author Aaron Pabst
 */
public class TraceAppTester {

	//The following tests verify the correct operation of the fake network objects
	@Test
	public void testTester() {
		DummyNetwork n = new DummyNetwork();
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
		DummyNetwork n = new DummyNetwork();
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
		DummyNetwork net = new DummyNetwork();
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
		
		net.AddFlow(1, 1, -1, -1, 100, 4, "tcp");
		
		h1.sendPingRequest(5);
		
		Assert.assertTrue(h1.getMessages().length() != 0);
		Assert.assertEquals("\nSending ping request to 5...\nPing reply received from: 5", h1.getMessages());
	}
}
