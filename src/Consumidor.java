

public class Consumidor extends Thread {
	Pool pool;
	String saida = "";
	private Boolean debug_;
	public Consumidor(Pool pool, Boolean debug_) {
		this.pool = pool;
		this.debug_ = debug_;
	}
	@Override
	public void run() {
		while(pool.aindaTem()){
			ImageProcessing clImg = pool.get();
			if(clImg != null){
				LerCartao cartao = new LerCartao(clImg, pool, this.debug_);
				String md5Hash = cartao.md5Hash();
				String nomeImgZip = pool.salvarZip(md5Hash, "jpg", clImg.img);
				if( nomeImgZip != null ){
					if(saida.length()!=0)saida+=",\n";
					String saidaAt ="{\"file\":\""+nomeImgZip+"\","+cartao.getSaida()+"}";
					pool.addMsg(saidaAt);
					saida+="\t"+saidaAt;
				}
			}
		}
	}
}
