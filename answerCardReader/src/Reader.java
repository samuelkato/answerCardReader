import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;


import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeReader;


public class Reader {
	public static Scanner in=new Scanner(System.in);
	public static void main(String[] args) {
		
		long startTime = System.nanoTime();
		BufferedImage file=null;
		try {
			file = ImageIO.read(new File(in.nextLine()));
			//file = ImageIO.read(new File("d:/Desktop/uia/CCF14012013_0005.jpg"));
			//file = ImageIO.read(new File("d:/Desktop/folhaRespostas/006.jpg"));
			//file = ImageIO.read(new File("d:/Desktop/cartao.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String qr=lerQr(file);
		
		BufferedImage resizedImage=criarImagemRedimensionada(file);
		ImageProcessing clImg=new ImageProcessing(resizedImage);
		
		
		
		
		
		float tempo=((float)System.nanoTime() - startTime)/1000000000;
		System.out.println("<cartao tempo='"+tempo+"' qr='"+qr+"'>");
		lerCartao(clImg);
		System.out.println("</cartao>");
		
	}
	
	public static List<Region> procurar3pontos(ImageProcessing clImg){
		ConfigImageProcessing config=new ConfigImageProcessing();
		config.invertThreshold_=true;
		config.minArea=600;
		boolean[][] mInv=clImg.createMatrix(config);
		int[][] bwInv=clImg.bwlabel(mInv);
		//clImg.saveFilteredImage("d:/desktop/tmpSaida.png",mInv);
		//clImg.saveFilteredImage("d:/desktop/tmpSaida2.png",bwInv);
		List<Region> regInv=clImg.regionProps(bwInv);
		
		regInv.remove(0);
		regInv.remove(0);
		regInv=clImg.filterRegions(regInv,config);
		List<Region> ret=new Vector<Region>();
		List<int[]> sorter=new Vector<int[]>();
		for(int i=0;i<regInv.size();i++){
			Region regAt=regInv.get(i);
			int []a=new int[4];
			a[0]=i;
			a[1]=regAt.centroy-regAt.centrox;
			a[2]=-regAt.centroy-regAt.centrox;
			a[3]=regAt.centrox-regAt.centroy;
			sorter.add(a);
		}
		Collections.sort(sorter,new Sorter2(1,-1));
		ret.add(regInv.get(sorter.remove(0)[0]));
		Collections.sort(sorter,new Sorter2(2,-1));
		ret.add(regInv.get(sorter.remove(0)[0]));
		ret.add(regInv.get(sorter.remove(0)[0]));
		
		
		//check perpendicular
		return ret;
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
	public static BufferedImage criarRegiaoQr(BufferedImage file){
		int width=file.getWidth();
		int height=file.getHeight();
		BufferedImage resizedImage = new BufferedImage(400, 400, 5);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(file, 0, 0, width/2, height/2, null);
		g.dispose();
		
		/*try {
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
		LuminanceSource source = new BufferedImageLuminanceSource(criarRegiaoQr(file));
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
	
	public static boolean lerCartao(ImageProcessing clImg){
		boolean[][] m=clImg.createMatrix();
		//m=clImg.erode(m);
		//clImg.m=clImg.erode(clImg.m);
		//clImg.m=clImg.dilate(clImg.m);
		List<Region> reg=clImg.regionProps(clImg.bwlabel(m));
		List<Region> pontosRef=procurar3pontos(clImg);
		
		Region ponto1=pontosRef.get(0);
		Region ponto2=pontosRef.get(1);
		Region ponto3=pontosRef.get(2);
		
		List<Region> col1=new Vector<Region>();
		List<Region> col2=new Vector<Region>();
		//linha entre ponto1 e ponto2
		double a=(double)(ponto1.centrox-ponto2.centrox)/(double)(ponto1.centroy-ponto2.centroy);
		double b=(double)ponto1.centrox-a*(double)ponto1.centroy;
		double b2=(double)ponto3.centrox-a*(double)ponto3.centroy;
		for(int i=0;i<reg.size();i++){
			int yAt=reg.get(i).centroy;
			int xAt=reg.get(i).centrox;
			if(yAt<ponto2.centroy+20 || yAt>ponto1.centroy-20)continue;
			int x=(int)(yAt*a+b);
			int x2=(int)(yAt*a+b2);
			if(x+5>xAt && x-5<xAt){//coluna1
				col1.add(reg.remove(i--));
			}else if(x2+5>xAt && x2-5<xAt){//coluna2
				col2.add(reg.remove(i--));
			}
		}
		Collections.sort(col1,new Sorter("centroy",1));
		Collections.sort(col2,new Sorter("centroy",1));
		if(col1.size()!=col2.size())System.out.printf("tamanho da coluna dos clocks invalido col1:%d col2:%d\n",col1.size(),col2.size());
		//check number of questions
		Hashtable<Integer,List<String>> resp=new Hashtable<Integer,List<String>>();
		for(int i=0;i<col1.size();i++){
			Region clock1=col1.get(i);
			Region clock2=col2.get(i);
			//int distx=clock2.centrox-clock1.centrox;
			double aClock=(double)(clock2.centroy-clock1.centroy)/(double)(clock2.centrox-clock1.centrox);
			double bClock=(double)clock1.centroy-aClock*(double)clock1.centrox;
			double clockDistX=((double)(clock2.centrox-clock1.centrox))/25;
			for(int j=1;j<25;j++){
				int x=(int)(clockDistX*j+clock1.centrox);
				int y=(int)(x*aClock+bClock);
				if(conferirMarcacao(x,y,m)){
					int quesN=(j-1)/6*col1.size()+i;
					char letra=(char)('a'-1+(j-1)%6);
					if(resp.containsKey(quesN)){
						resp.get(quesN).add(letra+"");
					}else{
						List<String>lista=new Vector<String>();
						lista.add(letra+"");
						resp.put(quesN, lista);
					}
				}
			}
			/*System.exit(0);
			for(int j=0;j<reg.size();j++){
				int y=(int)(reg.get(j).centrox*aClock+bClock);
				int yAt=reg.get(j).centroy;
				if(y+5>yAt && y-5<yAt){//ponto entre 2 clocks
					int pos=(int)Math.round((reg.get(j).centrox-clock1.centrox)/clockDistX);
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
			}*/
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
	
	public static boolean conferirMarcacao(int x,int y, boolean[][] m){
		int cnt=0;
		for(int i=x-3;i<x+3;i++){
			for(int j=y-5;j<y+5;j++){
				if(m[j][i])cnt++;
			}
		}
		return (float)cnt/60>0.1;
	}

}

class Sorter implements Comparator<Region>{
	private String chave;
	private int order=1;
	public Sorter(String chave,int order){
		this.chave=chave;
		this.order*=order;
	}
	@Override
	public int compare(Region o1,Region o2) {
		int retPri=-1*this.order;
		int retSec=1*this.order;
		if(o1.clock_ && !o2.clock_)return -1;
		else if(!o1.clock_ && o2.clock_)return 1;
		else if(o1.getFieldInt(this.chave)>o2.getFieldInt(this.chave))return retSec;
		else if(o1.getFieldInt(this.chave)==o2.getFieldInt(this.chave))return 0;
		else return retPri;
	}	
}
class Sorter2 implements Comparator<int[]>{
	private int chave;
	private int order=1;
	public Sorter2(int chave,int order){
		this.chave=chave;
		this.order*=order;
	}
	@Override
	public int compare(int[] o1,int[] o2) {
		int retPri=-1*this.order;
		int retSec=1*this.order;
		if(o1[this.chave]>o2[this.chave])return retSec;
		else if(o1[this.chave]==o2[this.chave])return 0;
		else return retPri;
	}	
}
