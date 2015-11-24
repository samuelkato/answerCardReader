import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;


public class LerCartao extends Thread{
	ImageProcessing clImg;
	public LerCartao(BufferedImage file) {
		this.clImg=new ImageProcessing(file);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void run() {
		String saida="";
		Hashtable<String,String> saidaHt = new Hashtable<String,String>();
		//clImg.createMatrix();
		//clImg.m=clImg.dilate(clImg.m);
		//clImg.saveFilteredImage("/home/samuelkato/sa1.bmp", clImg.bwlabel(clImg.m));
		//clImg.saveFilteredImage("/home/samuelkato/sa.bmp", clImg.bwlabel());
		
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
				clImg.rotate($e.ang);
				pontosRef = procurar3pontos(clImg);
			}
			
			String qr = lerQr(clImg, pontosRef);
			
			saidaHt.put("qr", "\""+qr+"\"");
			System.out.println("qr: "+qr);
			
			List<List<Region>> aCols = getClocks(clImg, pontosRef);
			
			
			boolean[][] m = clImg.m;
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
					
					int x=(int)(clockDistX*j+clock1.centrox);
					int y=(int)(x*aClock+bClock);
					
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
			//clImg.saveFilteredImage("/home/samuelkato/tmp1.bmp",m);
			
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
		
		String formatoImg = "jpg";
		
		BufferedImage imgRes = clImg.img;

		String nomeImgZip = md5Hash(imgRes) + "." + formatoImg;
		
		/*try{
			zipSaida.putNextEntry(new ZipEntry(nomeImgZip));
			ImageIO.write(imgRes, formatoImg, zipSaida);
			zipSaida.closeEntry();
			
			//passar saidaHt para json
			saidaHt.put("file", "\""+nomeImgZip+"\"");
			
			
		}catch(Exception e){
			//unico caso em que a imagem nao vai para o zip
			throw new Exception("imagemRepetida");
		}
		return saida;*/
		Enumeration<String> enumKey = saidaHt.keys();
		while(enumKey.hasMoreElements()){
			String key = enumKey.nextElement();
			String val = saidaHt.get(key);
			if(saida != "") saida += ",";
			saida += "\""+key+"\""+":"+val;
		}
		saida="{"+saida+"}";
		System.out.println(saida);
	}

	/**
	 * retorna o md5 hash de uma imagem
	 * */
	private String md5Hash(BufferedImage img){
		String hexString = "";
		try{
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(img, "png", outputStream);
			byte[] data = outputStream.toByteArray();

			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data);
			
			byte[] inBytes=md.digest();
			for (int i=0; i < inBytes.length; i++) { //for loop ID:1
				hexString +=
				Integer.toString( ( inBytes[i] & 0xff ) + 0x100, 16).substring( 1 );
			}
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
		return hexString;
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
		String[][] aInstr = {{},{"erode","dilate"},{"dilate","erode"},{"erode","dilate","dilate","erode"}};
		for(String[] aInstr2 : aInstr){
			boolean[][] m = clImg.m;
			for(String instr : aInstr2){
				System.out.println(instr+" 3pontos");
				if(instr.compareTo("erode")==0) m = clImg.erode(m);
				else if(instr.compareTo("dilate")==0) m = clImg.dilate(m);
			}
			$e = null;
			try{
				pontosRef = procurar3pontosTamanho(clImg, clImg.bwlabel(m));
				break;
			}catch(ErrAng $er){
				throw $er;
			}catch(Exception $er){
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
		
		config.minArea=200;
		config.maxArea=600;
		config.minDensity=0.6;
		config.maxDensity=1;
		List<Region> regIn = clImg.filterRegions(clImg.regionProps(bw),config);
		
		config.minArea=400;
		config.maxArea=1000;
		config.minDensity=0.1;
		config.maxDensity=0.5;
		List<Region> regOut = clImg.filterRegions(clImg.regionProps(bw),config);
		
		List<Region> pontosRef=new Vector<Region>();
		int folga = 3;
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
		String result=null;
		
		//long startTime = System.nanoTime();
		int ang = (int)(Math.atan2(pontosRef.get(2).centroy - pontosRef.get(1).centroy, pontosRef.get(2).centrox - pontosRef.get(1).centrox)*180/Math.PI);
		
		BufferedImage file = clImg.imgOrig;
		int rWidth = clImg.width;
		float prop=(float)file.getWidth()/(float)rWidth;
		BufferedImage qrImage=file.getSubimage((int)((pontosRef.get(1).centrox+165)*prop), (int)(Math.max(0,(pontosRef.get(1).centroy-130)*prop)), (int)(160*prop), (int)(160*prop));
		
		String[] mInstr = {"","tamanho100","tamanho200","pb","rotateang","rotate45","passabaixa"};
		
		for(int i = 0; i < mInstr.length; i++){
			BufferedImage qrImg=qrImage.getSubimage(0, 0, qrImage.getWidth(), qrImage.getHeight());
			qrImg = mudaImagem(qrImg,mInstr[i],ang);
			for(int j = 0; j < mInstr.length; j++){
				qrImg = mudaImagem(qrImg,mInstr[j],ang);
				for(int k = 0; k < mInstr.length; k++){
					qrImg = mudaImagem(qrImg,mInstr[k],ang);
					result = lerQr2(qrImg);
					if(result!=null)return result;
				}
			}
		}
		
		

		
		return "";
	}

	private BufferedImage mudaImagem(BufferedImage qrImg, String instr, int ang){
		if(instr.compareTo("pb")==0){
			ImageProcessing tmp=new ImageProcessing(qrImg);
			tmp.createMatrix(new configThresh());
			qrImg=tmp.matrix2img(tmp.m);
		}
		
		else if(instr.compareTo("rotateang")==0){
			qrImg = ImageProcessing.rotate(qrImg, -ang);
		}else if(instr.compareTo("rotate45")==0){
			qrImg = ImageProcessing.rotate(qrImg, 45);
		}
		
		else if(instr.compareTo("passabaixa")==0){
			ImageProcessing tmp=new ImageProcessing(qrImg);
			boolean[][] filtro = {{false,false,false,false,false},{true,true,true,true,true},{false,false,false,false,false}};
			tmp.passaBaixa(filtro);
			qrImg = tmp.rgb2img();
		}
		
		else if(instr.compareTo("tamanho100")==0){
			qrImg=ImageProcessing.criarImagemRedimensionada(qrImg, 100);
		}else if(instr.compareTo("tamanho200")==0){
			qrImg=ImageProcessing.criarImagemRedimensionada(qrImg, 200);
		}
		return qrImg;
	}
	
	private String lerQr2(BufferedImage qrImg){
		QRCodeReader reader=new QRCodeReader();
		Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		BinaryBitmap bitmap = null;
		
		try{
//			int cnt2=0;
//			File outputfile = new File("/home/samuelkato/qrZoado-"+(cnt2)+".png");
//			while(outputfile.exists()){
//				outputfile = new File("/home/samuelkato/qrZoado-"+(++cnt2)+".png");
//			}
//			ImageIO.write(qrImg, "png", outputfile);
			
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
		x+=1;//o x está um pouco à esquerda
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
	
}