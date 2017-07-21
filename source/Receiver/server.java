import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Size;
import java.io.*;
import java.net.*;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferByte;
import java.awt.Dimension;
import javax.swing.border.LineBorder;
import java.awt.Color;

public class server {
	/**
	 * 
	 * @param bi
	 * @return
	 */
	public static Mat bufferedImageToMat(BufferedImage bi) {
		  Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		  byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		  mat.put(0, 0, data);
		  return mat;
		}
	/**
	 * 
	 * @param image
	 * @return
	 */
	public static BufferedImage matToBufferedImage(Mat image) {
        Mat image_tmp = image;
        MatOfByte matOfByte = new MatOfByte();

        Imgcodecs.imencode(".jpg", image_tmp, matOfByte);

        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;

        try {

            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bufImage;
    }
    public static void main(String[] args) throws IOException {
    		
    		final int FILE_SIZE = 6022386; // filesize temporary hardcoded
    		final String PATH = "resources/logo.jpg";
    		final int PORT = 5000;
    		final int SCREEN_WIDTH = 1280;
    		final int SCREEN_HEIGHT = 800;
    		final int ANDROID_WIDTH = 460;//512 * 1.5;
    		final int ANDROID_HEIGHT = 900;//900 * 1.5;
    		final int DISTORTION_FACTOR = 100;
    		
        int filesize = FILE_SIZE; 
        int bytesRead = 0;
        int current = 0;
        
        System.load("/usr/local/Cellar/opencv3/3.2.0/share/OpenCV/java/libopencv_java320.dylib");
        // ---------------------------------------------------- //
        // Find perspective transform to fix image distortion
        // Need 4 reference points and 4 destination points
        // ---------------------------------------------------- //
        // 4 Reference Points:
        Point TOP_LEFT = new Point(0, 0);
        Point TOP_RIGHT = new Point(SCREEN_WIDTH, 0);
        Point BOTTOM_LEFT = new Point(0, SCREEN_HEIGHT);
        Point BOTTOM_RIGHT = new Point(SCREEN_WIDTH, SCREEN_HEIGHT);
        
        MatOfPoint2f corners = new MatOfPoint2f();
        corners.push_back(new MatOfPoint2f(TOP_LEFT));
        corners.push_back(new MatOfPoint2f(TOP_RIGHT));
        corners.push_back(new MatOfPoint2f(BOTTOM_LEFT));
        corners.push_back(new MatOfPoint2f(BOTTOM_RIGHT));
        
        // 4 Destination Points:
        Point NEW_TOP_LEFT = new Point(0  , 0);
        Point NEW_TOP_RIGHT = new Point(SCREEN_WIDTH ,0);
        Point NEW_BOTTOM_LEFT = new Point(0 + DISTORTION_FACTOR, SCREEN_HEIGHT + 60);
        Point NEW_BOTTOM_RIGHT = new Point(SCREEN_WIDTH- DISTORTION_FACTOR * 3, SCREEN_HEIGHT + 60);
        
        MatOfPoint2f dest_pts = new MatOfPoint2f();

        dest_pts.push_back(new MatOfPoint2f(NEW_TOP_LEFT));
        dest_pts.push_back(new MatOfPoint2f(NEW_TOP_RIGHT));
        dest_pts.push_back(new MatOfPoint2f(NEW_BOTTOM_LEFT));
        dest_pts.push_back(new MatOfPoint2f(NEW_BOTTOM_RIGHT));
        
        
        // --------------------------------- //
        // Read Logo file in PATH to display //
        // --------------------------------- //
        // TO_DO: Find out how warp image without casting tyoe to bufferedImage!
        Mat src = Imgcodecs.imread(PATH, 1); 
        Mat dest = Mat.zeros(src.height(), src.width(), src.type());
        //System.out.println("src size: " + src.size());
        //System.out.println("dest size: " + dest.size());
        
        // Find the transform matrix
        // Set the lambda matrix the same type and size as input
        Mat transmtx = Mat.zeros( src.rows(), src.cols(), src.type() );
        transmtx = Imgproc.getPerspectiveTransform(corners, dest_pts);
        
        // Transform the image
        Imgproc.warpPerspective(src, dest, transmtx, dest.size());
        
        Core.flip(dest, dest, 0);
        BufferedImage image = matToBufferedImage(dest);
        Image dimg = image.getScaledInstance(dest.width(), dest.height(), Image.SCALE_SMOOTH);
        ImageIcon imageIcon = new ImageIcon(dimg);
        JLabel label = new JLabel(imageIcon);
        label.setBorder(new LineBorder(Color.BLACK));
        label.setSize(new Dimension(ANDROID_WIDTH, ANDROID_HEIGHT));
        //label.setSize(d);
        // --------------------------------------------- //
        // Create a JFrame and make it full screen
        // Then add the jlabel to it and make it visible
        // ----------------------------------------------//
        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        frame.setVisible(true);
        
        
        // ---------------------------------------------------- //
        // ---------------------------------------------------- //
        // Find perspective transform to fix screen distortion
        // Need 4 reference points and 4 destination points
        // ---------------------------------------------------- //
        // 4 Reference Points:
        TOP_LEFT = new Point(0, 0);
        TOP_RIGHT = new Point(ANDROID_WIDTH, 0);
        BOTTOM_LEFT = new Point(0, ANDROID_HEIGHT);
        BOTTOM_RIGHT = new Point(SCREEN_WIDTH, ANDROID_HEIGHT);
        
        corners = new MatOfPoint2f();
        corners.push_back(new MatOfPoint2f(TOP_LEFT));
        corners.push_back(new MatOfPoint2f(TOP_RIGHT));
        corners.push_back(new MatOfPoint2f(BOTTOM_LEFT));
        corners.push_back(new MatOfPoint2f(BOTTOM_RIGHT));
        
        // 4 Destination Points:
        NEW_TOP_LEFT = new Point(0, 50);
        NEW_TOP_RIGHT = new Point(ANDROID_WIDTH + 10, 50);
        NEW_BOTTOM_LEFT = new Point(0 + DISTORTION_FACTOR , ANDROID_HEIGHT - 70);
        NEW_BOTTOM_RIGHT = new Point(ANDROID_WIDTH + DISTORTION_FACTOR*4, ANDROID_HEIGHT - 70);
        
        dest_pts = new MatOfPoint2f();

        dest_pts.push_back(new MatOfPoint2f(NEW_TOP_LEFT));
        dest_pts.push_back(new MatOfPoint2f(NEW_TOP_RIGHT));
        dest_pts.push_back(new MatOfPoint2f(NEW_BOTTOM_LEFT));
        dest_pts.push_back(new MatOfPoint2f(NEW_BOTTOM_RIGHT));
        
        // Find the transform matrix for screen
        // Set the lambda matrix the same type and size as input
        Mat screen_transmtx = Mat.zeros( ANDROID_HEIGHT, ANDROID_WIDTH, src.type() );
        screen_transmtx = Imgproc.getPerspectiveTransform(corners, dest_pts);
        
        byte [] mybytearray  = new byte [filesize];
        // ------------------------------------------------- //
        // Create server socket to listen for a request on 
        // PORT
        // ------------------------------------------------- //
        ServerSocket servsock = new ServerSocket(PORT);
        
        BufferedImage screen;
        BufferedImage screen_dest;
        Mat screen_mat;
        Mat screen_mat_dest;
        
        while (true) {
            //System.out.println("Waiting...");

            Socket sock = servsock.accept();
            //System.out.println("Accepted connection : " + sock);
            // ------------------------------------------------------ //
            // Receive file: get input stream of the opened socket 
            // and read with maxSize of FILE_SIZE
            // Then keep reading until number of bytes returned by
            // read function is -1 (Nothing to read)
            //------------------------------------------------------- //
            InputStream is = sock.getInputStream();
   
            bytesRead = is.read(mybytearray,0,mybytearray.length);
            current = bytesRead;

            do {
                bytesRead =
                        is.read(mybytearray, current, (mybytearray.length-current));
                if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);
            
            // ------------------------------------------------------------- //
            // Convert the data read from the socket to image, create 
            // imageIcon and display it by calling label.setIcon()
            // ------------------------------------------------------------- //
            screen = ImageIO.read(new ByteArrayInputStream(mybytearray));
            screen_mat = bufferedImageToMat(screen);
            screen_mat_dest = Mat.zeros(screen_mat.rows(), screen_mat.cols(), screen_mat.type());
            //System.out.println("src size: " + screen_mat.size());
            //System.out.println("dest size: " + screen_mat_dest.size());
            
            Imgproc.warpPerspective(screen_mat, screen_mat_dest, screen_transmtx, screen_mat_dest.size());
            
            Core.flip(screen_mat_dest, screen_mat_dest, 0);
            screen_dest = matToBufferedImage(screen_mat_dest);
            dimg = screen_dest.getScaledInstance(SCREEN_WIDTH, SCREEN_HEIGHT, Image.SCALE_SMOOTH);
            imageIcon = new ImageIcon(dimg);
            label.setIcon(imageIcon);
            
            // Close the socket connection
            sock.close();
        }
    }

}
