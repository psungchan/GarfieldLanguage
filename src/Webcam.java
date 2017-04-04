import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * A live updating openCV webcam. Base code comes from Ethan Lee (https://github.com/ethanlee16).
 * I optimized some things.
 *
 * @author Lukas Strobel
 * @since 4/3/2017
 */
public class Webcam extends JPanel {

   private static BufferedImage image;
   private static BufferedImage pastImage;
   private static BufferedImage thisFrame;

   public Webcam() {
      super();
   }

   public static void main(String args[]) {
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
      Webcam panel = new Webcam();

      // Initialize JPanel
      JFrame frame = new JFrame("Webcam");
      frame.setSize(1080, 720);
      frame.setContentPane(panel);
      frame.setVisible(true);

      VideoCapture camera = new VideoCapture(0);
      // Special window listener because there are threads that need to be shutdown on a close
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            e.getWindow().dispose();
            camera.release();
            System.exit(0);
         }
      });

      if (camera.isOpened()) {
         // Create SwingWorker to encapsulate the process in a thread
         SwingWorker<Void, Mat> worker = new SwingWorker<Void, Mat>() {
            @Override
            protected Void doInBackground() throws Exception {

               // Put something into thisFrame so it doesn't glitch at the beginning
               Mat firstFrame = new Mat();
               camera.read(firstFrame);
               thisFrame = panel.matrixToBuffer(firstFrame);

               Mat currentImage = new Mat();
               while (!isCancelled()) {
                  camera.read(currentImage); // Get camera image
                  if (!currentImage.empty()) {
                     frame.setSize(currentImage.width() + 40, currentImage.height() + 60);
                     pastImage = thisFrame;
                     thisFrame = panel.matrixToBuffer(currentImage);
                     image = diff(pastImage,thisFrame);
                     panel.repaint(); // Refresh
                  } else {
                     System.err.println("Error: no frame captured");
                  }
                  Thread.sleep(70); // Set refresh rate, as well as prevent the code from tripping over itself
               }
               return null;
            }
         };
         worker.execute();
      }
      return;
   }

   /**
    * Differences the pixels of an image by another.
    *
    * @param current  The current frame.
    * @param previous The previous frame.
    * @return The modified frame with diff pixels.
    */
   public static BufferedImage diff(BufferedImage current, BufferedImage previous) {
      int width = current.getWidth();
      int height = current.getHeight();
      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            Color c1 = new Color(current.getRGB(x, y));
            Color c2 = new Color(previous.getRGB(x, y));
            int gray1 = toGrayscale(c1);
            int gray2 = toGrayscale(c2);
            int diff = Math.abs(gray1 - gray2);
            diff = threshold(diff);
            Color dC = new Color(diff, diff, diff);
            current.setRGB(x, y, dC.getRGB());
         }
      }
      return current;
   }

   private static int toGrayscale(Color color) {
      int red = (int) (color.getRed() * 0.3);
      int green = (int) (color.getGreen() * 0.6);
      int blue = (int) (color.getBlue() * 0.1);
      return red + green + blue;
   }

   private static int threshold(int gray) {
      return gray < 10 ? 255 : 0;
   }

   /**
    * Converts/writes a Mat into a BufferedImage.
    *
    * @param matBGR Mat of type CV_8UC3 or CV_8UC1
    * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
    */
   public BufferedImage matrixToBuffer(Mat matBGR) {
      int type = BufferedImage.TYPE_BYTE_GRAY;
      if (matBGR.channels() > 1) {
         type = BufferedImage.TYPE_3BYTE_BGR;
      }
      int width = matBGR.width(), height = matBGR.height(), channels = matBGR.channels();
      byte[] sourcePixels = new byte[width * height * channels];
      matBGR.get(0, 0, sourcePixels);

      // Create new image and get reference to backing data
      image = new BufferedImage(width, height, type);
      final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
      System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
      return image;
   }

   @Override
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (image == null) {
         return;
      }

      g.drawImage(image, 10, 10, image.getWidth(), image.getHeight(), null);
      g.setColor(Color.WHITE);
   }
}
