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
        System.out.println("CLIENT started");
        this.rdt = new RDT(ipAddress, rcvPortNum, peerRcvPortNum, "CLIENT");
        Thread.sleep(100);
        System.out.println("CLIENT started");
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
        System.out.println("CLIENT started");
        sendFileRequest();
        out = new ByteArrayOutputStream();
        if (getFileName()) {
            while (true) {
                System.out.println("loop");
                byte[] chunk = rdt.receiveData();
                if(new String(chunk,0,chunk.length).contains("done")){
                    System.out.println("done");
                    break;
                }

                //out.write(trimmedBytes);
                System.out.println("size: "+chunk.length);
                out.write(chunk);

            }
            System.out.println("outsize: "+out.toByteArray().length);
            System.out.println("CLIENT received image file");
            displayImage(out.toByteArray());
            Thread.sleep(2000); 
            if (frame != null) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        }
        System.out.println("CLIENT shutting down");
    }// run

    // Do not modify this method
    public static void main(String[] args)
    {
        try
        {
            System.out.println("CLIENT started");
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
        //System.out.println("Client sent file request");
        byte[] requestMessage = {(byte)A5.MSG_REQUEST_IMG_FILE};
        // for (int i = 0; i < requestMessage.length; i++) {
        //     System.out.println(requestMessage[i]);
        // }
        //rdt.sendData(requestMessage);
        rdt.sendData("this string is a request".getBytes());
        System.out.println("CLIENT Sent file request.");
    }// sendFileRequest

    /**
     * Receives one of two messages from the server and returns true if and
     * only if the message received is MSG_FILE_NAME, in which case it updates
     * the appropriate instance variable.
     */    
    private boolean getFileName()
    {
        //System.out.println("getfileName");
        byte[] message = rdt.receiveData();
        System.out.println("CLIENT got file name: " +new String(message, 0, message.length));
        return true;
        // for (int i = 0; i < message.length; i++) {
        //     System.out.println("client.gotFileName"+i+": "+message[i]);
        // }
        // if (message != null && message[0] == A5.MSG_FILE_NAME){
        //     fileName = new String(message,message.length, message.length - 1);
        //     System.out.println("CLIENT got file name: " + fileName);
        //     return true;
        // }
        // System.out.println("CLIENT Failed to receive valid file name.");
        // return false;
    }// getFileName
}// Client
