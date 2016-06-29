import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.SwingWorker;

/**
 * classe para processar imagens de uma pasta
 * 
 * @author samuelkato
 */
class ProcessaFolder extends SwingWorker<Void, Void> {
	private File folder;
	private ZipOutputStream zipSaida;
	public int nConsumidores = 2;
	public Reader reader;
	/**
	 * contrutora da classe
	 * 
	 * @param folder pasta a ser lida
	 * @param reader 
	 */
	ProcessaFolder(File folder, Reader reader){
		this.folder=folder;
		this.reader = reader;
	}

	/**
	 * Executed in event dispatching thread
	 */
	@Override
	public void done() {
		Toolkit.getDefaultToolkit().beep();
		this.reader.startButton.setEnabled(true);
		this.reader.setCursor(null); //turn off the wait cursor
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
		
		this.reader.taskOutput.append("Inicio do processamento na pasta: "+pastaAt+"\n");
		System.out.println("Inicio do processamento na pasta: "+pastaAt);
		
		final File[] a=folder.listFiles();
		Pool pool = new Pool(zipSaida, this);
		Produtor p = new Produtor(a,pool);
		p.start();
		Consumidor[] aC = new Consumidor[nConsumidores];
		for(int i = 0; i < nConsumidores; i++){
			aC[i] = new Consumidor(pool);
			aC[i].start();
		}
		String conteudo = "";
		for (Consumidor c : aC) {
		    try {
				c.join();
				if(conteudo.length()>0)conteudo+=",\n";
				conteudo+=c.saida;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String saida="[\n"+conteudo+"\n]";
		this.reader.taskOutput.append("Fim da pasta "+pastaAt+"\n\n");
		System.out.println("Fim da pasta "+pastaAt+"\n");
		
		
		//File fileSaida=new File(pastaAt+"/saida.json");
		try {
			//if (!fileSaida.exists())fileSaida.createNewFile();
			//saida = new String(saida.getBytes(), "UTF-8");
			zipSaida.putNextEntry(new ZipEntry("saida.json"));
			zipSaida.write(saida.getBytes("UTF-8"));
			zipSaida.closeEntry();
			
			//taskOutput.append("Json gravado com sucesso em: "+fileSaida.getAbsolutePath()+"\n\n");
			//System.out.println("Json gravado com sucesso em: "+fileSaida.getAbsolutePath()+"\n");
		} catch (IOException e) {
			e.printStackTrace();
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
	
	public void mudarProgresso(int p){
		setProgress(Math.min(p, 100));
	}
	
}