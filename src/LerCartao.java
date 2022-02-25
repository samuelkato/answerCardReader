import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class LerCartao{
	ImageProcessing clImg;
	//private static Mat imgTemplate=null;
	private Boolean debug_;
	private Mat bw;
	private int tipo=2;//1=>teste 2=>diss
	private double prop;
	private static Mat[] matLetras=null;
	private static String[] aLetras = new String[]{"0","1","2","3","4","5","6","7","8","9","/"};
	String saida = "";
	
	/*public LerCartao(ImageProcessing clImg, ZipOutputStream zipSaida) {
		return;
	}*/
	public LerCartao(ImageProcessing clImg, Pool pool, Boolean debug_){
		this.debug_ = debug_;
		this.clImg=clImg;
		
		BufferedImage file = clImg.imgOriginal;
		this.prop = Math.max(file.getHeight(), file.getWidth())/1000.0;
		String respPag = this.procurarDissertativa(file);
		if(respPag!="") {
			this.saida  = respPag;
			return;
		}
		

		
		

		
		//
		this.clImg.createMatrix();
		//this.clImg.reloadImg2();
		Hashtable<String,String> saidaHt = new Hashtable<String,String>();
		if(this.debug_.booleanValue()){
			clImg.saveFilteredImage(clImg.bwlabel(clImg.m),"-areas");
			clImg.saveFilteredImage(clImg.bwlabel(),"-seila");
		}
		
		try{
			//file=rotate(file,45);
			//long startTime = System.nanoTime();
			//m=clImg.erode(m);
			//clImg.m=clImg.erode(clImg.m);
			//clImg.m=clImg.dilate(clImg.m);
			List<Region> pontosRef = null;
			try{
				pontosRef = procurar3pontos(clImg);
			}catch(ErrAng $e){
				System.out.println("Imagem rotacionada "+$e.ang);
				pool.rotateImg(clImg, $e.ang);
				pontosRef = procurar3pontos(clImg);
			}
			
			String qr = lerQr(clImg, pontosRef);

			saidaHt.put("qr", "\""+qr+"\"");
			System.out.println("qr: "+qr);
			
			List<List<Region>> aCols = getClocks(clImg, pontosRef);
			
			
			boolean[][] m = clImg.m;

			//boolean[][] mPosLeitura = new boolean[m.length][m[0].length];
			m = clImg.erode(m);
			m = clImg.dilate(m);
			//check number of questions
			Hashtable<Integer,List<String>> resp=new Hashtable<Integer,List<String>>();
			int colSize = aCols.get(0).size();
			for(int i=0;i < colSize;i++){
				Region clock1 = aCols.get(0).get(i);
				Region clock2 = aCols.get(1).get(i);
				//int distx=clock2.centrox-clock1.centrox;
				double aClock=(double)(clock2.centroy-clock1.centroy)/(double)(clock2.centrox-clock1.centrox);
				double bClock=(double)clock1.centroy-aClock*(double)clock1.centrox;
				double clockDistX=((double)(clock2.centrox-clock1.centrox))/26;//numero de divisoes entre o clocks
				for(int j=1;j<25;j++){
					if((j-1)%6==0)continue;
					
					int x=(int)(clockDistX*j+clock1.centrox)-1;
					int y=(int)(x*aClock+bClock)+1;

					//mPosLeitura[y][x] = true;
					String marca=String.format(Locale.US, "%1$d",Math.round(conferirMarcacao(x,y,m)*100));

					int quesN=(j-1)/6*colSize+i;
					if(resp.containsKey(quesN)){
						resp.get(quesN).add(marca);
					}else{
						List<String>lista=new Vector<String>();
						lista.add(marca);
						resp.put(quesN, lista);
					}
				}
			}
			//clImg.saveFilteredImage(mPosLeitura,"posLeitura");
			//clImg.saveFilteredImage(m,"bool");
			
			String questoes="[";
			for (int i=0;i<colSize*4;i++) {
				String alts="";
				if(resp.get(i)!=null){
					for(int j=0;j<resp.get(i).size();j++){
						if(j>0)alts+=",";
						alts+=resp.get(i).get(j);
					}
				}
				if(i>0)questoes+=",";
				questoes+=String.format("[%1$s]", alts);
			}
			questoes+="]";
			saidaHt.put("questoes", questoes);
			
		}catch(Exception e){
			saidaHt.put("msg", "\""+e.getMessage()+"\"");
		}
		//saidaHt.put("orig", "\""+clImg.fileName+"\"");
		
		
		Enumeration<String> enumKey = saidaHt.keys();
		while(enumKey.hasMoreElements()){
			String key = enumKey.nextElement();
			String val = saidaHt.get(key);
			if(saida != "") saida += ",";
			saida += "\""+key+"\""+":"+val;
		}
	}
	
	private String procurarDissertativa(BufferedImage file) {
		String qr="";
		String pagina="";
		
		double prop = this.prop;
		this.bw = new Mat();
		
		Mat mat;
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(file, "bmp", byteArrayOutputStream);
			byteArrayOutputStream.flush();
			mat = Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
		}catch (IOException e) {
			// TODO: handle exception
			mat = null;
			System.out.println(e.getMessage());
			return "";
		}
		int w = mat.width();
		int h = mat.height();
		if(w > h) {
			Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
			this.clImg.imgOriginal = ImageProcessing.rotate(this.clImg.imgOriginal, 90);
			w = h;
			h = mat.height();
		}
		Mat gray = new Mat();
		if(mat.channels()==1) {
			gray = mat;
		}else {
			Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
		}
		/*
		Mat qrPoints = new Mat();
		QRCodeDetector qrDetect = new QRCodeDetector();
		System.out.println(qrDetect.detectAndDecodeCurved(gray, qrPoints));
		for(int i = 0; i < qrPoints.cols(); i++) {
			double x = qrPoints.get(0, i)[0];
			double y = qrPoints.get(0, i)[1];
			System.out.println(x+":"+y);
		}
		*/
		
		Imgproc.adaptiveThreshold(gray, this.bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 201, 20);
		//Imgproc.adaptiveThreshold(gray, this.bw, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 12);
		//Imgcodecs.imwrite("C:\\Users\\Samuel Kato\\Desktop\\img diss\\gray.bmp", gray);
		//Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(Math.round(3*prop), Math.round(3*prop)));
		int sizeKernel = 3 + (int)Math.round(1.6*(prop - 1));
		//System.out.println(sizeKernel+" "+this.prop);
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(sizeKernel,sizeKernel));
		//System.out.println(kernel.dump());
		//Mat kernel = Mat.eye(3, 3, CvType.CV_8UC1);
		//kernel.put(0 ,0, 1,1,1, 1, 1, 1, 1,1,1 );
		Mat mDil = this.bw.clone();
		Imgproc.dilate(mDil, mDil, kernel);
		Imgproc.erode(mDil, mDil, kernel);
		Imgproc.erode(mDil, mDil, kernel);
		Imgproc.erode(mDil, mDil, kernel);
		//Imgproc.erode(mDil, mDil, kernel);
		Imgproc.dilate(mDil, mDil, kernel);
		Imgproc.dilate(mDil, mDil, kernel);
		Imgproc.dilate(mDil, mDil, kernel);
		//Imgproc.erode(mDil, mDil, kernel);
		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/dil.bmp", mDil);
		List<MatOfPoint> cntsQr = new ArrayList<>();
		Imgproc.findContours(mDil, cntsQr, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
		Mat matBranca = new Mat(mat.size(),CvType.CV_8U,new Scalar(255));
		double qrx=0;
		double qry=0;
		double qrAngle=0;
		int minAreaQr = (int)(4000*prop*prop);
		for(MatOfPoint cnt : cntsQr) {
			double area = Imgproc.contourArea(cnt,true);
			RotatedRect rrect = Imgproc.minAreaRect(new MatOfPoint2f( cnt.toArray() ));
			double ratio = rrect.size.width/rrect.size.height;
			if(area>minAreaQr && Math.abs(ratio - 1) < 0.3) {
				Moments moments = Imgproc.moments(cnt);
				double x = moments.m10 / moments.m00;
				double y = moments.m01 / moments.m00;
				if((x < w/4 && y > h*3/4) || (x > w*3/4 && y < h/4)) {
					qrAngle = rrect.angle % 90;
					if(qrAngle > 45) qrAngle -= 90;
					
					//desenha qr numa folha para processamento futuro
					List<MatOfPoint> cntsQrTmp = new ArrayList<>();
					cntsQrTmp.add(cnt);
					Imgproc.drawContours(matBranca, cntsQrTmp, -1, new Scalar(0), -1) ;
					qrx = x;
					qry = y;
					qr = lerQr1(mat.submat(Imgproc.boundingRect(cnt)));
					if(qr.length()>0) break;
				}
			}
		}
		if(qrx==0 || qry==0) {//sem regiao do qr,provavel erro de leitura
			this.tipo = 1;
			return "";
		}else if(qrx < w/2 && qry > h/2) {//de ponta cabeça
			Core.rotate(matBranca, matBranca, Core.ROTATE_180);
			Core.rotate(mat, mat, Core.ROTATE_180);
			Core.rotate(gray, gray, Core.ROTATE_180);
			Core.rotate(this.bw, this.bw, Core.ROTATE_180);
			this.clImg.imgOriginal = ImageProcessing.rotate(this.clImg.imgOriginal, 180);
		}
		this.tipo = 2;
		

		// Preparing the kernel matrix object
		
		//Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size((2*2) + 1, (2*2)+1));
		
		
		pagina = this.getNumeroPagina(matBranca);

		Mat bwLinha = new Mat();
		Mat kernel2 = Mat.eye(3, 3, CvType.CV_8UC1);
		kernel2.put(0 ,0, 0,0,0, 1, 1, 1, 0,0,0 );
		Imgproc.dilate(this.bw, bwLinha, kernel2);
		Imgproc.erode(bwLinha, bwLinha, kernel2);
		//Imgproc.erode(bwLinha, bwLinha, kernel);
		//Imgproc.erode(bwLinha, bwLinha, kernel);
		//Imgproc.erode(bwLinha, bwLinha, kernel);
		//Imgproc.erode(bwLinha, bwLinha, kernel);
		//Imgproc.erode(bwLinha, bwLinha, kernel);

		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/bwLinha.bmp", bwLinha);
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(bwLinha, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
		
		List<MatOfPoint> contoursLinhas = new ArrayList<>();
		double lastAng = qrAngle;
		double avgAng = qrAngle;

		//int minAreaLin = (int)(1000*prop*prop);
		//int maxAreaLin = (int)(4000*prop*prop);
		int minWLin = (int)(560*prop);
		int maxWHin = (int)(8*prop);
		for(MatOfPoint cnt : contours) {
			//double area = Imgproc.contourArea(cnt,true);
			//Moments moments = Imgproc.moments(cnt);
			//double cx = moments.m10 / moments.m00;
			//double cy = moments.m01 / moments.m00;
			//if(area<minAreaLin || area>maxAreaLin) continue;
			RotatedRect rrect = Imgproc.minAreaRect(new MatOfPoint2f( cnt.toArray() ));
			double angle = rrect.angle % 90;
			if(angle > 45) angle -= 90;
			if(angle > 10 || angle < -10) continue;
			double rw = Math.max(rrect.size.width,rrect.size.height);
			double rh = Math.min(rrect.size.width,rrect.size.height);
			if(rw < minWLin || rh > maxWHin) continue;
			if(Math.abs(lastAng - angle) > 1) continue;
			contoursLinhas.add(cnt);
			lastAng = angle;
			avgAng += angle;
			//System.out.println("angulo:"+(angle)+" w:"+rw+" h:"+rh+" center:"+cx+","+cy+" "+area);
		}
		


		
		

		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/bw.bmp", this.bw);
		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/template.bmp", imgTemplate);
		//System.out.println(avgAng);
		//rotacionar
		//talvez precise rotacionar a imagem original sem redução de qualidade
		avgAng /= contoursLinhas.size()+1;
		
		Imgproc.drawContours(matBranca, contoursLinhas, -1, new Scalar(0), -1) ;
		Mat mapMatrix = Imgproc.getRotationMatrix2D(new Point(w / 2D, h / 2D), avgAng, 1.0);
		Imgproc.warpAffine(matBranca, matBranca, mapMatrix, matBranca.size(), 0,Core.BORDER_CONSTANT,new Scalar(255));
		
		String ys = "";
		contoursLinhas.clear();
		Imgproc.findContours(matBranca, contoursLinhas, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
		for(MatOfPoint cnt : contoursLinhas) {
			double area = Imgproc.contourArea(cnt,true);
			if(area<=0) continue;
			Rect rect = Imgproc.boundingRect(cnt);
			if(ys.length()>0)ys+=",";
			ys+=rect.y;
		}
		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/matBranca.bmp", matBranca);
		//if(saida != "") saida += ",";
		//saida += "\""+key+"\""+":"+val;
		return "\"qr\":\""+qr+"\",\"pag\":\""+pagina+"\",\"div\":["+ys+"],\"ang\":"+avgAng;
	}

	private String getNumeroPagina(Mat matBranca) {
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(this.bw, contours, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
		if(LerCartao.matLetras==null) {
			LerCartao.matLetras = new Mat[11];
			Mat imgTemplate = Imgcodecs.imread(Reader.addFromJar("times.png","times.png"),0);
			//Mat imgTemplate = Imgcodecs.imread(Reader.addFromJar("template3.png","template3.png"),0);
			Imgproc.adaptiveThreshold(imgTemplate, imgTemplate, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 201, 20);
			List<MatOfPoint> contoursTmp = new ArrayList<>();
			Imgproc.findContours(imgTemplate, contoursTmp, new Mat(), Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
			for(MatOfPoint cnt : contoursTmp) {
				double area = Imgproc.contourArea(cnt,true);
				if(area<0) continue;//contoursTmp.remove(cnt);
				Rect rect = Imgproc.boundingRect(cnt);
				int ind = (int) Math.round((double)(rect.x - 94) / 99.8);//times
				//int ind = (int) Math.round((double)(rect.x - 63) / 92.4);//template3
				if(ind>=0 && ind<11) {
					LerCartao.matLetras[ind]=imgTemplate.submat(rect);
				}
			}
		}
		int w = this.bw.width();
		int h = this.bw.height();
		List<MatOfPoint> contoursLetras = new ArrayList<>();
		Map<MatOfPoint, String> mapLetras = new HashMap<>();
//		int iTmp = 0;

		double minArea = 15*this.prop*this.prop;
		double maxArea = 300*this.prop*this.prop;
		int minWidth = (int)Math.round(5 * this.prop);
		int maxWidth = (int)Math.round(20 * this.prop);
		int minHeight = (int)Math.round(10 * this.prop);
		int maxHeight = (int)Math.round(40 * this.prop);
		
		for(MatOfPoint cnt : contours) {
			Rect rect = Imgproc.boundingRect(cnt);
			if(rect.x > w/4 || rect.y <  h - h/6) continue;
			double area = Imgproc.contourArea(cnt,true);
			if(area < minArea || area > maxArea) continue;//only black cnt
			if(rect.width > maxWidth || rect.width < minWidth || rect.height > maxHeight || rect.height < minHeight) continue;
			/*Imgproc.rectangle (
				dst2,                    //Matrix obj of the image
		         new Point(rect.x, rect.y),        //p1
		         new Point(rect.x+rect.width, rect.y+rect.height),       //p2
		         new Scalar(128),     //Scalar object for color
		         1                          //Thickness of the line
		      );*/
			//MatOfPoint approxContour = new MatOfPoint();
			Mat matCnt= this.bw.submat(rect);
			//int wTmp = (int)(matCnt.cols() * (double)heightTemplate / matCnt.rows());
			//int hTmp = heightTemplate;
			//Imgproc.resize( matCnt, matCnt, new Size(wTmp,hTmp) );
			//if(wTmp > heightTemplate) continue;
			//Mat outputImage=new Mat();
			//Imgproc.matchTemplate(LerCartao.imgTemplate, matCnt, outputImage, Imgproc.TM_CCOEFF);
			//Point matchLoc = Core.minMaxLoc(outputImage).maxLoc;
			//String key = "2";
			Mat mRez = new Mat();
			int iEsco = 12;
			double maxVal = 0;
			for(int i=0;i<LerCartao.matLetras.length;i++) {
				Mat mLetraAt = LerCartao.matLetras[i];
				Imgproc.resize( matCnt, mRez, new Size(mLetraAt.cols(),mLetraAt.rows()) );
				Imgproc.adaptiveThreshold(mRez, mRez, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 201, 20);
				Mat dst = new Mat(mLetraAt.rows(), mLetraAt.cols(), mLetraAt.type());
				Core.bitwise_xor(mRez, mLetraAt, dst);
				double perc = (1 - (double)Core.countNonZero(dst)/(mLetraAt.rows() * mLetraAt.cols())) * 100;
				if(perc > maxVal) {
					maxVal = perc;
					iEsco = i;
				}
				/*Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/template-"+(iTmp++)+".bmp", mRez);
				Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/template-"+(iTmp++)+".bmp", mLetraAt);
				System.out.println(iTmp+":"+perc);
				Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/template-"+(iTmp++)+".bmp", dst);*/
			}
			if(maxVal > 80) {
				contoursLetras.add(cnt);
				mapLetras.put(cnt,aLetras[iEsco]);
				/*Point p1 = new Point(rect.x,rect.y);
				Point p2 = new Point(rect.x+rect.width,rect.y+rect.height);
				Imgproc.rectangle(matBranca, p1, p2, new Scalar(0));*/
			}
			//System.out.println(iEsco+":"+maxVal);
			/*for(String key : mTemplate.keySet()) {//dava pra fazer busca melhor com round de uma divisao
				double difY = mTemplate.get(key)[1] - matchLoc.y;
				if(Math.abs(mTemplate.get(key)[0] - matchLoc.x) <= 15 && difY <= 15 && difY >= -5) {
					//TODO verificar differença b e p entre posicao encontrada 
					//System.out.println(key+" "+iTmp+" area:"+area+" w:"+rect.width+" h:"+rect.height+" "+"pos:"+rect.x+","+rect.y+" ");
					//String keyPath = key == "/" ? "-" : key;
					//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/eita-"+(iTmp++)+"-"+keyPath+".bmp", matCnt);
					//Mat imgTemplate = LerCartao.imgTemplate.clone();
					//Imgproc.rectangle(imgTemplate, matchLoc, new Point(matchLoc.x + matCnt.cols(),matchLoc.y + matCnt.rows()), new Scalar(128));
					//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/template-"+(iTmp++)+"-"+keyPath+".bmp", imgTemplate);
					contoursLetras.add(cnt);
					mapLetras.put(cnt,key);
					//break;
					continue loop1;
				}
			}*/
			//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/z-"+(iTmp++)+".bmp", matCnt);
		}
		//Imgproc.drawContours(matBranca, contoursLetras, -1, new Scalar(0), -1) ;
		//Imgcodecs.imwrite("/home/samuelkato/Desktop/img diss/mathBranca.bmp", matBranca);
		//List<String> aCand = new ArrayList<String>();
		String lastStrLinha=""; 
		Linha lastLinha=null;
		List<Linha> linhas = Linha.gerarLinhas(contoursLetras);//pontos colineares
		for(Linha linha : linhas) {
			String strLinha = linha.getTexto(mapLetras);
			String[] aLinha = strLinha.split("/");
			if(strLinha.matches("\\d+\\/\\d+") && Integer.parseInt(aLinha[0]) <= Integer.parseInt(aLinha[1])) {
				//verificar se letras sao equidistantes
				//verificar se estão no canto inferior da pagina
				//verificar angulo
				
				if(strLinha.length() > lastStrLinha.length()) {
					lastStrLinha = strLinha;
					lastLinha = linha;
				}
				//return strLinha;
			}
			//System.out.println("\n"+linha.pontos.size());
			/*System.out.println("\n"+linha.rectMin.x+":"+linha.rectMin.y+" "+linha.rectMax.x+":"+linha.rectMax.y);
			Point pt1 = new Point(linha.rectMin.x, linha.rectMin.y);
			Point pt2 = new Point(linha.rectMax.x, linha.rectMax.y);
			Imgproc.line(gray, pt1, pt2, new Scalar(128), 1);*/
		}
		if(lastLinha!=null) {
			List<MatOfPoint> contoursTmp = new ArrayList<>();
			contoursTmp.add(lastLinha.getContourBarra(mapLetras));
			Imgproc.drawContours(matBranca, contoursTmp, -1, new Scalar(0), -1) ;
		}
		return lastStrLinha;
	}
	
	

	/**
	 * TASK permitir leitura invertida
	 * procura os 3 pontos de referencia do cartao
	 * */
	private List<Region> procurar3pontos(ImageProcessing clImg) throws Exception{
		//procurar areas brancas delimitadas
		
		//evitar "furos" nos 3 quadrados delimitantes
		//clImg.mInv=clImg.erode(clImg.mInv);
		
		//boolean[][] m = clImg.erode(clImg.m);
		
//		m=clImg.dilate(m);
//		m=clImg.erode(m);
		
//		clImg.saveFilteredImage("/home/samuelkato/tmp1.bmp", m);
//		clImg.saveFilteredImage("/home/samuelkato/tmp2.bmp", mInv);
//		clImg.saveFilteredImage("/home/samuelkato/tmp3.bmp", bwInv);
		Exception $e = null;
		List<Region> pontosRef=new Vector<Region>();
		String[][] aInstr = {{},{"erode","dilate"},{"dilate","erode"},{"erode","dilate","dilate","erode"},{"passabaixa"},{"passabaixa","erode","dilate"}};
		for(String[] aInstr2 : aInstr){
			boolean[][] m = clImg.m;
			for(String instr : aInstr2){
				System.out.println(instr+" 3pontos");
				if(instr.compareTo("erode")==0) m = clImg.erode(m);
				else if(instr.compareTo("dilate")==0) m = clImg.dilate(m);
				else if(instr.compareTo("passabaixa")==0) {
					boolean[][] filtro = {{true,true,true},{true,true,true},{true,true,true}};
					//*
					ImageProcessing tmp=new ImageProcessing(clImg.img,true);
					tmp.passaBaixa(filtro);
					m = tmp.createMatrix();
					/*/
					clImg.passaBaixa(filtro);
					m = clImg.createMatrix();
					//*/
				}
			}
			$e = null;
			try{
				pontosRef = procurar3pontosTamanho(clImg, clImg.bwlabel(m));
				break;
			}catch(ErrAng $er){
				System.out.println($er);
				throw $er;
			}catch(Exception $er){
				System.out.println($er);
				$e = $er;
			}
		}
		
		if($e!=null){
			throw $e;
		}
		
		return pontosRef;
	}
	
	private List<Region> procurar3pontosTamanho(ImageProcessing clImg, int[][] bw) throws Exception{
		ConfigImageProcessing config=new ConfigImageProcessing();
		
		List<Region> regions = clImg.regionProps(bw);
		List<Region> regions2 = new ArrayList<Region>(regions);
		/*for(Region r : regions) {
			if(r.area>=200 && r.area<=600) {
				System.out.println(r.area+" "+r.getDensity()+" "+r.centrox+":"+r.centroy);
			}
			if(r.centrox > 200 && r.centrox < 210 && r.centroy > 605 && r.centroy < 620) {

				System.out.println("aqui => "+r.area+" "+r.getDensity()+" "+r.centrox+":"+r.centroy);
			}
		}*/
		
		config.minArea=200;
		config.maxArea=600;
		config.minDensity=0.6;
		config.maxDensity=1;
		List<Region> regIn = clImg.filterRegions(regions,config);
		
		/*for(Region r : regions2) {
			if(r.area>=400) {
				System.out.println(r.area+" "+r.getDensity()+" "+r.centrox+":"+r.centroy);
			}
		}*/
		config.minArea=400;
		config.maxArea=1000;
		config.minDensity=0.1;
		config.maxDensity=0.6;
		List<Region> regOut = clImg.filterRegions(regions2,config);
		
		List<Region> pontosRef=new Vector<Region>();
		int folga = 5;
		for(Region pIn : regIn){
			for(Region pOut : regOut){
				int xout=pOut.centrox;
				int yout=pOut.centroy;
				int xin=pIn.centrox;
				int yin=pIn.centroy;
				if(xout-folga <= xin && xout+folga >= xin && yout-folga <= yin && yout+folga >= yin){
					pontosRef.add(pIn);
				}
			}
		}
		
		if(pontosRef.size() < 3){
			throw new Exception("Erro: 3 pontos nao encontrados (tamanho)");
		}
		
		pontosRef = busca3pontos(pontosRef);
		if(pontosRef == null) throw new Exception("Erro: 3 pontos nao encontrados (angulo 90)");
		
		return procurar3pontosRotacao(clImg, pontosRef);
	}
	
	/**
	 * verifiva a orientação do cartao
	 * 
	 * @param clImg
	 * @param pontosRef
	 * @return
	 * @throws Exception
	 */
	private List<Region> procurar3pontosRotacao(ImageProcessing clImg, List<Region> pontosRef) throws Exception{
		List<Region> ret;
		
		/*
		(2|3)
		-----
		(1|4)
		um deles eh repetido
		*/
		
		int mX = clImg.width / 2;
		int mY = clImg.height / 2;
		
		Region p1 = null,p2 = null, p3 = null, p4 = null;
		for(Region p : pontosRef){
			if(p.centrox < mX && p.centroy > mY){
				p1 = p;
			}
			if(p.centrox < mX && p.centroy < mY){
				p2 = p;
			}
			if(p.centrox > mX && p.centroy < mY){
				p3 = p;
			}
			if(p.centrox > mX && p.centroy > mY){
				p4 = p;
			}
		}
		
		if(p1 != null && p2 != null && p3 != null && p4 == null){//correto
			ret = new Vector<Region>();
			ret.add(p1);
			ret.add(p2);
			ret.add(p3);
		}else if(p1 != null && p2 != null && p3 == null && p4 != null){//girar 90 horario
			throw new ErrAng(90);
		}else if(p1 != null && p2 == null && p3 != null && p4 != null){//girar 180
			throw new ErrAng(180);
		}else if(p1 == null && p2 != null && p3 != null && p4 != null){//girar 90 anti-horario
			throw new ErrAng(-90);
		}else{
			throw new Exception("Erro: 3 pontos nao encontrados (posicao)\n"+pontosRef);
		}
		
		return ret;
	}
	
	private List<Region> busca3pontos(List<Region> variosPontos){
		int len=variosPontos.size();
		int i=1*len+1; //011 na base len
		int max=(len-1)*len*len+(len-2)*len+len-3;//valor maximo de i
		
		List<List<Region>> ret=new Vector<List<Region>>();
		do{
			i++;
			String a = Integer.toString(i, len);
			while(a.length()<3)a="0"+a;
			
			int p1=Integer.parseInt(a.substring(0,1),len);
			int p2=Integer.parseInt(a.substring(1,2),len);
			int p3=Integer.parseInt(a.substring(2,3),len);
			
			if(p1==p2 || p1==p3 || p2==p3)continue;
			
			List<Region> pTest=new Vector<Region>();
			pTest.add(variosPontos.get(p1));
			pTest.add(variosPontos.get(p2));
			pTest.add(variosPontos.get(p3));
			if(check3pontos(pTest)){
				ret.add(pTest);
			}
		}while(i<=max);
		
		
		
		int retInd=-1;
		int maxDistx=0;//distancia x entre p2 e p3
		for(int j=0; j<ret.size(); j++){
			List<Region> pAt=ret.get(j);
			int distX=Math.abs(pAt.get(1).centrox-pAt.get(2).centrox);
			if(maxDistx<distX){
				maxDistx=distX;
				retInd=j;
			}
		}
		if(retInd>=0)return ret.get(retInd);
		return null;
	}
	
	/**
	 * verifica posicionamento dos 3 pontos do cartao
	 * */
	private boolean check3pontos(List<Region> pontosRef){
		boolean ret=false;
		Region ponto1=pontosRef.get(0);
		Region ponto2=pontosRef.get(1);
		Region ponto3=pontosRef.get(2);
		
		double a=-(double)(ponto1.centrox-ponto2.centrox)/(double)(ponto1.centroy-ponto2.centroy);
		double a2=(double)(ponto2.centroy-ponto3.centroy)/(double)(ponto2.centrox-ponto3.centrox);
	
		ret = Math.abs(a2-a)<0.03;
		return ret;
	}
	
	private List<List<Region>> getClocks(ImageProcessing clImg, List<Region> pontosRef) throws Exception{
		List<List<Region>> ret = new Vector<List<Region>>();
		Region ponto1=pontosRef.get(0);
		Region ponto2=pontosRef.get(1);
		Region ponto3=pontosRef.get(2);
//		int centrox4=ponto3.centrox+ponto1.centrox-ponto2.centrox;
		int centroy4=ponto3.centroy+ponto1.centroy-ponto2.centroy;
//		gerarPontosCartao(pontosRef,reg,23);//prova de tatui impressa sem clocks
		
		ConfigImageProcessing config=new ConfigImageProcessing();
		config.minArea=30;
		config.maxArea=200;
		config.minDensity=0.5;
		config.maxDensity=1;
		
		//linha entre ponto1 e ponto2
		double a=(double)(ponto1.centrox-ponto2.centrox)/(double)(ponto1.centroy-ponto2.centroy);
		double b=(double)ponto2.centrox-a*(double)ponto2.centroy;
		double b2=(double)ponto3.centrox-a*(double)ponto3.centroy;
		
		List<Region> col1=null;
		List<Region> col2=null;
		
		String[][] aInstr = {{},{"dilate","erode"},{"erode","dilate"},{"erode","dilate","dilate","erode"}};
		for(String[] aInstr2 : aInstr){
			col1=new Vector<Region>();
			col2=new Vector<Region>();
			boolean[][] m = clImg.m;
			
			for(String instr : aInstr2){
				System.out.println(instr+" clocks");
				if(instr.compareTo("erode")==0){
					m = clImg.erode(m);
				}else if(instr.compareTo("dilate")==0){
					m = clImg.dilate(m);
				}
			}
			List<Region> liReg = clImg.filterRegions(clImg.regionProps(clImg.bwlabel(m)),config);
			
			for(Region reg : liReg){
				int yAt=reg.centroy;
				int xAt=reg.centrox;
				int x=(int)(yAt*a+b);
				int x2=(int)(yAt*a+b2);
				
				if(
					x+5>xAt &&
					x-5<xAt &&
					yAt>ponto2.centroy+20 &&
					yAt<ponto1.centroy-20 &&
					
					yAt > ponto2.centroy + 20 &&
					yAt < ponto1.centroy - 20
				){//coluna1
					col1.add(reg);
					//liReg.remove(reg);
				}else if(
					x2+5>xAt &&
					x2-5<xAt &&
					yAt>ponto3.centroy+20 &&
					
					yAt > ponto3.centroy + 20 &&
					yAt < centroy4 - 20
				){//coluna2
					col2.add(reg);
					//liReg.remove(reg);
				}
			}
			if(col1.size()==col2.size() && col1.size()>0){
				Collections.sort(col1,new Sorter("centroy",1));
				Collections.sort(col2,new Sorter("centroy",1));
				ret.add(col1);
				ret.add(col2);
				break;
			}
		}
		
		if(col1.size()!=col2.size()){
			for(int i=0; i<Math.max(col1.size(), col2.size()); i+=1){
				Region p1=null;
				try{
					p1=col1.get(i);
				}catch(Exception e){}
				Region p2=null;
				try{
					p2=col2.get(i);
				}catch(Exception e){}
				if(p1!=null && p2!=null){
					System.out.println(p1+" "+p2);
				}else if(p2!=null){
					System.out.println("-:-:- "+p2);
				}else if(p1!=null){
					System.out.println(p1+" -:-:-");
				}
			}
			throw new Exception("tamanho da coluna dos clocks invalido col1:"+col1.size()+" col2:"+col2.size());
		}
		return ret;
	}
	
	/**
	 * encontra e le o qr de uma folha de respostas
	 * 
	 * @param file imagem alvo
	 * @param pontosRef usado para localizar o qr
	 * @param rWidth width da imagem reduzida. é mais consistente que o tamanho da imagem
	 * @return
	 * @throws IOException 
	 */
	private String lerQr(ImageProcessing clImg, List<Region> pontosRef){
		
		
		//long startTime = System.nanoTime();
		int ang = (int)(Math.atan2(pontosRef.get(2).centroy - pontosRef.get(1).centroy, pontosRef.get(2).centrox - pontosRef.get(1).centrox)*180/Math.PI);
		
		BufferedImage file = clImg.img;
		int rWidth = clImg.width;
		float prop=(float)file.getWidth()/(float)rWidth;
		int posY = (pontosRef.get(1).centroy + pontosRef.get(2).centroy) / 2;
		float propx = (float)(pontosRef.get(2).centrox-pontosRef.get(1).centrox)/1000;
		
		BufferedImage qrImage = file.getSubimage((int)((pontosRef.get(1).centrox+(300*propx))*prop), (int)(Math.max(0,(posY-170)*prop)), (int)(180*prop), (int)(180*prop));
		
		return lerQr1(qrImage, ang);
	}

	private BufferedImage mudaImagem(BufferedImage qrImg, String instr, int ang){
		if(instr.compareTo("pb")==0){
			ImageProcessing tmp=new ImageProcessing(qrImg,true);
			tmp.createMatrix(new configThresh());
			qrImg=tmp.matrix2img(tmp.m);
		}
		
		else if(instr.compareTo("rotateang")==0){
			qrImg = ImageProcessing.rotate(qrImg, -ang);
		}else if(instr.compareTo("rotate45")==0){
			qrImg = ImageProcessing.rotate(qrImg, 45);
		}
		
		else if(instr.compareTo("passabaixa")==0){
			ImageProcessing tmp=new ImageProcessing(qrImg,true);
			boolean[][] filtro = {{false,false,false,false,false},{true,true,true,true,true},{false,false,false,false,false}};
			tmp.passaBaixa(filtro);
			qrImg = tmp.rgb2img();
		}
		
		else if(instr.compareTo("tamanho100")==0){
			qrImg=ImageProcessing.criarImagemRedimensionada(qrImg, 100);
		}else if(instr.compareTo("tamanho200")==0){
			qrImg=ImageProcessing.criarImagemRedimensionada(qrImg, 200);
		}
		
		else if(instr.compareTo("crop")==0){
			qrImg=qrImg.getSubimage(10, 10, qrImg.getWidth() - 20, qrImg.getHeight() - 20);
		}
		return qrImg;
	}
	
	private String lerQr1(Mat mat){
		MatOfByte mob=new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, mob);
		try {
			BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(mob.toArray()));
			return lerQr1(qrImage);
		} catch (IOException e) {
		}
		return "";		
	}
	
	private String lerQr1(BufferedImage qrImage) {
		return lerQr1(qrImage,0);
	}
	
	private String lerQr1(BufferedImage qrImage, int ang) {
		String[] mInstr = {"","crop","tamanho100","tamanho200","pb","rotateang","rotate45","passabaixa"};
		String result=null;
		BufferedImage qrImg1,qrImg2,qrImg3;
		
		for(int i = 0; i < mInstr.length; i++){
			qrImg1 = qrImage.getSubimage(0, 0, qrImage.getWidth(), qrImage.getHeight());
			qrImg1 = mudaImagem(qrImg1, mInstr[i], ang);
			for(int j = 0; j < mInstr.length; j++){
				qrImg2 = mudaImagem(qrImg1,mInstr[j],ang);
				for(int k = 0; k < mInstr.length; k++){
					qrImg3 = mudaImagem(qrImg2, mInstr[k],ang);
					result = lerQr2(qrImg3,mInstr[i]+"."+mInstr[j]+"."+mInstr[k]);
					if(result!=null)return result;
				}
			}
		}
		return "";
	}
	
	private String lerQr2(BufferedImage qrImg, String tipo){
		QRCodeReader reader=new QRCodeReader();
		Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		BinaryBitmap bitmap = null;
		
		try{
			if(this.debug_){
				this.clImg.saveImage(qrImg, "-qrImg-"+tipo);
			}
			
			bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(qrImg)));
			return reader.decode(bitmap,tmpHintsMap).getText();
		}catch(Exception e){
		}
		return null;
	}
	
	/**
	 * cria clocks de uma folha, por conta de folhas impressas sem clocks
	 * 
	 * @param pontosRef
	 * @param reg
	 */
	/*private void gerarPontosCartao(List<Region> pontosRef, List<Region> reg, int nClocks) {
		Region ponto1=pontosRef.get(0);
		Region ponto2=pontosRef.get(1);
		Region ponto3=pontosRef.get(2);
		
		double a=(double)(ponto1.centrox-ponto2.centrox)/(double)(ponto1.centroy-ponto2.centroy);
		double b=(double)ponto1.centrox-a*(double)ponto1.centroy;
		double b2=(double)ponto3.centrox-a*(double)ponto3.centroy;
		
		//em mm=228
		//dist ini=57
		//dist entre=5
		double disty=ponto1.centroy-ponto2.centroy;
		
		double razao=disty/228;
		
		double yIni=razao*57+ponto2.centroy;
		double yIniEsq=razao*57+ponto3.centroy;
		
		for(int i=0;i<nClocks;i++){
			int y=(int)(yIni+i*razao*5);
			int yEsq=(int)(yIniEsq+i*razao*5);
			
			int x=(int)(y*a+b);
			int xEsq=(int)(yEsq*a+b2);
			
			reg.add(new Region(60, x, y, x-5, y-5, x+5, y+5, x+y));
			
			reg.add(new Region(60, xEsq, yEsq, xEsq-5, yEsq-5, xEsq+5, yEsq+5, xEsq+yEsq));
		}
	}*/

	/**
	 * verifica a porcentagem preenchida de uma marcacao
	 * 
	 * @param x
	 * @param y
	 * @param m
	 * @return porcentagem preenchida
	 */
	
	private float conferirMarcacao(int x,int y, boolean[][] m){
		int cnt=0;
		//x+=2;//o x está um pouco à esquerda
		//y+=2;
		for(int i=x-4;i<x+5;i++){
			for(int j=y-2;j<y+3;j++){
				if(m[j][i])cnt++;
				//else m[j][i]=true;
			}
		}
		return (float)cnt/45;
	}
	
	class ErrAng extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public int ang = 0;
		public ErrAng(int ang){
			this.ang = ang;
		}
	}

	class configThresh extends ConfigImageProcessing{
		public boolean checkThreshold(int r, int g, int b, int rAvg, int gAvg, int bAvg){
			return r + g + b < (rAvg + gAvg + bAvg - 50) && !(r>g+10 && r>b+10 && r>150);
		}
	}
	

	/**
	 * Ordena vector de Region
	 * por campo
	 * asc(1) ou desc(-1)
	 * 
	 * @author samuelkato
	 *
	 */
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
	
	public String getSaida(){
		return saida;
	}
	
	public int getTipo() {
		return this.tipo;
	}


	public String md5Hash() {
		return clImg.md5Hash();
	}
	
}