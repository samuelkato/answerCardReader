import java.io.File;

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
				try{
					pool.add(new ImageProcessing(fileEntry, false));
				}catch(Exception e){
					pool.retiraTotal(1);
					System.out.println(fileEntry.getAbsolutePath()+" não é uma imagem");
				}
			}
		}
		this.pool.setFim(0);
	}

}
