package com.example;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.*;

import java.io.*;
import java.net.*;

public class javaservertho {

    public static void main(String[] args) throws IOException {
        int filesize=6022386; // filesize temporary hardcoded

        long start = System.currentTimeMillis();
        int bytesRead;
        int current = 0;
        String DIRECTORY = "\\Users\\Ahmadreza\\Downloads\\MediaProjectionDemo\\";

        int IMAGES_PRODUCED = 0;

        // create socket
        ServerSocket servsock = new ServerSocket(1149);
        while (true) {
            System.out.println("Waiting...");

            Socket sock = servsock.accept();
            System.out.println("Accepted connection : " + sock);

            // receive file
            byte [] mybytearray  = new byte [filesize];
            InputStream is = sock.getInputStream();
            IMAGES_PRODUCED = IMAGES_PRODUCED % 10;
            FileOutputStream fos = new FileOutputStream(DIRECTORY + IMAGES_PRODUCED + ".png"); // destination path and name of file
            IMAGES_PRODUCED++;
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bytesRead = is.read(mybytearray,0,mybytearray.length);
            current = bytesRead;


            do {
                bytesRead =
                        is.read(mybytearray, current, (mybytearray.length-current));
                if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);

            bos.write(mybytearray, 0 , current);
            bos.flush();
            long end = System.currentTimeMillis();
            System.out.println(end-start);
            bos.close();



            sock.close();
        }
    }

}