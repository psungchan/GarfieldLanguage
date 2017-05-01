import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

/**
 * A live updating openCV webcam. Base code comes from Ethan Lee (https://github.com/ethanlee16).
 * I optimized some things.
 *
 * @author Lukas Strobel
 * @since 4/3/2017
 */
public class Webcam extends JPanel {

   private static BufferedImage image; // The actual displayed image on screen
   private static CascadeClassifier faceCascade;
   private static CascadeClassifier eyesCascade;
   private static int absoluteFaceSize;

   public Webcam() {
      super();
      faceCascade = new CascadeClassifier("C:\\Users\\Lukas\\Documents\\Programming-Technology\\GarfieldLanguage\\resource\\haarcascade_frontalface_default.xml");
      eyesCascade = new CascadeClassifier("C:\\Users\\Lukas\\Documents\\Programming-Technology\\GarfieldLanguage\\resource\\haarcascade_eye.xml");
      absoluteFaceSize = 0;
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
               Imgproc.cvtColor(thisFrame, thisFrame, Imgproc.COLOR_BGR2GRAY); // Convert the diff to gray

               // Set up new Mats for diffing them later, manually set the amount of channels to avoid an openCV error
               Mat pastFrame = new Mat();
               Mat diff = new Mat();


               // isCancelled is set by the SwingWorker
               while (!isCancelled()) {

                  thisFrame.copyTo(pastFrame); // What was previously the frame is now the pastFrame
                  camera.read(thisFrame); // Get camera image, and set it to currentImage
                  Imgproc.cvtColor(thisFrame, thisFrame, Imgproc.COLOR_BGR2GRAY); // Convert the diff to gray

                  if (!thisFrame.empty()) {

                     // Set the frame size to have a nice border around the image
                     frame.setSize(thisFrame.width() + 40, thisFrame.height() + 60);


                     Core.absdiff(thisFrame, pastFrame, diff); // Diff the frames
                     Imgproc.GaussianBlur(diff, diff, new Size(7, 7), 7); // Despeckle
                     Imgproc.threshold(diff, diff, 5, 255, 1); // Threshhold the gray

                     image = matrixToBuffer(getFace(thisFrame, diff)); // Update the display image
                     panel.repaint(); // Refresh the panel
                  } else {
                     System.err.println("Error: no frame captured");
                  }
                  //Thread.sleep(70); // Set refresh rate, as well as prevent the code from tripping over itself
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

   /**
    * Detect and return a face
    *
    * @param frame The total webcam view. Must be in grayscale.
    * @return A Mat that contains only the face
    */
   public static Mat getFace(Mat frame, Mat drawFrame) {

      MatOfRect faces = new MatOfRect();

      // compute minimum face size (20% of the frame height)
      if (absoluteFaceSize == 0) {
         int height = frame.rows();
         if (Math.round(height * 0.2f) > 0) {
            absoluteFaceSize = Math.round(height * 0.2f);
         }
      }

      // detect faces
      faceCascade.detectMultiScale(frame, faces, 1.1, 3, 0 | CASCADE_SCALE_IMAGE,
            new Size(absoluteFaceSize, absoluteFaceSize), new Size());

      // each rectangle in faces is a face
      Rect[] facesArray = faces.toArray();
      for (int i = 0; i < facesArray.length; i++) {
         Point center = new Point(facesArray[i].x + (facesArray[i].width / 2), facesArray[i].y + facesArray[i].height / 2);

         Mat face = new Mat(frame, facesArray[i]);

         MatOfRect eyes = new MatOfRect();

         eyesCascade.detectMultiScale(face, eyes, 1.1, 2,
               0 | CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

         if (eyes.size().width > 0 && eyes.size().height > 0) {
            Imgproc.ellipse(drawFrame, center, new Size(facesArray[i].width / 2, facesArray[i].height / 2),
                  0, 0, 360, new Scalar(0, 0, 0), 4, 8, 0);
         }
      }
      return drawFrame;
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
