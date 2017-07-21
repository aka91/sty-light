package com.example;

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
import java.io.*;
import java.net.*;

public class javaservertho {

    public static void main(String[] args) throws IOException {
        int filesize=6022386; // filesize temporary hardcoded

        long start = System.currentTimeMillis();
        int bytesRead;
        int current = 0;
        String DIRECTORY = "\\Users\\fatemehdarbehani\\Desktop\\MediaProjectionDemo\\";

        int IMAGES_PRODUCED = 0;

        String path = "/Users/fatemehdarbehani/Desktop/MediaProjectionDemo/10.png";
        File logo_file = new File(path);
        BufferedImage image = ImageIO.read(logo_file);
        Image dimg = image.getScaledInstance(500, 900, Image.SCALE_SMOOTH);
        ImageIcon imageIcon = new ImageIcon(dimg);
        JLabel label = new JLabel(imageIcon);

        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(label);
        frame.pack();
        frame.setLocation(200,200);
        frame.setSize(500, 900);
        frame.setVisible(true);

        byte [] mybytearray  = new byte [filesize];
        // create socket
        ServerSocket servsock = new ServerSocket(5000);
        while (true) {
            //System.out.println("Waiting...");

            Socket sock = servsock.accept();
            //System.out.println("Accepted connection : " + sock);

            // receive file

            InputStream is = sock.getInputStream();
            //IMAGES_PRODUCED = IMAGES_PRODUCED % 10;
            //---------------------------------------
            // create new file'
            //File f = null;
            //boolean bool = false;
            //f = new File(IMAGES_PRODUCED + ".png");
            // tries to delete a non-existing file
            //bool = f.delete();
           // System.out.println(bool);

            //---------------------------------------

            //FileOutputStream fos = new FileOutputStream(IMAGES_PRODUCED + ".png"); // destination path and name of file

            //BufferedOutputStream bos = new BufferedOutputStream(fos);
            bytesRead = is.read(mybytearray,0,mybytearray.length);
            current = bytesRead;


            do {
                bytesRead =
                        is.read(mybytearray, current, (mybytearray.length-current));
                if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);

            image = ImageIO.read(new ByteArrayInputStream(mybytearray));

            //dimg = image.getScaledInstance(500, 900, Image.SCALE_SMOOTH);
            imageIcon = new ImageIcon(image);

            label.setIcon(imageIcon);
            //frame.getContentPane().add(label);
            //frame.pack();
            //frame.setVisible(true);

            //System.out.println("Received " + image.getHeight() + "x" + image.getWidth() + ": " + System.currentTimeMillis());
            //ImageIO.write(image, "jpg", new File(IMAGES_PRODUCED + ".png"));
            //IMAGES_PRODUCED++;
            //bos.write(mybytearray, 0 , current);
            //bos.flush();
            //long end = System.currentTimeMillis();
            //System.out.println(end-start);
            //bos.close();
            sock.close();
        }
    }

}