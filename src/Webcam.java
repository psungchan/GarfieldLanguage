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
      Mat currentImage = new Mat();

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
               while (!isCancelled()) {
                  camera.read(currentImage); // Get camera image
                  if (!currentImage.empty()) {
                     frame.setSize(currentImage.width() + 40, currentImage.height() + 60);
                     image = panel.matrixToBuffer(currentImage);
                     panel.repaint(); // Refresh
                  } else {
                     System.err.println("Error: no frame captured");
                  }
                  Thread.sleep(50); // Set refresh rate, as well as prevent the code from tripping over itself
               }
               return null;
            }
         };
         worker.execute();
      }
      return;
   }

   /**
    * Converts/writes a Mat into a BufferedImage.
    *
    * @param matBGR Mat of type CV_8UC3 or CV_8UC1
    * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
    */
   public BufferedImage matrixToBuffer(Mat matBGR) {
      int width = matBGR.width(), height = matBGR.height(), channels = matBGR.channels();
      byte[] sourcePixels = new byte[width * height * channels];
      matBGR.get(0, 0, sourcePixels);

      // Create new image and get reference to backing data
      image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
      System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
      return image;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (image == null) {
         return;
      }

      g.drawImage(image, 10, 10, image.getWidth(), image.getHeight(), null);
      g.setColor(Color.WHITE);
   }
}
