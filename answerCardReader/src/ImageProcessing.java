import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;


public class ImageProcessing {
	public boolean[][]m;
	public boolean[][]m2;
	public int[][]res;
	//deve receber externa para criar a matriz boleana
	public ImageProcessing(BufferedImage img) {
		int height = img.getHeight();
		int width = img.getWidth();

		m=new boolean[height][width];
		//m2=new boolean[height][width];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int color = img.getRGB(x, y);
				int r=(color & 0x00ff0000) >> 16;
				int g=(color & 0x0000ff00) >> 8;
				int b=(color & 0x000000ff);
				//m[y][x]=r<180 || b<200;
				m[y][x]=!(r+g+b>700 || r>180);
			}
		}
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
			// TODO: handle exception
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
				}
			}
		}
		int[] rplc=new int[gAt+1];

		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				if(res[i][j]==0)continue;
				//juntaGrupos(rplc,i,j,+1,+0);//N 1 probably useles
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
		for(int i=1;i<gAt+1;i++)if(rplc[i]==0){
			rplc2[i]=++ind;
		}

		for(int i=0;i<height;i++)for(int j=0;j<width;j++)if(res[i][j]!=0){
			if(rplc[res[i][j]]!=0){
				res[i][j]=rplc[res[i][j]];
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
				int max2=max;
				while(rplc[max]!=0)max=rplc[max];
				if(max!=max2)rplc[max2]=min;
				if(max!=min)rplc[max]=min;
				//
			}
		}
	}
	public List<Hashtable<String,Integer>> regionProps(int[][] res){
		List<Hashtable<String,Integer>> ret = new Vector<Hashtable<String,Integer>>();
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
			
			Hashtable<String,Integer> a=new Hashtable<String,Integer>();
			a.put("area",(int)tmp[i][0]);
			a.put("centrox",(int)tmp[i][1]);
			a.put("centroy",(int)tmp[i][2]);
			a.put("minx",(int)tmp[i][3]);
			a.put("miny",(int)tmp[i][4]);
			a.put("maxx",(int)tmp[i][5]);
			a.put("maxy",(int)tmp[i][6]);
			a.put("ID",(int)i);
			ret.add(a);
		}
		
		return ret;
	}
}
