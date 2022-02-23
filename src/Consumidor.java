

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
		while(true){
			ImageProcessing clImg = pool.get();
			if(clImg == null) break;
			LerCartao cartao = new LerCartao(clImg, pool, this.debug_);
			//verificar o tipo de cartao para poder escolher qual imagem vai para o zip
			//ver qual cartao salvar
			String nomeImgZip = pool.salvarZip(clImg.fileName, "jpg", cartao.getTipo() == 1 ? clImg.img : clImg.imgOriginal);
			if( nomeImgZip != null ){
				if(saida.length()!=0)saida+=",\n";
				String saidaAt ="{\"file\":\""+nomeImgZip+"\","+cartao.getSaida()+"}";
				pool.addMsg(saidaAt);
				saida+="\t"+saidaAt;
			}
			clImg = null;
			cartao = null;
		}
	}
}
