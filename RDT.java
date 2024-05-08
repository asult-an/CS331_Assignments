/*****************************************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: RDT.java
 *
 * Classes: RDT
 *          Sender (inner class)
 *          Receiver (inner class)
 *
 * FIRST STUDENT'S FULL NAME GOES HERE
 *
 * 2nd STUDENT'S FULL NAME GOES HERE
 *
 * 3rd STUDENT'S FULL NAME GOES HERE (DELETE THIS LINE IF NOT NEEDED)
 
 *****************************************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

/**  
 * Reliable Data Transfer potocol implemented on top of MyDatagramSocket.
 *
 * Important note: this class is NOT aware of (and thus cannot reference) the
 * client/server processes that will be using it. In fact, any client-server
 * application should be able to use this class.
 *
 * The handout explains how an RDT instance's sender thread talks to its
 * peer's receiver thread, and vice-versa.
 */

public class RDT
{
    private InetAddress peerIpAddress;   // IP address of this instance
    private int rcvPortNum;              // port # of this instance's receiver
    private int peerRcvPortNum;          // port # of peer's receiver
    private Thread senderThread;         // sender side of this instance
    private Thread receiverThread;       // receiver side of this instance
    private byte[] dataToSend;           // buffer for data to be sent
                                         // AKA the "send buffer"
    private boolean dataWaitingToBeSent; // flag indicating that there is data
         // yet to be sent in the send buffer and that the app will have to
         // wait before being able to send another message
    private byte[] dataReceived;         // buffer for data received from below
                                         // AKA the "receive buffer"    
    private boolean dataWasReceivedFromBelow; // flag indicating that there is
         // data in the receive buffer that has yet to be grabbed by the app
    private String tag;                  // only for debugging (see handout)
    
    // Do not modify this constructor
    public RDT(String inPeerIP, 
               int inRcvPortNum, 
               int inPeerRcvPortNum,
               String inTag) throws Exception
    {
        rcvPortNum = inRcvPortNum;
        peerRcvPortNum = inPeerRcvPortNum;
        tag = inTag;
        if (inPeerIP == null)
            peerIpAddress = InetAddress.getLoopbackAddress();
        else
            peerIpAddress = InetAddress.getByName(inPeerIP);
        dataWaitingToBeSent = false;
        dataWasReceivedFromBelow = false;
        senderThread = new Thread(new Sender());
        receiverThread = new Thread(new Receiver());
        senderThread.start();
        receiverThread.start();
    }// constructor
      
    /** The application calls this method to send a message to its peer.
     *  The RDT instance simply "copies" this data into its send buffer, but
     *  only after waiting for the data currently in that buffer (if any)
     *  has been sent.
     */
    public void sendData(byte[] data)
    {
         // Wait until any previous data has been sent
    while (dataWaitingToBeSent) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Once previous data has been sent, proceed to send new data
    dataToSend = data.clone();
    dataWaitingToBeSent = true;
    }// sendData

    /** The application calls this method to receive a message from its peer.
     *  The RDT instance simply returns this data to the app once it appears 
     *  in its receive buffer.
     */
    public byte[] receiveData()
    {
       // Wait until data is available in the receive buffer
    while (!dataWasReceivedFromBelow) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Once data is received, retrieve it from the buffer
    byte[] receivedData = dataReceived.clone();
    dataWasReceivedFromBelow = false;
    return receivedData;
        
    // only here to satisfy the compiler
    }// receiveData

    /**
     * Computes and returns the checksum (i.e., XORed byte values) over the 
     * first n bytes of the given array
     */
    private byte checkSum(byte[] array, int n)
    {
        byte values = 0;
        for (int i = 0; i < n; i++) {
            values ^= array[i];
        }
        return values;
        // To be completed
        
// only here to satisfy the compiler
    }// checkSum

    /***********************************************************************
     * inner class: Receiver
     ***********************************************************************/
    
    private class Receiver implements Runnable
    {
        private MyDatagramSocket rcvSocket;   // socket of the receiver
        private DatagramPacket rcvPacket;     // received packet
        private byte[] rcvData =              // data in the received packet
                new byte[A5.MAX_MSG_SIZE + 2];
        private int expectedSeqNum = 0;       // enough said!

        /**
         * Implements the receiver's FSM for RDT 2.2. More precisely:
         * 1) create the receiver's socket with the appropriate port number
         * 2) in an infinite loop: receive data from below and process it 
         *    adequately according to the FSM, that is:
         *    + if bad data is received, resend the "other" ACK
         *    + if good data is received, place it in the receive buffer and
         *      wait for the app layer to grab it, then send the corresponding
         *      ACK.
         *    Make sure to keep the state of the receiver up to date at all 
         *    times.
         */
        @Override
        public void run()
        {
             // Create the receiver's socket with the appropriate port number
            // Create the receiver's socket with the appropriate port number
    try {
        rcvSocket = new MyDatagramSocket(rcvPortNum);
    } catch (SocketException e) {
        e.printStackTrace();
        return;
    }

    // Continuously receive data and process it
    while (true) {
        try {
            rcvPacket = new DatagramPacket(rcvData, rcvData.length);
            rcvSocket.receive(rcvPacket);
            System.out.println("RECIEVER got data from below");

            // Place data in the receive buffer
            dataReceived = rcvPacket.getData();
            dataWasReceivedFromBelow = true;

            // Update the expected sequence number
            expectedSeqNum = expectedSeqNum == 0 ? 1 : 0;

            // Send ACK for the received packet
            sendAck(expectedSeqNum);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
            // To be completed
        }// run

        /**
         * Returns true except when:
         * + the checksum of the received packet is not zero
         *   or
         * + the received sequence number does not match the expected one
         * As always, the format of the messages sent to the console is given
         * in the handout.
         */
        private boolean dataOK()
        {
             // Calculate checksum for received data (excluding sequence number)
    byte checksum = checkSum(rcvPacket.getData(), rcvPacket.getLength() - 1);

    // Check if checksum is zero and sequence number matches the expected value
    return checksum == 0 && rcvPacket.getData()[rcvPacket.getLength() - 1] == expectedSeqNum;
            // To be completed

         // only here to satisfy the compiler       
        }// dataOK

        /**
         * Sends an acknowledgment packet with the given number
         */
        private void sendAck(int number)
        {
            try {
                byte[] ackData = new byte[1];
                ackData[0] = (byte) number;
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, peerIpAddress, peerRcvPortNum);
                rcvSocket.send(ackPacket);
                expectedSeqNum = (number + 1) % 2;
            } catch (IOException e) {
                e.printStackTrace();
            }
            // To be completed
        }// sendAck
    }// Receiver

    /***********************************************************************
     * inner class: Sender
     ***********************************************************************/

    private class Sender implements Runnable
    {
        private MyDatagramSocket senderSocket;  // socket of the sender
        private DatagramPacket rcvPacket;       // received packet
        private byte[] rcvData = new byte[4];   // data in the received packet
        private int curSeqNum = 0;              // enough said!

        /**
         * Implements the sender's FSM for RDT 3.0. More precisely:
         * 1) create the sender's socket with an OS-generated port number
         * 2) in an infinite loop: send messages from the app to the receiver,
         *    that is:
         *    + wait for data from above, then send the packet to the peer
         *    + start the socket's timer using the call: setSoTimeout(500)
         *      and wait for the ACK to come in
         *      - if the ACK comes in okay and in good time, go back to the top
         *        of the loop
         *      - if the ACK is not okay, keep waiting for the next ACK
         *      - if the timer goes off, resend the message and keep waiting
         *        for the ACK
         *    Make sure to keep the state of the sender up to date at all 
         *    times.
         */
        @Override
        public void run()
        {
            
              // Create the sender's socket with an OS-generated port number
    try {
        senderSocket = new MyDatagramSocket();
    } catch (SocketException e) {
        e.printStackTrace();
        return;
    }

    // Continuously send messages
    while (true) {
        try {
            // Wait for data from above
            while (!dataWaitingToBeSent) {
                Thread.sleep(100);
            }

            // Send the packet to the peer
            sendPacket();

            // Start the socket's timer and wait for the ACK
            senderSocket.setSoTimeout(500);
            byte[] ackData = new byte[1];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            senderSocket.receive(ackPacket);

            // Check if ACK packet is valid
            if (ackPacketOK()) {
                // Data sent successfully, reset the flag
                dataWaitingToBeSent = false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        }// run

        /**
         * Returns true except when:
         * + the checksum of the received packet is not zero
         *   or
         * + the received sequence number does not match the expected one
         * As always, the format of the messages sent to the console is given
         * in the handout.
         */
        private boolean ackPacketOK()
        {
             // Check if the received sequence number matches the expected one
    boolean seqNumMatch = rcvPacket.getData()[0] == (byte) curSeqNum;

    // Add more conditions as needed based on your requirements
    return seqNumMatch;
            // To be completed

             // only here to satisfy the compiler
        }// ackPacketOK

        /**
         * Sends to the peer's receiver a packet containing the data in the 
         * send buffer
         */     
        private void sendPacket() throws Exception
        {
          // Create packet data with sequence number and checksum
    byte[] packetData = new byte[dataToSend.length + 2];
    System.arraycopy(dataToSend, 0, packetData, 0, dataToSend.length);
    packetData[dataToSend.length] = checkSum(dataToSend, dataToSend.length);
    packetData[dataToSend.length + 1] = (byte) curSeqNum;

    // Create DatagramPacket and send it
    DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, peerIpAddress, peerRcvPortNum);
    senderSocket.send(sendPacket);
    
    // Reset the flag since data is sent
    dataWaitingToBeSent = false;
            // To be completed
        }// sendPacket
    }// Sender
}// RDT


