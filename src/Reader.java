/**
 * cria zip saida
 * melhoria na deteccao de cores
 * 
 */

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
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
	private ProcessaFolder task;
   
	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//Create and set up the window.
				JFrame frame = new JFrame("Ler Folha Resposta v6.5 paralelo");
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
		
		chkDebug = new JCheckBox("Debug");
		chkDebug.setToolTipText("Criar Imagens de Diagnóstico");
		
		JPanel panel = new JPanel();
		panel.add(startButton);
		panel.add(progressBar);
		panel.add(chkDebug);
 
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
				task = new ProcessaFolder(chooser.getSelectedFile(), this, this.chkDebug.isSelected());
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