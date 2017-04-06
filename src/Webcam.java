import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

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

   private static BufferedImage image; // The actual displayed image on screen

   public Webcam() {
      super();
   }

   public static void main(String args[]) {
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Import openCV
      Webcam panel = new Webcam(); // Initialize itself

      // Initialize JPanel
      JFrame frame = new JFrame("Webcam");
      frame.setSize(200, 200);
      frame.setContentPane(panel);
      frame.setVisible(true);

      VideoCapture camera = new VideoCapture(0); // The camera

      // Attempt to set the frame size, but it doesn't really work. Only just makes it bigger
      camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1900);
      camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 1000);


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
               Mat thisFrame = new Mat();
               camera.read(thisFrame);

               // Set up new Mats for diffing them later, manually set the amount of channels to avoid an openCV error
               Mat pastFrame = new Mat(thisFrame.width(),thisFrame.height(), CvType.CV_8UC3);
               Mat diff = new Mat(thisFrame.width(),thisFrame.height(), CvType.CV_8UC3);

               // isCancelled is set by the SwingWorker
               while (!isCancelled()) {

                  thisFrame.copyTo(pastFrame); // What was previously the frame is now the pastFrame
                  camera.read(thisFrame); // Get camera image, and set it to currentImage

                  if (!thisFrame.empty()) {

                     // Set the frame size to have a nice border around the image
                     frame.setSize(thisFrame.width() + 40, thisFrame.height() + 60);

                     Core.absdiff(thisFrame, pastFrame, diff); // Diff the frames
                     Imgproc.cvtColor(diff, diff, Imgproc.COLOR_BGR2GRAY); // Convert the diff to gray
                     Imgproc.GaussianBlur(diff,diff,new Size(7,7) , 7); // Despeckle
                     Imgproc.threshold(diff, diff, 5, 255, 1); // Threshhold the gray

                     image = matrixToBuffer(diff); // Update the display image
                     panel.repaint(); // Refresh the panel
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

   private static int threshold(int gray) {
      return gray < 10 ? 255 : 0;
   }

   /**
    * Converts/writes a Mat into a BufferedImage.
    *
    * @param matBGR Mat of type CV_8UC3 or CV_8UC1
    * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
    */
   public static BufferedImage matrixToBuffer(Mat matBGR) {
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
