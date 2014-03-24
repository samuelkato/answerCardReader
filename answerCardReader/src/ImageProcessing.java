import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;


public class ImageProcessing {
	BufferedImage img=null;
	int rAvg,gAvg,bAvg,width,height;
	int[][][] oRgb=null;
	public ImageProcessing(BufferedImage img) {
		this.width = img.getWidth();
		this.height=img.getHeight();
		
		this.oRgb=new int[height][width][3];
		long red=0;
		long green=0;
		long blue=0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = img.getRGB(x, y);
				this.oRgb[y][x][0]=(color & 0x00ff0000) >> 16;
				this.oRgb[y][x][1]=(color & 0x0000ff00) >> 8;
				this.oRgb[y][x][2]=(color & 0x000000ff);
				red+=this.oRgb[y][x][0];
				green+=this.oRgb[y][x][1];
				blue+=this.oRgb[y][x][2];
			}
			red/=height;
			green/=height;
			blue/=height;
		}
		this.rAvg=(int)red;
		this.gAvg=(int)green;
		this.bAvg=(int)blue;
	}
	public boolean[][] createMatrix(){
		return createMatrix(new ConfigImageProcessing());
	}
	public boolean[][] createMatrix(ConfigImageProcessing clComp){
		boolean[][] m=new boolean[this.height][this.width];
		
		//System.out.println(red+","+green+","+blue);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int r=this.oRgb[y][x][0];
				int g=this.oRgb[y][x][1];
				int b=this.oRgb[y][x][2];
				m[y][x]=clComp.checkThreshold(r, g, b, this.rAvg, this.gAvg, this.bAvg);
			}
		}
		return m;
	}

	public void saveFilteredImage(String fileName,boolean[][] m){
		int height=m.length;
		int width=m[0].length;
		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if(m[y][x]/* && reg[res[y][x]][0]>400*/){
					img.setRGB(x, y,0);
				}else{
					img.setRGB(x, y,0xffffff);
				}
			}
		}
		try{
			ImageIO.write(img,"bmp",new File(fileName));
		}catch (Exception e) {
			
		}
	}
	public void saveFilteredImage(String fileName,int[][] m){
		int height=m.length;
		int width=m[0].length;
		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		/*int[] cor=new int[100];
		for(int i=0;i<100;i++){
			cor[i]=(int)(Math.random()*255);
			cor[i]=cor[i]<<8;
			cor[i]=cor[i]|(int)(Math.random()*255);
			cor[i]=cor[i]<<8;
			cor[i]=cor[i]|(int)(Math.random()*255);
		}*/
		int[] cor=new int[100];
		cor[0]=8381564;
		cor[1]=0xff0000;
		cor[2]=0x00ff00;
		cor[3]=0x0000ff;
		cor[4]=0x00FFff;
		cor[5]=0xFF00ff;
		cor[6]=0xFFff00;
		cor[7]=6637909;
		cor[8]=7314075;
		cor[9]=0xffffff;
		cor[10]=0x00ff00;
		cor[11]=0x00ff00;
		cor[12]=0x00ff00;
		cor[13]=0x00ff00;
		cor[14]=0xffffff;
		cor[15]=0xffffff;
		cor[16]=0;
		cor[17]=5291567;
		cor[18]=4002483;
		cor[19]=13587305;
		cor[20]=1939733;
		cor[21]=13133125;
		cor[22]=3929930;
		cor[23]=5385166;
		cor[24]=1006952;
		cor[25]=11338835;
		cor[26]=13080858;
		cor[27]=4629900;
		cor[28]=9044988;
		cor[29]=4562419;
		cor[30]=9233338;
		cor[31]=10526393;
		cor[32]=12634977;
		cor[33]=15569486;
		cor[34]=12125792;
		cor[35]=11446530;
		cor[36]=1185377;
		cor[37]=6779463;
		cor[38]=14251419;
		cor[39]=4371168;
		cor[40]=105668;
		cor[41]=9456072;
		cor[42]=5782724;
		cor[43]=5806149;
		cor[44]=14925841;
		cor[45]=14986550;
		cor[46]=9797982;
		cor[47]=1238791;
		cor[48]=15306702;
		cor[49]=4985815;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if(m[y][x]!=0){
					img.setRGB(x, y,cor[m[y][x]]);
				}else{
					img.setRGB(x, y,0);
				}
			}
		}
		try{
			ImageIO.write(img,"bmp",new File(fileName));
		}catch (Exception e) {
			
		}
	}
	
	// Correct, but creates a copy of the image which is inefficient
	boolean[][] dilate(boolean[][] image){
		boolean[][] imagecopy = new boolean[image.length][image[0].length];
		for (int i=0; i<image.length; i++){
			for (int j=0; j<image[i].length; j++){
				if (image[i][j]){
					imagecopy[i][j] = true;
					if (i>0) imagecopy[i-1][j] = true;
					if (j>0) imagecopy[i][j-1] = true;
					if (i+1<image.length) imagecopy[i+1][j] = true;
					if (j+1<image[i].length) imagecopy[i][j+1] = true;
				}
			}
		}
		return imagecopy;
	}
	// Correct, but creates a copy of the image which is inefficient
	boolean[][] erode(boolean[][] image){
		boolean[][] imagecopy = new boolean[image.length][image[0].length];
		for (int i=0; i<image.length; i++)for (int j=0; j<image[i].length; j++)imagecopy[i][j]=true;
		for (int i=0; i<image.length; i++){
			for (int j=0; j<image[i].length; j++){
				if (!image[i][j]){
					imagecopy[i][j] = false;
					if (i>0) imagecopy[i-1][j] = false;
					if (j>0) imagecopy[i][j-1] = false;
					if (i+1<image.length) imagecopy[i+1][j] = false;
					if (j+1<image[i].length) imagecopy[i][j+1] = false;
				}
			}
		}
		return imagecopy;
	}
	
	public int[][] bwlabel(){
		return bwlabel(createMatrix());
	}
	public int[][] bwlabel(boolean[][] m){
		int height=m.length;
		int width=m[0].length;
		int[][] res=new int[height][width];
		
		int gAt=0;
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				if(m[i][j]){
					if(i>0 && res[i-1][j]!=0){//N
						res[i][j]=res[i-1][j];
					}else if(j>0 && res[i][j-1]!=0){//W
						res[i][j]=res[i][j-1];
					}else if(i>0 && j>0 && res[i-1][j-1]!=0){//NW
						res[i][j]=res[i-1][j-1];
					}else if(i>0 && j<width-1 && res[i-1][j+1]!=0){//NE
						res[i][j]=res[i-1][j+1];
					}else{
						res[i][j]=++gAt;
					}
				}else{
					res[i][j]=0;
				}
			}
		}
		//*
		int[] rplc=new int[gAt+1];

		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				if(res[i][j]==0)continue;
				juntaGrupos(res,rplc,i,j,+1,+0);//N 1
				juntaGrupos(res,rplc,i,j,+0,+1);//E 2
				juntaGrupos(res,rplc,i,j,-1,+0);//S 3
				juntaGrupos(res,rplc,i,j,+0,-1);//W 4
				juntaGrupos(res,rplc,i,j,+1,+1);//NE 5
				juntaGrupos(res,rplc,i,j,-1,+1);//SE 6
				juntaGrupos(res,rplc,i,j,-1,-1);//SW 7
				juntaGrupos(res,rplc,i,j,+1,-1);//NW 8
			}
		}
		
		int[] rplc2=new int[gAt+1];
		int ind=0;
		for(int i=1;i<=gAt;i++)if(rplc[i]==0){
			rplc2[i]=++ind;
		}
		

		for(int i=0;i<height;i++)for(int j=0;j<width;j++)if(res[i][j]!=0){
			int newG=rplc[res[i][j]];
			while(rplc[newG]!=0)newG=rplc[newG];
			if(newG!=0){
				res[i][j]=newG;
			}
			res[i][j]=rplc2[res[i][j]];
		}
		
		return res;
	}
	public static void juntaGrupos(int[][] res,int[] rplc,int i,int j,int i2,int j2){
		i2+=i;j2+=j;
		if(i2>=0 && j2>=0 && i2<res.length && j2<res[0].length){
			if(res[i][j]!=res[i2][j2] && res[i2][j2]!=0){
				int min=Math.min(res[i][j],res[i2][j2]);
				int max=Math.max(res[i][j],res[i2][j2]);
				while(rplc[min]!=0)min=rplc[min];
				while(rplc[max]!=0)max=rplc[max];
				if(min>max)rplc[min]=max;
				else if(min<max) rplc[max]=min;
			}
		}
	}
	public List<Region> regionProps(){
		return regionProps(this.bwlabel());
	}
	public List<Region> regionProps(int[][] res){
		List<Region> ret = new Vector<Region>();
		int height=res.length;
		int width=res[0].length;
		
		int nRegions=0;
		for(int i=0;i<height;i++)for(int j=0;j<width;j++)if(res[i][j]>nRegions)nRegions=res[i][j];
		nRegions++;
		long[][] tmp=new long[nRegions][7];

		for(int i=0;i<nRegions;i++){
			tmp[i][3]=width+1;
			tmp[i][4]=height+1;
			tmp[i][5]=-1;
			tmp[i][6]=-1;
		}
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				tmp[res[i][j]][0]++;
				tmp[res[i][j]][1]+=j;
				tmp[res[i][j]][2]+=i;
				if(j<tmp[res[i][j]][3])tmp[res[i][j]][3]=j;
				if(i<tmp[res[i][j]][4])tmp[res[i][j]][4]=i;
				if(j>tmp[res[i][j]][3])tmp[res[i][j]][5]=j;
				if(i>tmp[res[i][j]][4])tmp[res[i][j]][6]=i;
			}
		}
		for(int i=0;i<nRegions;i++){
			tmp[i][1]=(tmp[i][1]/tmp[i][0]);
			tmp[i][2]=(tmp[i][2]/tmp[i][0]);
			
			Region a=new Region((int)tmp[i][0],(int)tmp[i][1],(int)tmp[i][2],(int)tmp[i][3],(int)tmp[i][4],(int)tmp[i][5],(int)tmp[i][6],(int)i);
			ret.add(a);
		}
		
		return ret;
	}
	public List<Region> filterRegions(List<Region> reg){
		return filterRegions(reg,new ConfigImageProcessing());
	}
	public List<Region> filterRegions(List<Region> reg,ConfigImageProcessing config){
		for (int i=0;i<reg.size();i++) {
			if(!config.checkRegion(reg.get(i))){
				reg.remove(i);
				i--;
			}
		}
		return reg;
	}
}
class ConfigImageProcessing{
	boolean invertThreshold_=false;
	int minArea=50;
	long maxArea=999999999;
	//true => preto
	//false => branco
	public boolean checkThreshold(int r, int g, int b, int rAvg, int gAvg, int bAvg){
		//return !(r>g+20 && r>b+20) && r+g+b800;
		boolean ret=r+g+b<rAvg+gAvg+bAvg-80 && !(r>g+10 && r>b+10 && r>150);
		if(!invertThreshold_)return ret;
		else return !ret;
		//return !(r+g+b>700 || r>180);
	}
	public boolean checkRegion(Region regAt){
		if(regAt.area<this.minArea || regAt.area>this.maxArea)return false;
		int width=regAt.maxx-regAt.minx;
		int height=regAt.maxy-regAt.miny;
		float prop=(float)width/height;
		if(prop < 0.1 && prop > 10)return false;
		return true;
	}
}
class Region{
	int area,centrox,centroy,minx,miny,maxx,maxy,ID;
	boolean clock_=false;
	List<int[]> points=null;//nao esta sendo usado
	Region(int area,int centrox,int centroy,int minx,int miny,int maxx,int maxy,int ID){
		this.area=area;
		this.centrox=centrox;
		this.centroy=centroy;
		this.minx=minx;
		this.miny=miny;
		this.maxx=maxx;
		this.maxy=maxy;
		this.ID=ID;
	}
	int getFieldInt(String field){
		switch(field){
		case "area":return this.area;
		case "centrox":return this.centrox;
		case "centroy":return this.centroy;
		case "minx":return this.minx;
		case "miny":return this.miny;
		case "maxx":return this.maxx;
		case "maxy":return this.maxy;
		case "ID":return this.ID;
		}
		return 0;
	}
	
	@Override
   public String toString() {
     return "\n-------\narea:"+this.area+"\nx:"+this.centrox+"\ny:"+this.centroy;
   }
}