package traceapp.core;

import java.util.Date;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Represents a hop in the trace
 * 
 * @author Aaron Pabst
 */
public class Hop {
	protected long dpid;
	protected OFPort ingress;
	protected Date timestamp;
	
	public Hop(long dpid){
		this.dpid = dpid;
	}
	
	public void setDpid(long dpid){
		this.dpid = dpid;
	}
	
	public void setTimestamp(Date d){
		timestamp = d;
	}
	
	public void setIngress(OFPort p){
		ingress = p;
	}
	
	public long getDpid(){
		return dpid;
	}
	
	public OFPort getIngress(){
		return ingress;
	}
	
	public Date getTimestamp(){
		return timestamp;
	}
	
	
	@Override
	public String toString(){
		String res = "Datapath ID: ";
		
		DatapathId id = DatapathId.of(dpid);
		res += id.toString();
		
		if(ingress != null){
			res += "\nIngress Port: " + Integer.toString(ingress.getPortNumber());
		}
		
		if(timestamp != null){
			res += "\nTimestamp: " + timestamp.toString();
		}
		
		return res;
	}
}
