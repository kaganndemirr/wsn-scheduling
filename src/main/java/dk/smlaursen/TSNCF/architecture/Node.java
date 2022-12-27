package dk.smlaursen.TSNCF.architecture;

public class Node {

	private final String aId;

	public Node(String id){
		this.aId = id;
	}

	@Override
	public String toString(){
		return aId;
	}
}
