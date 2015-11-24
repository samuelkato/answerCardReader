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
				JFrame frame = new JFrame("Ler Folha Resposta v6.3");
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
					String retAt="";
					BufferedImage file=null;
					System.out.println("\nProcessando arquivo "+fileEntry.getAbsolutePath());
					try{
						file = ImageIO.read(fileEntry);
						file.getType();//soh pra levantar uma Exception e nao processa o arquivo
						LerCartao l = new LerCartao(file);
						l.start();
						//retAt = lerCartao(file);
						if(!retAt.equals("")){
							if (!saida.equals(""))saida += ",\n";
							saida += "\t"+retAt;
						}
					}catch(Exception e){
						if(e.getMessage()=="imagemRepetida"){
							retAt="\"Erro: uma imagem identica à " + fileEntry.getAbsolutePath() + " ja foi adicionada à saida.\"";
						}else{
							//retAt="\"Erro: " + fileEntry.getAbsolutePath() + " nao é imagem.\"";
						}
					}
					taskOutput.append(retAt+"\n");
				}
				int progress=(int)Math.floor(((float)++n/a.length)*100);
				setProgress(Math.min(progress, 100));
			}
			return saida;
		}
	}
}