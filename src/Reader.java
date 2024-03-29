/**
 * cria zip saida
 * melhoria na deteccao de cores
 * 
 */
//package br.com.novometodo.reader

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.beans.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;



public class Reader  extends JPanel implements ActionListener, PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 480869136325312005L;
	private JProgressBar progressBar;
	public JButton startButton;
	public JTextArea taskOutput;
	public JCheckBox chkDebug;
	public JCheckBox chkBlur;
	private ProcessaFolder task;
   
	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//Create and set up the window.
				JFrame frame = new JFrame("Ler Folha Resposta v6.8");
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
	
	static String addFromJar(String jarPath, String tmpName) {
		tmpName = System.getProperty("java.io.tmpdir")+"/"+tmpName;
		File fileOut = new File(tmpName);
        
        //System.out.println(win);
		InputStream in = Reader.class.getClassLoader().getResourceAsStream(jarPath);
        // always write to different location
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        try {
        	OutputStream out = new FileOutputStream(fileOut);
			while ((bytesRead = in.read(buffer)) != -1) {
			    out.write(buffer, 0, bytesRead);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //System.out.println(fileOut.toString());
        return fileOut.toString();
	}
	
	public Reader(){
		super(new BorderLayout());
		boolean win = System.getProperty("os.name").startsWith("Windows");
		if(win) {
			System.load(Reader.addFromJar("windows/opencv_java3415.dll","opencv_java3415.dll"));
		}else {
			System.load(Reader.addFromJar("linux/libopencv_java3415.so","libopencv_java3415.so"));
			//System.load(Reader.addFromJar("linux/libopencv_java453.so","libopencv_java453.so"));
			//System.load("/usr/lib/jni/libopencv_java420.so");
		}
		//System.load("/home/samuelkato/eclipse-workspace/answerCardReader/lib/linux/libopencv_java3415.so");
		//System.load("C:\\Users\\SAMUEL~1\\Desktop\\opencv\\build\\java\\x64\\opencv_java3415.dll");
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
		
		chkDebug = new JCheckBox("Debug");
		chkDebug.setToolTipText("Criar Imagens de Diagnóstico");
		
		chkBlur = new JCheckBox("Blur");
		chkBlur.setToolTipText("Borrar Imagens");
		
		JPanel panel = new JPanel();
		panel.add(startButton);
		panel.add(progressBar);
		panel.add(chkDebug);
		panel.add(chkBlur);
 
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
				task = new ProcessaFolder(chooser.getSelectedFile(), this, this.chkDebug.isSelected(), this.chkBlur.isSelected());
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
}