package traceapp.core;


/**
 * Represents a hop in the trace
 * 
 * @author Aaron Pabst
 */
public class Hop {
	protected long dpid;
	
	public Hop(long dpid){
		this.dpid = dpid;
	}
	
	public void setDpid(long dpid){
		this.dpid = dpid;
	}
	
	public long getDpid(){
		return dpid;
	}
}
