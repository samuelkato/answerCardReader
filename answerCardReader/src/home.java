/*
1 0 0 0 1 1 1 0 1 0
1 1 0 0 1 1 1 0 1 1
1 0 1 1 0 0 0 0 1 1
0 0 0 0 0 0 0 0 1 0
0 1 1 0 1 0 0 0 1 1
0 1 0 0 1 0 0 0 1 0
1 0 1 1 0 0 0 0 0 1
0 0 0 1 0 0 0 0 1 0
0 1 1 0 0 1 0 0 1 0
0 0 1 0 0 0 0 0 1 0

1	0	1	0	0	0	0	1	0	1
0	0	0	0	0	1	0	0	0	0
1	1	0	0	1	0	0	1	0	0
0	0	1	1	0	0	0	0	0	1
1	0	0	1	1	0	1	1	1	0
0	0	1	1	0	0	0	1	0	0
0	0	0	0	1	1	1	0	0	0
0	1	0	0	0	0	0	0	0	0
0	1	0	1	1	1	0	1	0	0
0	0	0	0	0	0	1	0	0	1

 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;


public class home {
	public static Scanner in=new Scanner(System.in);
	public static void main(String[] args) {
		
		long startTime = System.nanoTime();
		
		ImageProcessing clImg=null;
		try {
			BufferedImage file = ImageIO.read(new File(in.nextLine()));
			//BufferedImage file = ImageIO.read(new File("d:/Desktop/tmpRed.png"));
			
			int width=file.getWidth();
			int height=file.getHeight();
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
			
			//ImageIO.write(resizedImage,"bmp",new File("d:/Desktop/tmp4.bmp"));
			
			clImg=new ImageProcessing(resizedImage);
			//clImg.m=clImg.erode(clImg.m);
			//clImg.m=clImg.erode(clImg.m);
			//clImg.m=clImg.dilate(clImg.m);
			//clImg.m=clImg.dilate(clImg.m);
		} catch (Exception e) {e.printStackTrace();}
		
		int[][]res=clImg.bwlabel(clImg.m);
		List<Hashtable<String,Integer>> reg=clImg.regionProps(res);
		for (int i=0;i<reg.size();i++) {
			int width=reg.get(i).get("maxx")-reg.get(i).get("minx");
			int height=reg.get(i).get("maxy")-reg.get(i).get("miny");
			float prop=(float)width/height;
			if(reg.get(i).get("area")<50 || (prop < 0.1 && prop > 10)){
				reg.remove(i);
				i--;
			}
		}
		//System.out.println(reg.size());
		
		
		res=clImg.bwlabel(clImg.m2);
		List<Hashtable<String,Integer>> reg2=clImg.regionProps(res);
		
		//clImg.saveFilteredImage("d:/desktop/tmpSaida.png",clImg.m);
		
		
		procurarLinhaClock(reg);
		float tempo=((float)System.nanoTime() - startTime)/1000000000;
		System.out.println("<cartao tempo='"+tempo+"'>");
		lerCartao(reg,reg2);
		System.out.println("</cartao>");
		
	}
	public static void lerCartao(List<Hashtable<String,Integer>> reg,List<Hashtable<String,Integer>> regSuja){
		Hashtable<String,Integer> ini=reg.get(0);
		Hashtable<String,Integer> fim=null;
		int lastClock=0;
		int largura=0;
		
		while(reg.get(lastClock).containsKey("clock"))lastClock++;
		fim=reg.get(lastClock-1);
		double a90=-(double)(fim.get("centrox")-ini.get("centrox"))/(double)(fim.get("centroy")-ini.get("centroy"));
		
		int maxd=0;
		int biggest=0;
		for(int j=0;j<lastClock;j++){
			Hashtable<String,Integer> clock=reg.get(j);
			double b=clock.get("centroy")-a90*clock.get("centrox");
			
			for(int i=0;i<regSuja.size();i++){
				int yAt=regSuja.get(i).get("centroy");
				int xAt=regSuja.get(i).get("centrox");
				int ypos=(int)(xAt*a90+b);
				if(yAt<ypos+20 && yAt>ypos-20){
					int yd=regSuja.get(i).get("centroy")-clock.get("centroy");
					int xd=regSuja.get(i).get("centrox")-clock.get("centrox");
					int d=(int)Math.sqrt((xd)*(xd)+(yd)*(yd));
					if(maxd<d)maxd=d;
				}
				if(maxd>biggest)biggest=maxd;
			}
			largura+=maxd;
		}
		largura/=(lastClock);
		
		
		for(int j=0;j<lastClock;j++){
			Hashtable<String,Integer> clock=reg.get(j);
			double b=clock.get("centroy")-a90*clock.get("centrox");
			System.out.println("<clock ordinal='"+(j+1)+"'>");
			
			//System.out.println(j+")"+clock.get("ID")+" "+clock.get("area")+" "+clock.get("centrox")+":"+clock.get("centroy"));
			for(int i=lastClock;i<reg.size();i++){
				int yAt=reg.get(i).get("centroy");
				int xAt=reg.get(i).get("centrox");
				int ypos=(int)(xAt*a90+b);
				if(yAt<ypos+10 && yAt>ypos-10){
					int yd=reg.get(i).get("centroy")-clock.get("centroy");
					int xd=reg.get(i).get("centrox")-clock.get("centrox");
					int d=(int)Math.sqrt((xd)*(xd)+(yd)*(yd));
					int pos=(int)Math.round(d/(double)(largura/12));
					System.out.println("<marca pos='"+pos+"'></marca>");
				}
			}
			System.out.println("</clock>");
		}
	}
	public static int fx(int x,double a,double b){
		return (int)(x*a+b);
	}
	
	//precisa melhorar horrores ainda.
	//principal cagada: usa 2 pontos mto proximos para traçar uma reta
	public static boolean procurarLinhaClock(List<Hashtable<String,Integer>> reg){
		//remove the 0s region
		reg.remove(0);
		//get left most region
		Hashtable<String,Integer> lmostP=reg.get(0);
		for(int i=0;i<reg.size();i++){
			if(reg.get(i).get("centrox")<lmostP.get("centrox")){
				lmostP=reg.get(i);
			}
		}
		
		//get closest region
		Hashtable<String,Integer> closestP=null;
		int d=0;
		int yd=0;
		for(int i=0;i<reg.size();i++){
			if(reg.get(i)!=lmostP){
				if(closestP==null){
					closestP=reg.get(i);
					yd=reg.get(i).get("centroy")-lmostP.get("centroy");
					int xd=lmostP.get("centrox")-reg.get(i).get("centrox");
					d=(int)Math.sqrt((xd)*(xd)+(yd)*(yd));
				}else{
					int ydCand=reg.get(i).get("centroy")-lmostP.get("centroy");
					int xd=lmostP.get("centrox")-reg.get(i).get("centrox");
					int dCand=(int)Math.sqrt(xd*xd+ydCand*ydCand);
					if(dCand<d){
						closestP=reg.get(i);
						d=dCand;
						yd=ydCand;
					}
				}
			}
		}
		
		
		double a=(double)(lmostP.get("centrox")-closestP.get("centrox"))/(double)(lmostP.get("centroy")-closestP.get("centroy"));
		double b=(double)closestP.get("centrox")-a*(double)closestP.get("centroy");
		
		if(a<1 && a>-1){//usar y
			Collections.sort(reg,new Sorter("y",a>0?1:-1));
		}else{//usar x
			Collections.sort(reg,new Sorter("x",1));
		}
		
		int margem=(int)(d)/2;
		int margemA=lmostP.get("area")*3;
		int posLmost=0;while(reg.get(posLmost)!=lmostP)posLmost++;
		lmostP.put("clock", 1);
		
		for(int i=posLmost+1;i<reg.size();i++){//pra um lado
			int nextx=(int)(reg.get(i).get("centroy")*a+b);
			int areaAt=reg.get(i).get("area");
			if(reg.get(i).get("centrox")>nextx-margem && reg.get(i).get("centrox")<nextx+margem && areaAt>lmostP.get("area")-margemA && areaAt<lmostP.get("area")+margemA){
				reg.get(i).put("clock", 1);
				a=(double)(lmostP.get("centrox")-reg.get(i).get("centrox"))/(double)(lmostP.get("centroy")-reg.get(i).get("centroy"));
				b=reg.get(i).get("centrox")-a*reg.get(i).get("centroy");
				margemA=reg.get(i).get("area")*3;
			}
		}

		margemA=lmostP.get("area")*3;
		b=lmostP.get("centrox")-a*lmostP.get("centroy");
		for(int i=posLmost-1;i>=0;i--){//pra outro lado
			int nextx=(int)(reg.get(i).get("centroy")*a+b);
			int areaAt=reg.get(i).get("area");
			if(reg.get(i).get("centrox")>nextx-margem && reg.get(i).get("centrox")<nextx+margem && areaAt>lmostP.get("area")-margemA && areaAt<lmostP.get("area")+margemA){
				reg.get(i).put("clock", 1);
				b=reg.get(i).get("centrox")-a*reg.get(i).get("centroy");
			}
		}
		
		if(a<1 && a>-1){//usar y
			Collections.sort(reg,new Sorter("y",1));
		}else{//usar x
			Collections.sort(reg,new Sorter("x",1));
		}
		return true;
	}

}


