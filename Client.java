/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: Client.java
 *
 **************************************************/

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.event.WindowEvent;

/**
 * The client program for the application-level protocol
 */
public class Client
{
    private static final String MSG_REQUEST_IMG_FILE = null;
    private RDT rdt;                   // the client-side rdt instance 
    private String fileName;           // name of the image file to be received
    private JFrame frame;              // window to display the image
    private ByteArrayOutputStream out; // stream used to store the data bytes in
      // the image file into a byte array to be displayed in the frame above

    /**
     * Constructor: creates the rdt instance with its IP address and the port
     *              number of its receiver, as well as the port number of its
     *              peer's receiver; then the thread sleeps of 0.1 second.
     */
    public Client(String ipAddress, 
                  int rcvPortNum,
                  int peerRcvPortNum) throws Exception
    {
         rdt = new RDT(ipAddress, rcvPortNum, peerRcvPortNum, "CLIENT");
        Thread.sleep(100);
        // To be completed
    }// constructor

    /**
     * Implements the client-side of the application-level protocol as 
     * described in the handout. The messages to be sent to the console are
     * also specified in the traces given in the handout, namely, the lines
     * starting with the string "CLIENT ".
     *
     * If the server has an image file:
     * 1) loop on the file chunks, writing them to disk (and sending 
     *    appropriate messages to the console), making sure to have the thread
     *    yield after each block.
     * 2) display the image for two seconds (using the Thread.sleep method)
     * 3) close the frame before exiting the program.
     */
    public void run() throws Exception
    {
        System.out.println("CLIENT");
        // Step 1: Send file request to the server
        sendFileRequest();
        
        // Step 2: Receive file name from the server
        boolean fileNameReceived = getFileName();
        
        if (!fileNameReceived) {
            System.out.println("CLIENT: No image file available.");
            return;
        }
    
        System.out.println("CLIENT: Image file name received: " + fileName);
    
        // Step 3: Receive file data from the server and write to disk
        ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
        boolean fileTransferComplete = false;
        while (!fileTransferComplete) {
            byte[] fileData = rdt.receiveData();
            if (fileData != null && fileData[0] == A5.MSG_FILE_DATA) {
                fileOutputStream.write(fileData, 1, fileData.length - 1);
                // Send ACK to server
                byte[] ack = { A5.MSG_FILE_DATA };
                rdt.sendData(ack);
            } else if (fileData != null && fileData[0] == A5.MSG_FILE_DONE) {
                System.out.println("CLIENT: File transfer complete.");
                fileTransferComplete = true;
            } else {
                System.out.println("CLIENT: Unexpected message received: " + Arrays.toString(fileData));
                // Send NACK to server or take appropriate action
                // Handle unexpected message
            }
        }
        out = fileOutputStream;
    
        // Step 4: Display the image for two seconds
        displayImage(out.toByteArray());
        Thread.sleep(2000);
    
        // Step 5: Close the frame before exiting the program
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        
        // Additional logic from the provided run method
        System.out.println("CLIENT shutting down");
    }// run

    // Do not modify this method
    public static void main(String[] args)
    {
        try
        {
            new Client(args.length != 1 ? null : args[0],
                       A5.CLIENT_RCV_PORT_NUM,
                       A5.CLIENT_PEER_RCV_PORT_NUM).run();
        }
        catch (Exception e)
        {
            A5.print("","CLIENT closing with error: " + e);
        }
    }// main

    /******************************************************************
     *   private methods
     ******************************************************************/

    // do not modify this method
    private void displayImage(byte[] fileData)
    {
        try
        {
            Image image = null;
            ByteArrayInputStream bin = new ByteArrayInputStream(fileData);
            image = ImageIO.read(bin);
            image = image.getScaledInstance(500, -1, Image.SCALE_DEFAULT);
            
            frame = new JFrame();
            JLabel label = new JLabel(new ImageIcon(image));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
        catch (Exception e)
        {
            System.err.println("CLIENT display image error: " + e);
        }
    }// displayImage

    /**
     * Sends the MSG_REQUEST_IMG_FILE message
     */
    private void sendFileRequest()
    {
        try {
            // Create a message with the request type
            byte[] message = { A5.MSG_REQUEST_IMG_FILE };
    
            // Send the message to the server using RDT
            rdt.sendData(message);
    
            // Print confirmation message
            System.out.println("CLIENT: Sent file request to server.");
    
            // Logic for additional message
            String additionalMessage = "this string is a request";
            rdt.sendData(additionalMessage.getBytes());
            System.out.println("CLIENT: Sent additional message to server.");
        } catch (Exception e) {
            // Handle exceptions
            System.out.println("CLIENT: Error sending file request to server: " + e.getMessage());
        }
    }// sendFileRequest

    /**
     * Receives one of two messages from the server and returns true if and
     * only if the message received is MSG_FILE_NAME, in which case it updates
     * the appropriate instance variable.
     */    
    private boolean getFileName()
    {
        try {
            while (true) {
                // Receive message from server
                byte[] receivedMessage = rdt.receiveData();
                
                if (receivedMessage != null && receivedMessage[0] == A5.MSG_FILE_NAME) {
                    // Extract file name from message
                    fileName = new String(Arrays.copyOfRange(receivedMessage, 1, receivedMessage.length));
                    System.out.println("CLIENT: Received file name from server: " + fileName);
                    return true;
                } else {
                    // Message received is not MSG_FILE_NAME, continue waiting
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            // Handle exceptions
            System.out.println("CLIENT: Error receiving file name from server: " + e.getMessage());
            return false;
        }
        // To be completed
        
     // only to satisfy the compiler
    }// getFileName

}// Client
