import java.awt.image.BufferedImage;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;


public class Pool {
	private Vector<ImageProcessing> contents;
	private int cnt = 1;
	private int total = 1;
	private int jaFoi = 0;
	ZipOutputStream zipSaida;
	ProcessaFolder processaFolder;
	public Pool(ZipOutputStream zipSaida, ProcessaFolder processaFolder) {
		this.contents = new Vector<ImageProcessing>();
		this.zipSaida = zipSaida;
		this.processaFolder = processaFolder;
	}
	public synchronized ImageProcessing get() {
		while (contents.size() == 0) {
			if(!aindaTem())return null;
			try {
				wait();
			}
			catch (InterruptedException e) {
			}
		}
		processaFolder.mudarProgresso((int)Math.floor(((float)++jaFoi/total)*100));
		ImageProcessing ret = contents.remove(0);
		notifyAll();
		return ret;
	}
	public synchronized void add(ImageProcessing value) {
		while(contents.size()>5){
			System.out.println("parou de add");
			try {
				wait();
			}
			catch (InterruptedException e) {
			}
		}
		contents.add(value);
		notifyAll();
	}
	public synchronized void setFim(int n){
		notifyAll();
		cnt = n;
	}
	public synchronized boolean aindaTem() {
		return cnt>0 || contents.size() > 0;
	}
	public synchronized String salvarZip(String nome, String formato, BufferedImage im){
		String nomeImgZip =  nome + "." + formato;
		try{
			zipSaida.putNextEntry(new ZipEntry(nomeImgZip));
			ImageIO.write(im, formato, zipSaida);
			zipSaida.closeEntry();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return nomeImgZip;
	}
	public void setTotal(int n) {
		total = n;
	}
	public void retiraTotal(int n) {
		total -= n;
		processaFolder.mudarProgresso((int)Math.floor(((float)++jaFoi/total)*100));
	}
	public void addMsg(String saidaAt) {
		processaFolder.reader.taskOutput.append(saidaAt+"\n");
	}
	public void rotateImg(ImageProcessing clImg, int ang){
		clImg.rotate(ang);
	}
}
