import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.beans.*;
import java.util.Random;

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
    
    class ProcessaFolder extends SwingWorker<Void, Void> {
    	private File folder;
    	ProcessaFolder(File folder){
    		this.folder=folder;
    	}
    	
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
        	String saida="[\n"+listFilesForFolder(this.folder)+"\n]";
			try {
				File fileSaida=new File(this.folder.getAbsolutePath()+"/saida.json");
				if (!fileSaida.exists())fileSaida.createNewFile();
				
				FileWriter fw = new FileWriter(fileSaida.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				
				bw.write(saida);
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            return null;
        }
 
        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            startButton.setEnabled(true);
            setCursor(null); //turn off the wait cursor
            taskOutput.append("Done!\n");
        }
        

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
    				try {
    					BufferedImage file = ImageIO.read(fileEntry);
    					String retAt = processaImg(file);
    					// em caso de erro nao adiciona o arquivo aki
    					if (!saida.equals(""))
    						saida += ",\n";
    					saida += "\t"+retAt;
    				} catch (Exception e) {
    					System.out.println("erro ao processar o arquivo "
    							+ fileEntry.getAbsolutePath() + "\n"
    							+ e.getMessage());
    				}// ignora arquivos nao imagem e outros erros
    			}
    			int progress=(int)Math.floor(((float)++n/a.length)*100);
    			setProgress(Math.min(progress, 100));
    		}
            return saida;
        }
    	
    	private String processaImg(BufferedImage file) {
    		//long startTime = System.nanoTime();
    		
    		String qr=lerQr(file);
    		
    		BufferedImage resizedImage=criarImagemRedimensionada(file);
    		ImageProcessing clImg=new ImageProcessing(resizedImage);
    		
    		//float tempo=((float)System.nanoTime() - startTime)/1000000000;
    		
    		//String saida=String.format(Locale.US,"{\"qr\":\"%1$s\",\"tempo\":%2$f,\"questoes\":[%3$s]}",qr,tempo,lerCartao(clImg));
    		String saida=String.format(Locale.US,"{\"qr\":\"%1$s\",\"questoes\":[%2$s]}",qr,lerCartao(clImg));
    		
    		return saida;
    	}
    	
    	private List<Region> procurar3pontos(ImageProcessing clImg){
    		ConfigImageProcessing config=new ConfigImageProcessing();
    		config.invertThreshold_=true;
    		config.minArea=1000;
    		config.maxArea=1800;
    		boolean[][] mInv=clImg.createMatrix(config);
    		int[][] bwInv=clImg.bwlabel(mInv);
    		//clImg.saveFilteredImage("/home/samuelkato/tmp.bmp",mInv);
    		//clImg.saveFilteredImage("/home/samuelkato/tmp.bmp",bwInv);
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
    		
    		
    		//System.out.println(ret);
    		
    		//check perpendicular
    		return ret;
    	}
    	
    	private BufferedImage criarImagemRedimensionada(BufferedImage file){
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

    		return resizedImage;
    	}
    	private BufferedImage criarRegiaoQr(BufferedImage file){
    		int width=file.getWidth();
    		int height=file.getHeight();
    		BufferedImage resizedImage = new BufferedImage(400, 400, 5);
    		Graphics2D g = resizedImage.createGraphics();
    		g.drawImage(file, 0, 0, width/2, height/2, null);
    		g.dispose();
    		
    		return resizedImage;
    	}
    	private String lerQr(BufferedImage file){
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

    	private String lerCartao(ImageProcessing clImg){
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
    		String saida="";
    		for (int i=0;i<col1.size()*4;i++) {
    			String alts="";
    			if(resp.get(i)!=null){
    				for(int j=0;j<resp.get(i).size();j++){
    					if(j>0)alts+=",";
    					alts+=resp.get(i).get(j);
    				}
    			}
    			if(i>0)saida+=",";
    			saida+=String.format("[%1$s]", alts);
    		}
    		return saida;
    	}
    	
    	private float conferirMarcacao(int x,int y, boolean[][] m){
    		int cnt=0;
    		for(int i=x-5;i<x+5;i++){
    			for(int j=y-3;j<y+3;j++){
    				if(m[j][i])cnt++;
    			}
    		}
    		return (float)cnt/60;
    	}
    }
    
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	//Create and set up the window.
                JFrame frame = new JFrame("ProgressBarDemo");
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
	/*
	public static void main(String[] args) {
		new Reader();
	}
	*/
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
            taskOutput.append(String.format("Completed %d%% of task.\n", task.getProgress()));
        }
    }
	
	public Reader(){
		super(new BorderLayout());
		 
        //Create the demo's UI.
        startButton = new JButton("Start");
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