import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;


public class ImageSubtract {
   BufferedImage image1;
   BufferedImage image2;
   int width;
   int height;

   public ImageSubtract() {
      try {
         File input1 = new File("C:\\Users\\Lukas\\Documents\\Programming-Technology\\GarfieldLanguage\\stand1.jpg");
         File input2 = new File("C:\\Users\\Lukas\\Documents\\Programming-Technology\\GarfieldLanguage\\lay.jpg");
         image1 = ImageIO.read(input1);
         image2 = ImageIO.read(input2);
         width = image1.getWidth();
         height = image1.getHeight();


         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               Color c1 = new Color(image1.getRGB(x, y));
               Color c2 = new Color(image2.getRGB(x, y));
               int gray1 = toGrayscale(c1);
               int gray2 = toGrayscale(c2);
               int diff = Math.abs(gray1 - gray2);
               diff = threshold(diff);
               Color dC = new Color(diff, diff, diff);
               image1.setRGB(x, y, dC.getRGB());
            }
         }

         File output = new File("C:\\Users\\Lukas\\Documents\\Programming-Technology\\Fun\\GarfieldLanguage\\output.jpg");
         ImageIO.write(image1, "jpg", output);
      } catch (Exception e) {
         System.err.println(e);
      }
   }

   public static void main(String[] args) {
      ImageSubtract obj = new ImageSubtract();
   }

   private int toGrayscale(Color color) {
      int red = (int) (color.getRed() * 0.3D);
      int green = (int) (color.getGreen() * 0.6D);
      int blue = (int) (color.getBlue() * 0.1D);
      return red + green + blue;
   }

   private int threshold(int gray) {
      return gray < 30 ? 255 : 0;
   }
}