/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: Server.java
 *
 **************************************************/

import java.io.FileInputStream;
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
        
        rdt = new RDT(ipAddress, rcvPortNum, peerRcvPortNum, "SERVER");
        Thread.sleep(100);
        // To be completed
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
        System.out.println("SERVER");
        try {
            getRequest();
    
            String imgFileName = getRandomImageFile();
            if (imgFileName != null) {
                System.out.println("SERVER: Found image file: " + imgFileName);
                sendFileName(imgFileName);
    
                File imgFile = new File(A5.IMG_SUBFOLDER + imgFileName);
                try (FileInputStream fileInputStream = new FileInputStream(imgFile)) {
                    sendFile(fileInputStream);
                } catch (Exception e) {
                    System.err.println("SERVER: Error sending image file: " + e);
                }
            } else {
                System.out.println("SERVER: No image file available.");
                rdt.sendData(new byte[]{A5.MSG_NO_IMG_FILE_AVAILABLE});
            }
    
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("SERVER: Error during execution: " + e.getMessage());
            throw e;
        }
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
        return imgFileList[index];
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
        try {
            byte[] fileNameBytes = inFileName.getBytes();
            byte[] message = new byte[fileNameBytes.length + 1];
            message[0] = A5.MSG_FILE_NAME;
            System.arraycopy(fileNameBytes, 0, message, 1, fileNameBytes.length);
            rdt.sendData(message);
        } catch (Exception e) {
            System.err.println("Error sending filename: " + e.getMessage());
        }
        // To be completed
    }// sendFileName

    // sends to the client the chunk(s) of the file to be read from the given
    // input stream
    private void sendFile(FileInputStream in) throws Exception
    {
        try {
            byte[] buffer = new byte[A5.MAX_DATA_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] message = new byte[bytesRead + 1];
                message[0] = A5.MSG_FILE_DATA;
                System.arraycopy(buffer, 0, message, 1, bytesRead);
                rdt.sendData(message);
            }
            rdt.sendData(new byte[]{A5.MSG_FILE_DONE});
            in.close();
            System.out.println("SERVER: Done sending the file " + fileName);
        } catch (Exception e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
        // To be completed
    }// sendFile

    // waits for the file request from the client.
    // As explained in the handout, uses a flag-based loop that causes the
    // thread to yield until a MSG_REQUEST_IMG_FILE sent by the client is
    // received
    private void getRequest()
    {
        try {
            boolean requestReceived = false;
            while (!requestReceived) {
                byte[] message = rdt.receiveData();
                if (message.length == 1 && message[0] == A5.MSG_REQUEST_IMG_FILE) {
                    requestReceived = true;
                    // Log the message
                    System.out.println("message: " + new String(message, 0, message.length));
                    System.out.println("SERVER looping in getRequest");
                    System.out.println("SERVER got request for image file");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting request: " + e.getMessage());
        }
    
        // To be completed
    }// getRequest

}// Server
