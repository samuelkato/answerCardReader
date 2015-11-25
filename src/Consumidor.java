

public class Consumidor extends Thread {
	Pool pool;
	String saida = "";
	public Consumidor(Pool pool) {
		this.pool = pool;
	}
	@Override
	public void run() {
		while(pool.aindaTem()){
			ImageProcessing clImg = pool.get();
			if(clImg != null){
				LerCartao cartao = new LerCartao(clImg, pool);
				String md5Hash = cartao.md5Hash();
				String nomeImgZip = pool.salvarZip(md5Hash, "jpg", clImg.img);
				if(saida.length()!=0)saida+=",\n";
				String saidaAt ="{\"file\":\""+nomeImgZip+"\","+cartao.getSaida()+"}";
				pool.addMsg(saidaAt);
				saida+="\t"+saidaAt;
			}
			
		}
	}
}
