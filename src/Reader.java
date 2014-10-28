/**
 * cria zip saida
 * melhoria na deteccao de cores
 * 
 */

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.beans.*;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeReader;


public class Reader  extends JPanel implements ActionListener, PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 480869136325312005L;
	private JProgressBar progressBar;
    private JButton startButton;
    private JTextArea taskOutput;
    private ProcessaFolder task;
   
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	//Create and set up the window.
                JFrame frame = new JFrame("Ler Folha Resposta v6");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         
                //Create and set up the content pane.
                JComponent newContentPane = new Reader();
                newContentPane.setOpaque(true); //content panes must be opaque
                frame.setContentPane(newContentPane);
         		
                //Display the window.
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    public Reader(){
		super(new BorderLayout());
		 
        //Create the demo's UI.
        startButton = new JButton("Escolher Pasta");
        startButton.setActionCommand("start");
        startButton.addActionListener(this);
 
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
 
        taskOutput = new JTextArea(10, 50);
        taskOutput.setMargin(new Insets(5,5,5,5));
        taskOutput.setEditable(false);
 
        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(progressBar);
 
        add(panel, BorderLayout.PAGE_START);
        add(new JScrollPane(taskOutput), BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	}
	public void actionPerformed(ActionEvent evt) {
		if(evt.getActionCommand().equals("start")){
			JFileChooser chooser = new JFileChooser();
			//chooser.setCurrentDirectory(new java.io.File(System.getenv().get("HOME")));
			chooser.setDialogTitle("Selecione a pasta desejada");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				startButton.setEnabled(false);
		        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		        //Instances of javax.swing.SwingWorker are not reusuable, so
		        //we create new instances as needed.
		        task = new ProcessaFolder(chooser.getSelectedFile());
		        task.addPropertyChangeListener(this);
		        task.execute();
			}
		}
    }
 
    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        }
    }
	
    /**
     * classe para processar imagens de uma pasta
     * 
     * @author samuelkato
     */
    class ProcessaFolder extends SwingWorker<Void, Void> {
    	private File folder;
    	private ZipOutputStream zipSaida;
    	/**
    	 * contrutora da classe
    	 * 
    	 * @param folder pasta a ser lida
    	 */
    	ProcessaFolder(File folder){
    		this.folder=folder;
    	}

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            startButton.setEnabled(true);
            setCursor(null); //turn off the wait cursor
        }
        
        /**
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
        	String pastaAt = this.folder.getAbsolutePath();
        	try{
        		zipSaida = new ZipOutputStream(new FileOutputStream(pastaAt+"/saida.zip"));
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        	
        	taskOutput.append("Inicio do processamento na pasta: "+pastaAt+"\n");
			System.out.println("Inicio do processamento na pasta: "+pastaAt);
        	String saida="[\n"+listFilesForFolder(this.folder)+"\n]";
        	taskOutput.append("\nFim da pasta "+pastaAt+"\n");
			System.out.println("\nFim da pasta "+pastaAt+"\n");
			
			
			//File fileSaida=new File(pastaAt+"/saida.json");
			try {
				//if (!fileSaida.exists())fileSaida.createNewFile();
				
				zipSaida.putNextEntry(new ZipEntry("saida.json"));
	    		zipSaida.write(saida.getBytes());
	    		zipSaida.closeEntry();
				/*
				FileWriter fw = new FileWriter(fileSaida.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				
				bw.write(saida);
				bw.close();
				*/
				//taskOutput.append("Json gravado com sucesso em: "+fileSaida.getAbsolutePath()+"\n\n");
				//System.out.println("Json gravado com sucesso em: "+fileSaida.getAbsolutePath()+"\n");
			} catch (IOException e) {
				//taskOutput.append("Erro ao gravar o arquivo "+fileSaida.getAbsolutePath()+"\n\n");
				//System.out.println("Erro ao gravar o arquivo "+fileSaida.getAbsolutePath()+"\n");
			}
			
			try{
				zipSaida.close();
			}catch(Exception e){
				e.printStackTrace();
			}
			
            return null;
        }
        
        /**
         * processa todos os arquivos de uma pasta
         * ignora pastas. para torna-lo recursivo, descomente o codigo do primeiro if 
         * */
    	private String listFilesForFolder(final File folder) {
    		String saida="";
    		File[] a=folder.listFiles();
    		int n=0;
    		for (final File fileEntry : a) {
    			if (fileEntry.isDirectory()) {
    				// String ret=listFilesForFolder(fileEntry);//versao recursiva
    				// if(!ret.equals("")){
    				// if(!saida.equals(""))saida+=",";
    				// saida+=ret;
    				// }
    			} else {
    				String retAt;
    				BufferedImage file=null;
					System.out.println("\nProcessando arquivo "+fileEntry.getAbsolutePath());
					try{
						file = ImageIO.read(fileEntry);
						file.getType();//soh pra levantar uma Exception e nao processa o arquivo
						retAt = lerCartao(file);
						if(!retAt.equals("")){
	    					if (!saida.equals(""))saida += ",\n";
	    					saida += "\t"+retAt;
						}
					}catch(Exception e){
						if(e.getMessage()=="imagemRepetida"){
							retAt="\"Erro: uma imagem identica à " + fileEntry.getAbsolutePath() + " ja foi adicionada à saida.\"";
						}else{
							retAt="\"Erro: " + fileEntry.getAbsolutePath() + " nao é imagem.\"";
						}
					}
					taskOutput.append(retAt+"\n");
    			}
    			int progress=(int)Math.floor(((float)++n/a.length)*100);
    			setProgress(Math.min(progress, 100));
    		}
            return saida;
        }
    	
    	/**
    	 * le uma imagem retorna um json com dados do cartao
    	 * @throws Exception 
    	 * */
    	private String lerCartao(BufferedImage file) throws Exception{
    		ImageProcessing clImg=new ImageProcessing(file);
    		String saida="";
    		Hashtable<String,String> saidaHt=new Hashtable<String,String>();
    		//clImg.m = clImg.createMatrix();
    		//clImg.m=clImg.dilate(clImg.m);
    		//clImg.saveFilteredImage("/home/samuelkato/sa.bmp", clImg.bwlabel(clImg.m));
    		
//    		clImg.saveFilteredImage("/home/samuelkato/sa.bmp", clImg.bwlabel());
    		
    		try{
	    		//file=rotate(file,45);
	    		//long startTime = System.nanoTime();
	    		//m=clImg.erode(m);
	    		//clImg.m=clImg.erode(clImg.m);
	    		//clImg.m=clImg.dilate(clImg.m);
	    		List<Region> reg=clImg.regionProps();
	    		List<Region> pontosRef=null;
	    		try{
	    			pontosRef=procurar3pontos(clImg);
	    		}catch(Exception e){
	    			switch(e.getMessage()){
	    			case "90":
	    				System.out.println("imagem rotacionada em 90");
	    				file=ImageProcessing.rotate(file,90);
	    				break;
	    			case "180":
	    				System.out.println("imagem rotacionada em 180");
	    				file=ImageProcessing.rotate(file,180);
	    				break;
	    			case "-90":
	    				System.out.println("imagem rotacionada em -90");
	    				file=ImageProcessing.rotate(file,-90);
	    				break;
	    			default:
	    				e.printStackTrace();
	    				throw e;
	    			}
	    		}finally{
	    			clImg=new ImageProcessing(file);
	    			reg=clImg.regionProps();
	    			pontosRef=procurar3pontos(clImg);
	    		}
	    		boolean[][] m = clImg.m;
	    		String qr=lerQr(file, pontosRef, clImg.width);
	    		saidaHt.put("qr", "\""+qr+"\"");
	    		
	    		System.out.println("qr: "+qr);
	    		
	    		Region ponto1=pontosRef.get(0);
	    		Region ponto2=pontosRef.get(1);
	    		Region ponto3=pontosRef.get(2);
	//    		int centrox4=ponto3.centrox+ponto1.centrox-ponto2.centrox;
	    		int centroy4=ponto3.centroy+ponto1.centroy-ponto2.centroy;
	    		//gerarPontosCartao(pontosRef,reg,23);//prova de tatui impressa sem clocks
	    		
	    		List<Region> col1=new Vector<Region>();
	    		List<Region> col2=new Vector<Region>();
	    		//linha entre ponto1 e ponto2
	    		double a=(double)(ponto1.centrox-ponto2.centrox)/(double)(ponto1.centroy-ponto2.centroy);
	    		double b=(double)ponto2.centrox-a*(double)ponto2.centroy;
	    		double b2=(double)ponto3.centrox-a*(double)ponto3.centroy;
	    		for(int i=0;i<reg.size();i++){
	    			if(reg.get(i).area < 30 || reg.get(i).area>200)continue;
	    			
	    			int yAt=reg.get(i).centroy;
	    			int xAt=reg.get(i).centrox;
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
	    				col1.add(reg.remove(i--));
	    			}else if(
	    					x2+5>xAt &&
	    					x2-5<xAt &&
	    					yAt>ponto3.centroy+20 &&
	    					
	    					yAt > ponto3.centroy + 20 &&
	    					yAt < centroy4 - 20
	    			){//coluna2
	    				col2.add(reg.remove(i--));
	    			}
	    		}
	    		Collections.sort(col1,new Sorter("centroy",1));
	    		Collections.sort(col2,new Sorter("centroy",1));
	    		
	    		
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
	        				System.out.println(p1.centrox+":"+p1.centroy+":"+p1.area+" "+p2.centrox+":"+p2.centroy+":"+p2.area);
	        			}else if(p2!=null){
	        				System.out.println("-:-:- "+p2.centrox+":"+p2.centroy+":"+p2.area);
	        			}else if(p1!=null){
	        				System.out.println(p1.centrox+":"+p1.centroy+":"+p1.area+" -:-:-");
	        			}
	        		}
	    			throw new Exception("tamanho da coluna dos clocks invalido col1:"+col1.size()+" col2:"+col2.size());
	    		}
	    		
	    		
	    		//check number of questions
	    		Hashtable<Integer,List<String>> resp=new Hashtable<Integer,List<String>>();
	    		for(int i=0;i<col1.size();i++){
	    			Region clock1=col1.get(i);
	    			Region clock2=col2.get(i);
	    			//int distx=clock2.centrox-clock1.centrox;
	    			double aClock=(double)(clock2.centroy-clock1.centroy)/(double)(clock2.centrox-clock1.centrox);
	    			double bClock=(double)clock1.centroy-aClock*(double)clock1.centrox;
	    			double clockDistX=((double)(clock2.centrox-clock1.centrox))/26;//numero de divisoes entre o clocks
	    			for(int j=1;j<25;j++){
	    				if((j-1)%6==0)continue;
	    				
	    				int x=(int)(clockDistX*j+clock1.centrox);
	    				int y=(int)(x*aClock+bClock);
	    				
	    				String marca=String.format(Locale.US, "%1$d",Math.round(conferirMarcacao(x,y,m)*100));
	    				
	    				int quesN=(j-1)/6*col1.size()+i;
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
	    		for (int i=0;i<col1.size()*4;i++) {
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
			
			try{
				zipSaida.putNextEntry(new ZipEntry(nomeImgZip));
				ImageIO.write(imgRes, formatoImg, zipSaida);
	    		zipSaida.closeEntry();
	    		
	    		//passar saidaHt para json
	    		saidaHt.put("file", "\""+nomeImgZip+"\"");
	    		Enumeration<String> enumKey = saidaHt.keys();
	    		while(enumKey.hasMoreElements()){
	    			String key = enumKey.nextElement();
	    		    String val = saidaHt.get(key);
	    		    if(saida != "") saida += ",";
	    		    saida += "\""+key+"\""+":"+val;
	    		}
	    		saida="{"+saida+"}";
	    		
			}catch(Exception e){
				//unico caso em que a imagem nao vai para o zip
				throw new Exception("imagemRepetida");
			}
			return saida;
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
    		
    		int[][] bw=clImg.bwlabel(clImg.m);
//    		clImg.saveFilteredImage("/home/samuelkato/tmp1.bmp",clImg.m);
//    		clImg.saveFilteredImage("/home/samuelkato/tmp2.bmp",clImg.mInv);
//    		clImg.saveFilteredImage("/home/samuelkato/tmp3.bmp",bwInv);
    		
    		ConfigImageProcessing config=new ConfigImageProcessing();
    		config.minArea=200;
    		config.maxArea=600;
    		List<Region> regOut=clImg.filterRegions(clImg.regionProps(bw),config);
    		config.minArea=400;
    		config.maxArea=1000;
    		List<Region> regIn=clImg.filterRegions(clImg.regionProps(bw),config);

    		List<Region> regInv=new Vector<Region>();
    		for(int i=0; i<regIn.size(); i++){
    			for(int j=0; j<regOut.size(); j++){
    				int folga=3;
    				int xout=regOut.get(j).centrox;
    				int yout=regOut.get(j).centroy;
    				int xin=regIn.get(i).centrox;
    				int yin=regIn.get(i).centroy;
    				if(xout-folga <= xin && xout+folga >= xin && yout-folga <= yin && yout+folga >= yin){
    					regInv.add(regIn.get(i));
    				}
    			}
    		}
    		
    		if(regInv.size()<3)throw new Exception("Erro: 3 pontos nao encontrados (tamanho)");
    		
    		regInv = busca3pontos(regInv);
    		if(regInv==null)throw new Exception("Erro: 3 pontos nao encontrados (90)");
    		
    		List<Region> ret=new Vector<Region>();
    		
    		int[][] max = new int[4][2];
    		for(int i=0; i<max.length; i+=1)max[i][1]=-10000000;
    		
    		int maxx = 0, maxy = 0;
    		for(int i=0;i<regInv.size();i++){
    			Region regAt=regInv.get(i);
    			int y=regAt.centroy, x=regAt.centrox;
    			if(maxx < x)maxx = x;
    			if(maxy < y)maxy = y;
    			
    			//infesq
    			int infesq = +y-x;
    			if(infesq > max[0][1]){
    				max[0][1]=infesq;
    				max[0][0]=i;
    			}
    			//supesq
    			int supesq = -y-x;
    			if(supesq > max[1][1]){
    				max[1][1]=supesq;
    				max[1][0]=i;
    			}
    			//supdir
    			int supdir = -y+x;
    			if(supdir > max[2][1]){
    				max[2][1]=supdir;
    				max[2][0]=i;
    			}
    			//infdir
    			int infdir = +y+x;
    			if(infdir > max[3][1]){
    				max[3][1]=infdir;
    				max[3][0]=i;
    			}
    		}
    		
    		
    		/*
    		(2|3)
    		-----
    		(1|4)
    		um deles eh repetido
    		*/
    		
    		Region p1=regInv.get(max[0][0]);//infesq
    		Region p2=regInv.get(max[1][0]);//supesq
    		Region p3=regInv.get(max[2][0]);//supdir
    		Region p4=regInv.get(max[3][0]);//infdir
    		
    		
    		
    		boolean ok1,ok2,ok3,ok4;
    		
    		ok1=(p1.centrox < maxx/2) && (p1.centroy > maxy/2);//infesq
    		ok2=(p2.centrox < maxx/2) && (p2.centroy < maxy/2);//supesq
    		ok3=(p3.centrox > maxx/2) && (p3.centroy < maxy/2);//supdir
    		ok4=(p4.centrox > maxx/2) && (p4.centroy > maxy/2);//infdir
    		
    		
    		
    		if(ok1 && ok2 && ok3 && !ok4){//correto
    			ret.add(p1);
    			ret.add(p2);
    			ret.add(p3);
    		}else if(ok1 && ok2 && !ok3 && ok4){//girar 90 horario
    			throw new Exception("90");
    		}else if(ok1 && !ok2 && ok3 && ok4){//girar 180
    			throw new Exception("180");
    		}else if(!ok1 && ok2 && ok3 && ok4){//girar 90 anti-horario
    			throw new Exception("-90");
    		}else{
    			throw new Exception("Erro: 3 pontos nao encontrados (posicao)");
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
		
			ret = Math.abs(a2-a)<0.01;
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
    	private String lerQr(BufferedImage file, List<Region> pontosRef, int rWidth){
    		//long startTime = System.nanoTime();
    		float prop=(float)file.getWidth()/(float)rWidth;
    		
    		String result=null;
    		QRCodeReader reader=new QRCodeReader();
    		Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
            tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    		
    		
    		int cnt=0;
    		BufferedImage qrImage=file.getSubimage((int)((pontosRef.get(1).centrox+175)*prop), (int)(Math.max(0,(pontosRef.get(1).centroy-130)*prop)), (int)(160*prop), (int)(160*prop));
    		//clone
    		BufferedImage small=qrImage.getSubimage(0, 0, qrImage.getWidth(), qrImage.getHeight());
    		BufferedImage bwImg=null;
    		while(result==null){
    			
    			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(small)));
    			
    			/* salva imagem zoada na pasta pessoal */
//    			try{
//    				int cnt2=0;
//        			File outputfile = new File("/home/samuelkato/qrZoado-"+(cnt2)+".png");
//        			while(outputfile.exists()){
//        				outputfile = new File("/home/samuelkato/qrZoado-"+(++cnt2)+".png");
//        			}
//        		    ImageIO.write(small, "png", outputfile);
//        		    
//        		    File fileSaida=new File("/home/samuelkato/qrZoado-"+(cnt2)+".txt");
//    				if (!fileSaida.exists())fileSaida.createNewFile();
//    				
//    				FileWriter fw = new FileWriter(fileSaida.getAbsoluteFile());
//    				BufferedWriter bw = new BufferedWriter(fw);
//    				
//    				bw.write(bitmap.toString());
//    				bw.close();
//    				
//    			}catch(Exception e3){
//    				System.out.println(e3.getMessage());
//    			}
    			/* fim salva imagem zoada na pasta pessoal */
    			
    			class configThresh extends ConfigImageProcessing{
    				public boolean checkThreshold(int r, int g, int b, int rAvg, int gAvg, int bAvg){
    					return r + g + b < (rAvg + gAvg + bAvg - 50) && !(r>g+10 && r>b+10 && r>150);
    				}
    			}
    			
    			try{
    				result=reader.decode(bitmap,tmpHintsMap).getText();
    			}catch(Exception e){
    				if(cnt==0){
    					System.out.println("qr preto e branco");
    					ImageProcessing tmp=new ImageProcessing(qrImage);
    	        		tmp.createMatrix(new configThresh());
    	        		small=tmp.matrix2img(tmp.m);
    	        		bwImg=small.getSubimage(0, 0, small.getWidth(), small.getHeight());
    				}else if(cnt==1){
    					System.out.println("-redimensionado 200x200");
    					small=ImageProcessing.criarImagemRedimensionada(small, 200);
    				}else if(cnt==2){
    					System.out.println("-rotacionado 30");
    					small=ImageProcessing.rotate(small, 30);
    				}else if(cnt==3){
    					System.out.println("-100x100");
    					small=ImageProcessing.criarImagemRedimensionada(small, 100);
    				}else if(cnt==4){
    					System.out.println("-rotacionado 45");
    					small=ImageProcessing.rotate(small, 45);
    				}else if(cnt==5){
    					System.out.println("qr preto e branco e redimensionado 100x100");
    					small=ImageProcessing.criarImagemRedimensionada(bwImg, 100);
    				}else if(cnt==6){
    					System.out.println("-rotacionado 45");
    					small=ImageProcessing.rotate(small, 45);
    				}else if(cnt==7){
    					System.out.println("qr redimensionado 100x100");
    					small=ImageProcessing.criarImagemRedimensionada(qrImage, 100);
    				}else if(cnt==8){
    					System.out.println("redimensionado 100x100 e rotacionado 45");
    					small=ImageProcessing.rotate(small, 45);
    				}else if(cnt==9){
    					System.out.println("qr rotacionado 45");
    					small=ImageProcessing.rotate(qrImage, 45);
    				}else{
    					result="";
    				}
    				cnt++;
    			}
    		}
    		
    		//System.out.println((System.nanoTime()-startTime)/1000000000);
    		//inverter qr de alunoID:provaID => provaID:alunoID
    		 String[] parts = result.split(":");
    		 if(parts.length==2){
    			 int p1 = Integer.parseInt(parts[0]);
    			 int p2 = Integer.parseInt(parts[1]);
    			 if(p2>p1)result = ""+p2+":"+p1;
    		 }
    		
    		return result;
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
    		x+=1;//o x estah um pouco à esquerda
    		for(int i=x-4;i<x+5;i++){
    			for(int j=y-2;j<y+3;j++){
    				if(m[j][i])cnt++;
    				//else m[j][i]=true;
    			}
    		}
    		return (float)cnt/45;
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
}