import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.Scanner;
import javax.imageio.ImageIO;


import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeReader;


public class home {
	public static Scanner in=new Scanner(System.in);
	public static void main(String[] args) {
		
		long startTime = System.nanoTime();
		BufferedImage file=null;
		try {
			//file = ImageIO.read(new File(in.nextLine()));
			//file = ImageIO.read(new File("d:/Desktop/uia/CCF14012013_0005.jpg"));
			file = ImageIO.read(new File("d:/Desktop/005.jpg"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String qr=lerQr(file);
		
		BufferedImage resizedImage=criarImagemRedimensionada(file);
		
		ImageProcessing clImg=new ImageProcessing(resizedImage);
		//clImg.m=clImg.erode(clImg.m);
		clImg.m=clImg.dilate(clImg.m);
		//clImg.saveFilteredImage("d:/desktop/tmpSaida.png",clImg.m);
		List<Hashtable<String,Integer>> reg=filterRegions(clImg);
		//List<Hashtable<String,Integer>> reg2=clImg.regionProps(clImg.bwlabel(clImg.m2));
		
		
		
		
		
		float tempo=((float)System.nanoTime() - startTime)/1000000000;
		System.out.println("<cartao tempo='"+tempo+"' qr='"+qr+"'>");
		procurarLinhaClock(reg);
		System.out.println("</cartao>");
		
	}
	
	public static List<Hashtable<String,Integer>> filterRegions(ImageProcessing clImg){
		List<Hashtable<String,Integer>> reg=clImg.regionProps(clImg.bwlabel(clImg.m));
		for (int i=0;i<reg.size();i++) {
			int width=reg.get(i).get("maxx")-reg.get(i).get("minx");
			int height=reg.get(i).get("maxy")-reg.get(i).get("miny");
			float prop=(float)width/height;
			if(reg.get(i).get("area")<50 || (prop < 0.1 && prop > 10)){
				reg.remove(i);
				i--;
			}
		}
		return reg;
	}
	public static BufferedImage criarImagemRedimensionada(BufferedImage file){
		int width=file.getWidth();
		int height=file.getHeight();
		if(width<=1000 && height<=1000)return file;
		if(width>height){
			height/=(float)width/1000;
			width=1000;
		}else{
			width/=(float)height/1000;
			height=1000;
		}
		BufferedImage resizedImage = new BufferedImage(width, height, 5);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(file, 0, 0, width, height, null);
		g.dispose();
		/*
		try {
			ImageIO.write(resizedImage, "jpg", new File("d:/Desktop/tmp.jpg"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		return resizedImage;
	}
	public static String lerQr(BufferedImage file){
		//achar a regiao especifica do qr code
		String result="";
		LuminanceSource source = new BufferedImageLuminanceSource(file);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		QRCodeReader reader=new QRCodeReader();
		try{
			result=reader.decode(bitmap).getText();
		}catch (Exception e) {
			result=e.getMessage();
		}
		return result;
	}
	public static int fx(int x,double a,double b){
		return (int)(x*a+b);
	}
	
	//precisa melhorar horrores ainda.
	//principal cagada: usa 2 pontos mto proximos para traçar uma reta
	public static boolean procurarLinhaClock(List<Hashtable<String,Integer>> reg){
		//remove the 0s region
		Collections.sort(reg,new Sorter("area",-1));
		List<Hashtable<String,Integer>> list2point=new Vector<Hashtable<String,Integer>>();
		Hashtable<String,Integer> ponto1,ponto2,ponto3=null;
		while(true){
			reg.remove(0);
			
			int a1=reg.get(0).get("area");
			int a2=reg.get(1).get("area");
			int a3=reg.get(2).get("area");
			//logica para checkar shape do ponto
			int avg=(a1+a2+a3)/3;
			int avgBig=(int)(avg*1.1);
			int avgSmall=(int)(avg*0.9);
			if(a1>avgBig || a1<avgSmall || a2>avgBig || a2<avgSmall || a3>avgBig || a3<avgSmall){
				continue;
			}
			//logica para conferir angulo reto entre os 2 pontos
			
			list2point.add(reg.remove(0));
			list2point.add(reg.remove(0));
			list2point.add(reg.remove(0));
			Collections.sort(list2point,new Sorter("centroy",-1));
			ponto1=list2point.remove(0);
			Collections.sort(list2point,new Sorter("centrox",1));
			ponto2=list2point.remove(0);
			ponto3=list2point.remove(0);
			break;
			//essa logica precisa ser melhorada horrores
		}
		
		List<Hashtable<String,Integer>> col1=new Vector<Hashtable<String,Integer>>();
		List<Hashtable<String,Integer>> col2=new Vector<Hashtable<String,Integer>>();
		//linha entre ponto1 e ponto2
		double a=(double)(ponto1.get("centrox")-ponto2.get("centrox"))/(double)(ponto1.get("centroy")-ponto2.get("centroy"));
		double b=(double)ponto1.get("centrox")-a*(double)ponto1.get("centroy");
		double b2=(double)ponto3.get("centrox")-a*(double)ponto3.get("centroy");
		
		for(int i=0;i<reg.size();i++){
			int x=(int)(reg.get(i).get("centroy")*a+b);
			int x2=(int)(reg.get(i).get("centroy")*a+b2);
			int xAt=reg.get(i).get("centrox");
			if(x+5>xAt && x-5<xAt){//coluna1
				col1.add(reg.remove(i--));
			}else if(x2+5>xAt && x2-5<xAt){//coluna2
				col2.add(reg.remove(i--));
			}
		}
		Collections.sort(col1,new Sorter("centroy",1));
		Collections.sort(col2,new Sorter("centroy",1));
		if(col1.size()!=col2.size())System.out.println("tamanho da coluna dos clocks invalido");
		//check number of questions
		Hashtable<Integer,List<String>> resp=new Hashtable<Integer,List<String>>();
		for(int i=0;i<col1.size();i++){
			Hashtable<String,Integer> clock1=col1.get(i);
			Hashtable<String,Integer> clock2=col2.get(i);
			//int distx=clock2.get("centrox")-clock1.get("centrox");
			double aClock=(double)(clock2.get("centroy")-clock1.get("centroy"))/(double)(clock2.get("centrox")-clock1.get("centrox"));
			double bClock=(double)clock1.get("centroy")-aClock*(double)clock1.get("centrox");
			double clockDistX=((double)(clock2.get("centrox")-clock1.get("centrox")))/25;
			for(int j=0;j<reg.size();j++){
				int y=(int)(reg.get(j).get("centrox")*aClock+bClock);
				int yAt=reg.get(j).get("centroy");
				if(y+5>yAt && y-5<yAt){//ponto entre 2 clocks
					int pos=(int)Math.round((reg.get(j).get("centrox")-clock1.get("centrox"))/clockDistX);
					char letra=(char)('a'-1+(pos-1)%6);
					int quesN=(pos-1)/6*col1.size()+i;
					if(resp.containsKey(quesN)){
						resp.get(quesN).add(letra+"");
					}else{
						List<String>lista=new Vector<String>();
						lista.add(letra+"");
						resp.put(quesN, lista);
					}
				}
			}
		}
		
		for (int i=0;i<col1.size()*4;i++) {
			
			System.out.print("<questao ordinal='"+(i+1)+"' letras='");
			if(resp.get(i)!=null){
				for(int j=0;j<resp.get(i).size();j++){
					if(j>0)System.out.print(",");
					System.out.print(resp.get(i).get(j));
				}
			}
			System.out.println("'></questao>");
		}
		return true;
	}

}

class Sorter implements Comparator<Hashtable<String,Integer>>{
	private String chave;
	private int order=1;
	public Sorter(String chave,int order){
		this.chave=chave;
		this.order*=order;
	}
	@Override
	public int compare(Hashtable<String, Integer> o1,Hashtable<String, Integer> o2) {
		int retPri=-1*this.order;
		int retSec=1*this.order;
		if(o1.containsKey("clock") && !o2.containsKey("clock"))return -1;
		else if(!o1.containsKey("clock") && o2.containsKey("clock"))return 1;
		else if(o1.get(this.chave)>o2.get(this.chave))return retSec;
		else if(o1.get(this.chave)==o2.get(this.chave))return 0;
		else return retPri;
	}	
}
