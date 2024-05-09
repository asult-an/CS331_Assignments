/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: Server.java
 *
 **************************************************/

import java.io.FileInputStream;
import java.util.Arrays;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.event.WindowEvent;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * The server program for the application-level protocol
 */
public class Server
{
    private RDT rdt;     // the server-side rdt instance
    private String fileName;
    /**
     * Constructor: creates the rdt instance with its IP address and the port
     *              number of its receiver, as well as the port number of its
     *              peer's receiver; then the thread sleeps of 0.1 second.
     */
    public Server(String ipAddress, 
                  int rcvPortNum, 
                  int peerRcvPortNum) throws Exception
    {
        this.rdt = new RDT(ipAddress, rcvPortNum, peerRcvPortNum, "SERVER");
        Thread.sleep(100);
    }// constructor

    /**
     * Implements the server-side of the application-level protocol as 
     * described in the handout. The messages to be sent to the console are
     * also specified in the traces given in the handout, namely, the lines
     * starting with the string "SERVER ".
     *
     * When the server is done, make the thread wait for two seconds before
     * terminating the program.
     */
    public void run() throws Exception
    {
        System.out.println("Server started");
        while (true) {
            //System.out.println("SERVER Waiting for a file request...");
            getRequest();
            String filename = getRandomImageFile();
            // if (filename == null) {
            //     System.out.println("SERVER: No image files found.");
            //     return;
            // }
            System.out.println("SERVER: Sending file name: " + filename);
            sendFileName(filename);
            try (FileInputStream f = new FileInputStream(new File(A5.IMG_SUBFOLDER, filename))) {
                sendFile(f);
            }
            System.out.println("SERVER File sent, waiting for 2 seconds...");
            Thread.sleep(2000); 
            break; 
        }
        System.out.println("Server shutting down");
    }// run

    // Do not modify this method
    private String getRandomImageFile()
    {
        File dir = new File(A5.IMG_SUBFOLDER);
        String [] entireFileList = dir.list();
        String [] imgFileList = new String[entireFileList.length];
        int imgCount = 0;
        
        for( int i = 0; i < entireFileList.length; i++ )
        {
            String filename = entireFileList[i].toLowerCase();
            if ( filename.endsWith(".jpg") ||
                 filename.endsWith(".jpeg") ||
                 filename.endsWith(".gif") ||
                 filename.endsWith(".png"))
                imgFileList[imgCount++] = entireFileList[i];
        }
        if (imgCount == 0)
            return null;
        int index = new java.util.Random().nextInt(imgCount);
        return imgFileList[0];
    }// getRandomImageFile

    // Do not modify this method
    public static void main(String[] args)
    {
        try
        {
            new Server(args.length != 1 ? null : args[0],
                       A5.SERVER_RCV_PORT_NUM,
                       A5.SERVER_PEER_RCV_PORT_NUM).run();
        }
        catch (Exception e)
        {
            A5.print("S","SERVER closing due to error in main: "
                               + e);
        }
    }// main

    /******************************************************************
     *   private methods
     ******************************************************************/

    // sends to the client, the name of the image file given as parameter
    private void sendFileName(String inFileName)
    {
        fileName = inFileName;
        rdt.sendData(inFileName.getBytes());
        System.out.println("SERVER: File name sent");
    }// sendFileName

    // sends to the client the chunk(s) of the file to be read from the given
    // input stream
    private void sendFile(FileInputStream in) throws Exception
    {   
        System.out.println("sendfile");
        int total = 0;
        byte[] buffer = new byte[8192]; 
        int bytesRead ;//= in.read(buffer);
        while ((bytesRead = in.read(buffer)) != -1) {
            //System.out.print("read: "+bytesRead);
            rdt.sendData(Arrays.copyOf(buffer, bytesRead));
            System.out.println("SERVER Sent a chunk of size " + bytesRead);
            total += bytesRead;
        }
        System.out.println(total);
        rdt.sendData("done".getBytes());
        in.close();
        System.out.println("SERVER done sending the file "+fileName);
    }// sendFile

    // waits for the file request from the client.
    // As explained in the handout, uses a flag-based loop that causes the
    // thread to yield until a MSG_REQUEST_IMG_FILE sent by the client is
    // received
    private void getRequest()
    {
        
        byte[] requestData = rdt.receiveData();
        System.out.println("message: "+ new String(requestData,0,requestData.length));//TODO remove this line
        System.out.println("SERVER looping in getRequest");
        System.out.println("SERVER got request for image file");
    }// getRequest

}// Server
