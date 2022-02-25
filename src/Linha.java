import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class Linha {
	double angulo;
	double m;
	double c;
	Rect rectMin;
	Rect rectMax;
	List<MatOfPoint> pontos = new ArrayList<>();
	
	static public List<Linha> gerarLinhas(List<MatOfPoint> cnts) {
		List<Linha> linhas = new ArrayList<>();
		for(int i = 0; i < cnts.size(); i++) {
			MatOfPoint cntI = cnts.get(i);
			for(int j = i+1; j < cnts.size(); j++) {
				MatOfPoint cntJ = cnts.get(j);
				Linha linha = new Linha();
				linhas.add(linha);
				linha.addPonto(cntI);
				linha.addPonto(cntJ);
			}
		}
		for(Linha l : linhas) {
			for(int i = 0; i < cnts.size(); i++) {
				MatOfPoint cntI = cnts.get(i);
				l.addPonto(cntI);
			}
		}
		return linhas;
	}
	
	public MatOfPoint getContourBarra(Map<MatOfPoint, String> map) {
		for(MatOfPoint cnt : this.pontos) {
			if(map.get(cnt)=="/") return cnt;
		}
		return null;
	}
	
	public boolean addPonto(MatOfPoint cnt) {
		if(this.pontos.size()<2 || this.inLine(Imgproc.boundingRect(cnt))) {
			if(!this.pontos.contains(cnt)) {
				this.pontos.add(cnt);
			}
			this.setAngulo();
			return true;
		}
		return false;
	}
	
	public void setAngulo() {
		if(this.pontos.size()<2) return;
		this.rectMin = Imgproc.boundingRect(this.pontos.get(0));
		this.rectMax = rectMin;
		for(MatOfPoint cnt : this.pontos) {
			Rect rect = Imgproc.boundingRect(cnt);
			if(this.rectMin.x > rect.x) this.rectMin = rect;
			if(this.rectMax.x < rect.x) this.rectMax = rect;
		}
		double[] aAng = this.calcularAngulo(this.rectMin, this.rectMax);
		this.m = aAng[0];
		this.angulo = aAng[1];
		this.c = aAng[2];
	}
	
	public double[] calcularAngulo(Rect rectMin, Rect rectMax) {
		double m,angulo,c;
		if(rectMin.x > rectMax.x) {
			Rect rAux = rectMax;
			rectMax = rectMin;
			rectMin = rAux;
		}
		int cX0 = rectMin.x;
		int cY0 = rectMin.y + rectMin.height;
		int cX = rectMax.x;
		int cY = rectMax.y + rectMax.height;
		if(cX==cX0) m = 99999;
		else m = (double)(cY-cY0) / (double)(cX-cX0);
		angulo = Math.atan2(cY-cY0, cX-cX0) * 180 / Math.PI;
		//angulo = Math.atan(m) * 180 / Math.PI;
		c = cY0 - m * cX0;
		return new double[]{m,angulo,c};
	}
	
	public boolean inLine(Rect rect) {
		double[] aAng = this.calcularAngulo(this.rectMin, rect);
		if(Math.abs(aAng[1] - this.angulo) < 5 && Math.abs(aAng[2] - this.c) < 20) {
			return true;
		}
		aAng = this.calcularAngulo(this.rectMax, rect);
		if(Math.abs(aAng[1] - this.angulo) < 5 && Math.abs(aAng[2] - this.c) < 20) {
			return true;
		}
		return false;
	}
	
	public String getTexto(Map<MatOfPoint, String> map) {
		String ret = "";
		Collections.sort(this.pontos, (a,b) -> Imgproc.boundingRect(a).x - Imgproc.boundingRect(b).x);
		for(MatOfPoint cnt : this.pontos) {
			ret += map.get(cnt);
		}
		return ret;
	}
}
