import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;


public class Generator {
	public static void main(String[] args) {
		String data="";
		if(args.length==0){
			data="7441:20994";
		}else{
			data=args[0];
		}
		
		BitMatrix matrix;
		com.google.zxing.Writer writer = new QRCodeWriter();
		try {
			Hashtable<EncodeHintType,ErrorCorrectionLevel> hints = new Hashtable<EncodeHintType,ErrorCorrectionLevel>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			//EncodeHintType.ERROR_CORRECTION -> ErrorCorrectionLevel.H
			matrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 100, 100, hints);
		}
		catch (com.google.zxing.WriterException e) {
		 //exit the method
		 return;
		}

		//generate an image from the byte matrix
		int width = matrix.getWidth(); 
		int height = matrix.getHeight(); 


		//create buffered image to draw to
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		//iterate through the matrix and draw the pixels to the image
		for (int y = 0; y < height; y++) { 
			for (int x = 0; x < width; x++) {
			 //System.out.print(matrix.get(x, y)?'■':' ');
			 image.setRGB(x, y, (matrix.get(x, y) ? 0 : 0xFFFFFF));
		 	}
		 //System.out.println();
		}

		//write the image to the output stream
		try {
			ImageIO.write(image, "png", System.out);
			if(args.length==0)ImageIO.write(image, "png", new File("/home/samuelkato/aoba.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
