--SDNTrace--
Last Updated On: 7/15/2016 by Aaron Pabst

Provides layer 2 tracing capabilities for OpenFlow networks running 
the Floodlight controller, with planned integration into other OpenFlow
controllers.

Presently, the application only records the switch DPID for each hop in
the probe packet's path. However, ingress/egress port, switch IP address,
switch MAC address, etc... will be added to the information recorded in the
future.

To report a problem or suggest a new feature, email Joe Breen at joe.breen@utah.edu
or Aaron Pabst at aaron.pabst@utah.edu.

--Installation--

Step 1: Download and install Eclipse. 
     sudo apt-get install eclipse

Step 2: Download and install the Floodlight controller. Information on installing
Floodlight can be found at https://floodlight.atlassian.net/wiki/display/floodlightcontroller/Installation+Guide
	   *You should be familiar with Floodlight before trying to install this package

Step 3: Clone the J-SDNTrace repository.
     git clone https://github.com/pabstaaron/J-SDNTrace

Step 4: Copy the net.floodlightcontroller.traceapp package into the src/main/java directory
of your Floodlight installation.

Step 5: Register the TraceModule.java file with the floodlight module loader by adding the 
following line to the src/main/resources.floodlightdefault.properties file of Floodlight:
	  net.floodlightcontroller.traceapp.TraceModule,\
this line should now be the second line in the file. This is to ensure the the traceapp
is able to get at the packet before the rest of the controller is allowed to deal with it.

Step 6: Install mininet.
     sudo apt-get install mininet

Step 7: Install Jpcap. See this link for more information:
	http://jpcap.gitspot.com/install.html

	*We will have detailed instructions on how to do this in the near future.

Step 8: Verify Installation by first starting the controller inside eclipse. After its running without error,
run the following command on a seperate command line:
    sudo mn --topo=linear,3 --mac --controller=remote,ip=0.0.0.0,port=6653

Run the mininet command pingall in order to ensure that the controller is aware of all links in 
the network.

Open an xterm for h1:
     xterm h1

Within the xterm, navigate to your TraceApp directory. Run the SDNTrace.jar executable as follows:
       sudo java -jar SDNTrace.jar 3 h1-eth0

You should receive the following output:

Torpedoes away!
Datapath ID: 00:00:00:00:00:00:00:01
Ingress Port: -8
Timestamp: Tue Jul 19 11:54:47 MDT 2016

Datapath ID: 00:00:00:00:00:00:00:02
Ingress Port: -8
Timestamp: Tue Jul 19 11:54:47 MDT 2016

Datapath ID: 00:00:00:00:00:00:00:03
Ingress Port: -8
Timestamp: Tue Jul 19 11:54:47 MDT 2016
