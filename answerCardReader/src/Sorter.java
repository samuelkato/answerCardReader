import java.util.Comparator;
import java.util.Hashtable;

public class Sorter implements Comparator<Hashtable<String,Integer>>{
	private String eixo;
	private int order=1;
	public Sorter(String eixo,int order){
		this.eixo=eixo;
		this.order*=order;
	}
	@Override
	public int compare(Hashtable<String, Integer> o1,Hashtable<String, Integer> o2) {
		int retPri=-1*this.order;
		int retSec=1*this.order;
		if(o1.containsKey("clock") && !o2.containsKey("clock"))return -1;
		else if(!o1.containsKey("clock") && o2.containsKey("clock"))return 1;
		else if(o1.get("centro"+this.eixo)>o2.get("centro"+this.eixo))return retSec;
		else if(o1.get("centro"+this.eixo)==o2.get("centro"+this.eixo))return 0;
		else return retPri;
	}	
}