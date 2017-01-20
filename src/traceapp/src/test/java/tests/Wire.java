package traceapp.tests;

/**
 * Represents a link between two network nodes
 * 
 * A node can be either a DummyHost or a port on a DummySwitch
 * 
 * @author Aaron Pabt
 */
public class Wire {
	INetNode left;
	INetNode right;
	
	public Wire(){}
	
	public Wire(INetNode left, INetNode right){
		this.left = left;
		this.right = right;
	}
	
	public boolean hasLeft(){
		return left != null;
	}
	
	public boolean hasRight(){
		return right != null;
	}
	
	public void setLeft(INetNode node){
		left = node;
	}
	
	public void setRight(INetNode node){
		right = node;
	}
	
	public INetNode getLeft(){
		return left;
	}
	
	public INetNode getRight(){
		return right;
	}
}
