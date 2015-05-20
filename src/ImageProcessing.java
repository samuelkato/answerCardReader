import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;


public class ImageProcessing {
	BufferedImage img=null;
	boolean[][] m=null;
	boolean[][] mInv=null;
	int rAvg,gAvg,bAvg,width,height;
	int[][][] oRgb=null;
	public ImageProcessing(BufferedImage img) {
		this.img = criarImagemRedimensionada(img, 1000);
		this.width = this.img.getWidth();
		this.height = this.img.getHeight();
		this.m = new boolean[this.height][this.width];
		this.mInv = new boolean[this.height][this.width];
		this.oRgb = new int[height][width][3];
		
		long red=0;
		long green=0;
		long blue=0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				long color = (long)this.img.getRGB(x, y);
				this.oRgb[y][x][0] = (int)((color >> 16) & 0xff) ;
				this.oRgb[y][x][1] = (int)((color >> 8) & 0xff) ;
				this.oRgb[y][x][2] = (int)(color & 0xff);
				red += this.oRgb[y][x][0];
				green += this.oRgb[y][x][1];
				blue += this.oRgb[y][x][2];
			}
		}
		this.rAvg = (int)(red/(width*height));
		this.gAvg = (int)(green/(width*height));
		this.bAvg = (int)(blue/(width*height));
	}
	/*
	public void rotate(int ang){
		int[][][] newORgb=new int[width][height][3];
		switch(ang){
		case 90:
			for(int y=0; y<this.height; y++){
				for(int x=0; x<this.width; x++){
					int newy=x, newx=this.height-1-y;
					newORgb[newy][newx]=this.oRgb[y][x];
				}
			}
			break;
		case 180:
			break;
		case -90:
			break;
		default:
			break;
		}
		this.height=newORgb.length;
		this.width=newORgb[0].length;
		
		this.oRgb=newORgb;
		this.m=new boolean[this.height][this.width];
		this.mInv=new boolean[this.height][this.width];
		
		this.createMatrix();
	}
	*/
	public boolean[][] createMatrix(){
		this.m=createMatrix(new ConfigImageProcessing());
		return this.m;
	}
	public boolean[][] createMatrix(ConfigImageProcessing clComp){
		//System.out.println(red+","+green+","+blue);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int r=this.oRgb[y][x][0];
				int g=this.oRgb[y][x][1];
				int b=this.oRgb[y][x][2];
				this.m[y][x]=clComp.checkThreshold(r, g, b, this.rAvg, this.gAvg, this.bAvg);
				this.mInv[y][x]=!this.m[y][x];
			}
		}
		return this.m;
	}
	
	/**
	 * Rotates an image. Actually rotates a new copy of the image.
	 * 
	 * @param img The image to be rotated
	 * @param angle The angle in degrees
	 * @return The rotated image
	 */
	public static BufferedImage rotate(BufferedImage img, int angle) {
		int width=img.getWidth();
		int height=img.getHeight();
		
		//para agilizar nos angulos de 90,180,-90
		if(angle==90){
			BufferedImage imgRot = new BufferedImage(height,width,BufferedImage.TYPE_INT_RGB);
			for(int y=0; y<height; y++){
				for(int x=0; x<width; x++){
					int newy=x, newx=height-1-y;
					imgRot.setRGB(newx, newy, img.getRGB(x, y));
				}
			}
			return imgRot;
		}else if(angle==180){
			BufferedImage imgRot = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
			for(int y=0; y<height; y++){
				for(int x=0; x<width; x++){
					int newy=height-1-y, newx=width-1-x;
					imgRot.setRGB(newx, newy, img.getRGB(x, y));
				}
			}
			return imgRot;
		}else if(angle==-90){
			BufferedImage imgRot = new BufferedImage(height,width,BufferedImage.TYPE_INT_RGB);
			for(int y=0; y<height; y++){
				for(int x=0; x<width; x++){
					int newy=width-1-x, newx=y;
					imgRot.setRGB(newx, newy, img.getRGB(x, y));
				}
			}
			return imgRot;
		}
		
		
		
	    double sin = Math.abs(Math.sin(Math.toRadians(angle))),
	           cos = Math.abs(Math.cos(Math.toRadians(angle)));

	    int w = img.getWidth(null), h = img.getHeight(null);

	    int neww = (int) Math.floor(w*cos + h*sin),
	        newh = (int) Math.floor(h*cos + w*sin);
	    
	    //BufferedImage bimg = toBufferedImage(getEmptyImage(neww, newh));
	    BufferedImage bimg = new BufferedImage(neww, newh, 5);
	    for(int i=0;i<neww;i+=1){
	    	for(int j=0;j<newh;j+=1)bimg.setRGB(i, j, 16777215);
	    }
	    Graphics2D g = bimg.createGraphics();

	    g.translate((neww-w)/2, (newh-h)/2);
	    g.rotate(Math.toRadians(angle), w/2, h/2);
	    g.drawRenderedImage(img, null);
	    g.dispose();
	    
	    return bimg;
	}
	
	/**
	 * cria uma imagem redimensionada a partir de uma imagem de referencia
	 * 
	 * @param file
	 * @return imagem redimensionada
	 */
	public static BufferedImage criarImagemRedimensionada(BufferedImage file, int max){
		int width=file.getWidth();
		int height=file.getHeight();
		
		if(width<=max && height<=max)return file;
		if(width>height){
			height/=(float)width/max;
			width=max;
		}else{
			width/=(float)height/max;
			height=max;
		}
		BufferedImage resizedImage = new BufferedImage(width, height, 5);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(file, 0, 0, width, height, null);
		g.dispose();

		return resizedImage;
	}

	public void saveFilteredImage(String fileName,boolean[][] m){
		BufferedImage img = matrix2img(m);
		
		try{
			ImageIO.write(img,"bmp",new File(fileName));
		}catch (Exception e) {
			
		}
	}
	
	public BufferedImage matrix2img(boolean[][] m){
		int height=m.length;
		int width=m[0].length;
		
		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_BINARY);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if(m[y][x]){
					img.setRGB(x, y,0);
				}else{
					img.setRGB(x, y,0xffffff);
				}
			}
		}
		return img;
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
		int[] cor=new int[3];
		cor[0]=0xffdddd;
		cor[1]=0xddffdd;
		cor[2]=0xddddff;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if(m[y][x]!=0){
					img.setRGB(x, y,cor[m[y][x]%cor.length]);
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
			tmp[i][3]=width+1;//minx
			tmp[i][4]=height+1;//miny
			tmp[i][5]=-1;//maxx
			tmp[i][6]=-1;//maxy
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
	int minArea=50;
	long maxArea=999999999;
	//true => preto
	//false => branco
	public boolean checkThreshold(int r, int g, int b, int rAvg, int gAvg, int bAvg){
		//return !(r>g+20 && r>b+20) && r+g+b800;
		
//		return
//				r+g+b<rAvg+gAvg+bAvg-50		//cores mais escuras q a media-80
//				&& !(r>g+10 && r>b+10 )		//cores vermelhas
//				&& !(r>200 || g>200)		//mais de 200 r ou mais de 200 g
//		;
		//System.out.println(r+" "+rAvg);
		return
				(
					( r < rAvg - 30 || r < 40 )
					&& ( g < gAvg - 30  || g < 40 )
					&& ( b < bAvg - 30 || b < 40 )	//cores mais escuras q a media-20
					&& !( r > g+60 && r > b+60 )	//cores vermelhas
				)
				|| ( b > g+50 && b > r+50 )
		;
		
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