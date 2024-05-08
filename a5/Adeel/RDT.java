/*****************************************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: RDT.java
 *
 * Classes: RDT
 *          Sender (inner class)
 *          Receiver (inner class)
 *
 * Adeel Sultan
 *
 * Mitchell Bricco
 *
 * Yeakpan Kopah
 
 *****************************************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
        dataToSend = data;
        dataWaitingToBeSent = true;
        
        
    }// sendData

    /** The application calls this method to receive a message from its peer.
     *  The RDT instance simply returns this data to the app once it appears 
     *  in its receive buffer.
     */
    public byte[] receiveData()
    {
        dataWasReceivedFromBelow = true;
        
        return dataReceived; // only here to satisfy the compiler
    }// receiveData

    /**
     * Computes and returns the checksum (i.e., XORed byte values) over the 
     * first n bytes of the given array
     */
    private byte checkSum(byte[] array, int n)
    {
        byte value = array[0];
        for (int i = 1; i < n; ++i)
        value = (byte) ((int)value ^ (int)array[i]);
        //xor bits in all bytes but last
        //return the result
        
        return value; // only here to satisfy the compiler
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
            System.out.println("RECEIVER started");
            try {
                rcvSocket = new MyDatagramSocket(rcvPortNum);
                
            while (true) {
                rcvSocket.receive(rcvPacket);
                System.out.println("RECEIVER got data from below");
                rcvData = rcvPacket.getData();
                //FSM RDT Receiver 2.2
                if (dataOK()) {
                    System.out.println("RECEIVER got good data: send new ACK");
                    dataReceived = rcvData;
                    dataWasReceivedFromBelow = true;
                    while (dataWasReceivedFromBelow)
                        Thread.yield();
                    sendAck(expectedSeqNum);
                    expectedSeqNum = (expectedSeqNum == 0) ? 1 : 0;
                }
                else {
                    sendAck(Byte.toUnsignedInt(rcvData[0]));
                }
            }
            }
            catch (IOException e) {
                System.err.println(e.getMessage()); //PRINT STACK TRACE FOR DEBUGGING
            }
            catch (Exception e) {
                System.err.println(e.getMessage()); //PRINT STACK TRACE FOR DEBUGGING
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
        private boolean dataOK()
        {
            if (Byte.toUnsignedInt(rcvData[0]) != expectedSeqNum){
                System.out.println("RECEIVER got BAD ACK: " + expectedSeqNum);
                return false;
            }
            if (Byte.toUnsignedInt(checkSum(rcvData, rcvData.length)) != 0){
                System.out.println("RECEIVER got bad data");
                return false;
            }

            return true;   
        }// dataOK

        /**
         * Sends an acknowledgment packet with the given number
         */
        private void sendAck(int number)
        {
            byte[] data = {(byte)number,checkSum(rcvData, rcvData.length - 1)};
            DatagramPacket ackPacket = new DatagramPacket(data, data.length);
            try {
                System.out.println("RECEIVER sent ACK: " + expectedSeqNum);
                rcvSocket.send(ackPacket);
            }
            catch (IOException e) {
                System.err.println(e.getMessage());
            }
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
            System.out.println("SENDER started");
            try {
                senderSocket = new MyDatagramSocket();
                while (true) {
                    //FSM RDT Sender 3.0
                    try {
                        if (dataWaitingToBeSent) {
                            byte[] data = new byte[dataToSend.length+2];
                            data[0] = (byte)curSeqNum;
                            for (int i = 1; i < data.length - 1; ++i){
                                data[i] = dataToSend[i-1];
                            }
                            data[data.length-1] = checkSum(dataToSend, dataToSend.length-1);
                            dataToSend = data;
                            System.out.println("SENDER got data from above");
                            sendPacket();
                        }
                        else {
                            senderSocket.receive(rcvPacket);
                            rcvData = rcvPacket.getData();
                            if (ackPacketOK()){
                                senderSocket.setSoTimeout(0);
                                curSeqNum = (curSeqNum == 0) ? 1 : 0;
                                dataWaitingToBeSent = !dataWaitingToBeSent;
                            }
                            else {
                                senderSocket.setSoTimeout(500); //FIGURE OUT WHEN I HAVE TO CALL SENDERSOCKET.RECEIVE()
                            }
                        }
                    }
                    catch (SocketTimeoutException e) {
                        dataWaitingToBeSent = true;
                    }

                }
            }
            catch (IOException e) {
                System.err.println(e.getMessage()); //PRINT STACK TRACE FOR DEBUGGING
            }
            catch (Exception e) {
                System.err.println(e.getMessage()); //PRINT STACK TRACE FOR DEBUGGING
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
            if (Byte.toUnsignedInt(checkSum(rcvData, rcvData.length)) != 0){
                System.out.println("SENDER got bad data");
                return false;
            }
            if ((int)rcvData[0] != curSeqNum){
                System.out.println("SENDER got BAD ACK: " +  curSeqNum);
                return false;
            }

            return true;
        }// ackPacketOK

        /**
         * Sends to the peer's receiver a packet containing the data in the 
         * send buffer
         */     
        private void sendPacket() throws Exception
        {
            senderSocket.send(new DatagramPacket(dataToSend, dataToSend.length));
            dataWaitingToBeSent = !dataWaitingToBeSent;
            senderSocket.setSoTimeout(500);
            System.out.println("SENDER sent packet");
        }// sendPacket
    }// Sender
}// RDT


