import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;


public class Produtor extends Thread {
	File[] a;
	Pool pool;
	public Produtor(File[] a, Pool pool) {
		this.a = a;
		this.pool = pool;
	}
	
	@Override
	public void run() {
		pool.setTotal(a.length);
		for (File fileEntry : a) {
			if (!fileEntry.isDirectory()) {
				BufferedImage file=null;
				try{
					file = ImageIO.read(fileEntry);
					file.getType();//soh pra levantar uma Exception e nao processa o arquivo
					pool.add(new ImageProcessing(file, false));
				}catch(Exception e){
					pool.retiraTotal(1);
					System.out.println(fileEntry.getAbsolutePath()+" não é uma imagem");
				}
			}
		}
		this.pool.setFim(0);
	}

}
