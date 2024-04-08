/** Peer.java - A peer-to-peer file sharing program
 *
 *  @version CS 391 - Spring 2024 - A4
 *
 *  @author Adeel Sultan
 *
 *  @author Mitchell Bricco
 *
 *  @author Yeakpan Kopah
 * 
 *  @bug The initial join of a new peer [does/does not] work fully. [pick one]
 *
 *  @bug The Status command [does/does not] work fully. [pick one]
 *
 *  @bug The Find command [does/does not] work fully. [pick one]
 *
 *  @bug The Get command [does/does not] work fully. [pick one]
 * 
 *  @bug The Quit command [does/does not] work fully. [pick one]
 * 
 **/

import java.io.*;
import java.net.*;
import java.util.*;

class Peer
{
    String name,    // symbolic name of the peer, e.g., "P1" or "P2"
        ip,         // IP address in dotted decimal notation, e.g., "127.0.0.1"
        filesPath;  // path to local file repository, e.g., "dir1/dir2/dir3"
    int lPort,      // lookup port number (permanent UDP port)
        ftPort;     // file transfer port number (permanent TCP port)
    List<Neighbor> neighbors;      // current neighbor peers of this peer
    LookupThread  lThread;         // thread listening via the lookup socket
    FileTransferThread  ftThread;  // thread listening via file transfer socket
    int seqNumber;                 // identifier for next Find request (used to
                                   // control the flooding by avoiding loops)
    Scanner scanner;               // used for keyboard input
    HashSet<String> findRequests;  // record of all lookup requests seen so far

    /* Instantiate a new peer (including setting a value of 1 for its initial
       sequence number), launch its lookup and file transfer threads,
       (and start the GUI thread, which is already implemented for you)
     */
    Peer(String name2, String ip, int lPort, String filesPath, 
         String nIP, int nPort)
    {
        name = name2;
        this.ip = ip;
        this.lPort = lPort;
        this.ftPort = lPort + 1;
        this.filesPath = filesPath;
        neighbors = new ArrayList<Neighbor>();
        scanner = new Scanner(System.in);
        seqNumber = 0;
        findRequests = new HashSet<String>();

        lThread = new LookupThread();
        ftThread = new FileTransferThread();
        GUI.createAndShowGUI("Peer " + name);

        

        if (nIP != null) {
            Neighbor neighbor = new Neighbor(nIP, nPort);
            neighbors.add(neighbor);
        }
        

    }// constructor

    /* display the commands available to the user
       Do NOT modify this method.
     */
    static void displayMenu()
    {
        System.out.println("\nYour options:");
        System.out.println("    1. [S]tatus");
        System.out.println("    2. [F]ind <filename>");
        System.out.println("    3. [G]et <filename> <peer IP> <peer port>");
        System.out.println("    4. [Q]uit");
        System.out.print("Your choice: ");
    }// displayMenu method

    /* input the next command chosen by the user
     */
    int getChoice()
    {
        String input = scanner.nextLine();
        switch (input) {
            case "S":
            case "s":
            case "1":
                return 1;

            case "F":
            case "f":
            case "2":
                return 2;

            case "G":
            case "g":
            case "3":
                return 3;

            case "Q":
            case "q":
            case "4":
                return 4;
        }

        return -1;
    }// getChoice method
        
    /* this is the implementation of the peer's main thread, which
       continuously displays the available commands, inputs the user's
       choice, and executes the selected command, until the latter
       is "Quit"
     */
    void run()
    {
        lThread.start();
        ftThread.start();
        int command = -1;
        while (command != 4) {
            displayMenu();
            command = getChoice();
            while (command == -1) {
                System.out.println("Invalid option!\nYour choice: ");
                command = getChoice();
            }
            switch (command) {
                case 1:
                    processStatusRequest();
                    break;
                
                case 2:
                    processFindRequest();
                    break;

                case 3:
                    processGetRequest();
                    break;

                case 4:
                    processQuitRequest(); //don't terminate main.run() from this method...
                    break;
            }
        }

    }// run method

    /* execute the Quit command, that is, send a "leave" message to all of the
       peer's neighbors, then terminate the lookup thread
     */
    void processQuitRequest()
    {
        byte[] buf;
        InetAddress address;
        DatagramPacket packet;

        try {
            buf = ("leave " + ip + " " + Integer.toString(lPort)).getBytes();
            for (Neighbor n : neighbors) {
                address = InetAddress.getByName(n.ip);
                packet = new DatagramPacket(buf, buf.length, address, n.port);
                lThread.socket.send(packet);
                GUI.displayLU("Sent: " + new String(packet.getData(), 0, packet.getLength()));
            }
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            System.out.println("Failed to leave network");
        }
        lThread.terminate();
        

    }// processQuitRequest method

    /* execute the Status command, that is, read and display the list
       of files currently stored in the local directory of shared
       files, then print the list of neighbors. The EXACT format of
       the output of this method (including indentation, line
       separators, etc.) is specified via examples in the assignment
       handout.
     */
    void processStatusRequest()
    {
        String border = "=======================";
        File directory = new File(filesPath);
        File[] fileList = directory.listFiles();
        
        System.out.println(border + "\nLocal files: ");
        for (int i = 0; i < fileList.length; ++i) {
            System.out.println(fileList[i].toString().substring(filesPath.length() + 1,
            fileList[i].toString().length()));
        }

        System.out.println("Neighbors: ");
        printNeighbors();
        System.out.println(border);

    }// processStatusRequest method

    /* execute the Find command, that is, prompt the user for the file
       name, then look it up in the local directory of shared
       files. If it is there, inform the user. Otherwise, send a
       lookup message to all of the peer's neighbors. The EXACT format
       of the output of this method (including the prompt and
       notification), as well as the format of the 'lookup' messages
       are specified via examples in the assignment handout. Do not forget
       to handle the Find-request ID properly.
     */
    void processFindRequest()
    {
        String border = "=======================";
        File directory = new File(filesPath);
        File[] fileList = directory.listFiles();
        boolean found = false;

        System.out.print(border + "\nName of file to find: ");
        String fileName = scanner.nextLine();
        for (int i = 0; i < fileList.length; ++i) {
            String tempName = fileList[i].toString().substring(filesPath.length() + 1,
            fileList[i].toString().length());
            if (tempName.equals(fileName)) {
                System.out.println("This file already exists locally in " + filesPath);
                found = true;
            }
        } 
        if (!found) {
            byte[] buf;
            InetAddress address;
            DatagramPacket packet;
            String reqID = name + "#" + seqNumber++;
            try {
                buf = ("lookup " + fileName + " " + reqID + " " + ip + " " +
                Integer.toString(lPort) + " " + ip + " " + Integer.toString(lPort)).getBytes();
                for (Neighbor n : neighbors) {
                    address = InetAddress.getByName(n.ip);
                    packet = new DatagramPacket(buf, buf.length, address, n.port);
                    lThread.socket.send(packet);
                    GUI.displayLU("Sent: " + new String(packet.getData(), 0,
                        packet.getLength()));
                }
            }
            catch (IOException e) {
                System.err.println(e.getMessage());
                System.out.println("Failed to leave network");
            }
        }
        System.out.println(border);

    }// processFindRequest method

    /* execute the Get command, that is, prompt the user for the file
       name and address and port number of the selected peer with the
       needed file. Send a "get" message to that peer and wait for its
       response. If the file is not available at that peer, inform the
       user. Otherwise, extract the file contents from the response,
       output the contents on the user's terminal and save this file
       (under its original name) in the local directory of shared
       files.  The EXACT format of this method's output (including the
       prompt and notification), as well as the format of the "get"
       messages are specified via examples in the assignment
       handout.
     */
    void processGetRequest()
    {
        String fileName, fileContents;
        String peerIP;
        int peerPort;
        String border = "=======================";
        String fileBorder = "- - - - - - - - - - - - -";
        String response = "";
        System.out.print(border + "\nName of file to get: ");
        fileName = scanner.nextLine();
        System.out.print("Address of the source peer: ");
        peerIP = scanner.nextLine();
        System.out.print("Port of source peer: ");
        peerPort = scanner.nextInt();

        try {
            
            ftThread.clientSocket = new Socket(peerIP, peerPort);
            ftThread.openStreams();
            ftThread.out.writeUTF("get <" + fileName + ">");
            response = ftThread.in.readUTF();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("Contents of the received file between dashed lines: ");
        System.out.println(fileBorder);
        System.out.println(response);
        System.out.println(fileBorder);
        
        fileContents = response.substring(response.indexOf("\n") + 2, response.length());
        writeFile(fileName, fileContents);

    }// processGetRequest method

    /* create a text file in the local directory of shared files whose
       name and contents are given as arguments.
     */
    void writeFile(String fileName, String contents)
    {
        try {
            File newFile = new File(filesPath + "\\" + fileName);
            newFile.createNewFile();
            FileWriter fileWriter = new FileWriter(newFile);
            //PrintWriter printer = new PrintWriter(fileWriter);
            //printer.print(contents);\
            fileWriter.write(contents);
            fileWriter.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
        

    }// writeFile method

    /* Send to the user's terminal the list of the peer's
       neighbors. The EXACT format of this method's output is specified by
       example in the assignment handout.
     */
    void printNeighbors()
    {   
        for (int i = 0; i < neighbors.size(); ++i) {
            System.out.println(neighbors.get(i).ip + ":" + neighbors.get(i).port);
        }

    }// printNeighbors method

    /* Do NOT modify this inner class
     */
    class Neighbor
    {
        String ip;
        int port;

        Neighbor(String ip, int port)
        {
            this.ip = ip;
            this.port = port;
        }// constructor

        public boolean equals(Object o)
        {
            if (o == this) { return true;  }
            if (!(o instanceof Neighbor)) { return false;  }        
            Neighbor n = (Neighbor) o;         
            return n.ip.equals(ip) && n.port == port;
        }// equals method

        public String toString()
        {
            return ip + ":" + port;
        }// toString method
    }// Neighbor class

    class LookupThread extends Thread
    {   
        DatagramSocket socket = null;           // UDP server socket
        private volatile boolean stop = false;  // flag used to stop the thread

        /* Stop the lookup thread by closing its server socket. This
           works (in a not-so-pretty way) because this thread's run method is
           constantly listening on that socket.
           Do NOT modify this method.
         */
        public void terminate()
        {
            stop = true;
            socket.close();
        }// terminate method

        /* This is the implementation of the thread that listens on
           the UDP lookup socket. First (at startup), if the peer has
           exactly one neighbor, send a "join" message to this
           neighbor. Otherwise, skip this step. Second, continuously
           wait for an incoming datagram (i.e., a request), display
           its contents in the GUI's Lookup panel, and process the
           request using the helper method below.
        */
        public void run()
        {            
            try {
                socket = new DatagramSocket(lPort);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
            }
            if (!neighbors.isEmpty()) {
                try {
                    byte[] buf = ("join " + ip + " " + Integer.toString(lPort)).getBytes();
                    InetAddress address = InetAddress.getByName(neighbors.get(0).ip);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, 
                    address, neighbors.get(0).port);
                    socket.send(packet);
                    GUI.displayLU("Sent: " + new String(packet.getData(), 0, packet.getLength()));
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.out.println("Failed to join network");
                }
            }

            while (!stop) {
                
                try {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    GUI.displayLU("Received: " + message);
                    process(message);
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.out.println("Failed to receive message");
                }
                
            }
            System.out.println("exited lookup.run");

        }// run method

        /* This helper method processes the given request, which was
           received by the Lookup socket. Based on the first field of
           the request (i.e., the "join", "leave", "lookup", or "file"
           keyword), perform the appropriate action. All actions are
           quite short, except for the "lookup" request, which has its
           own helper method below.
         */
        void process(String request)
        {
            String incomingIP;
            int lookupPort;
            Neighbor neighbor;
            String type = request.split(" ")[0];
            
            switch (type) {
                case "join":
                    incomingIP = request.split(" ")[1];
                    lookupPort = Integer.parseInt(request.split(" ")[2]);
                    neighbor = new Neighbor(incomingIP, lookupPort);
                    neighbors.add(neighbor);
                    break;

                case "lookup":
                    processLookup(new StringTokenizer(request));
                    break;

                case "file":

                    String ownerIP = request.split(" ")[4];
                    int ownerPort = Integer.parseInt(request.split(" ")[5]);
                    Neighbor owner = new Neighbor(ownerIP, ownerPort);
                    neighbors.add(owner);

                    try {
                        byte[] buf = ("join " + ip + " " + Integer.toString(lPort)).getBytes();
                        InetAddress address = 
                            InetAddress.getByName(neighbors.get(neighbors.size() - 1).ip);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, 
                        address, neighbors.get(neighbors.size() - 1).port);
                        socket.send(packet);
                        GUI.displayLU("Sent: " + new String(packet.getData(), 0, packet.getLength()));
                    }
                    catch (IOException e) {
                        System.err.println(e.getMessage());
                        System.out.println("Failed to join network");
                    }

                    break;

                case "leave":
                    ip = request.split(" ")[1];
                    lookupPort = Integer.parseInt(request.split(" ")[2]);
                    for (int i = 0; i < neighbors.size(); ++i) {
                        if (neighbors.get(i).ip.equals(ip)) {
                            neighbors.remove(neighbors.get(i));
                        }
                    }
                    break;
            }


        }// process method

        /* This helper method processes a "lookup" request received
           by the Lookup socket. This request is represented by the
           given tokenizer, which contains the whole request line,
           minus the "lookup" keyword (which was removed from the
           beginning of the request) by the caller of this method.
           Here is the algorithm to process such requests:
           If the peer already received this request in the past (see request
           ID), ignore the request. 
           Otherwise, check if the requested file is stored locally (in the 
           peer's directory of shared files):
             + If so, send a "file" message to the source peer of the request 
               and, if necessary, add this source peer to the list 
               of neighbors of this peer.
             + If not, send a "lookup" message to all neighbors of this peer,
               except the peer that sent this request (that is, the "from" peer
               as opposed to the "source" peer of the request).
         */
        void processLookup(StringTokenizer line)
        {
            line.nextToken();
            String fileName = line.nextToken();
            String incomingReqID = line.nextToken();
            String senderIP = line.nextToken();
            int senderPort = Integer.parseInt(line.nextToken());
            String initIP = line.nextToken();
            int initPort = Integer.parseInt(line.nextToken());
            if (findRequests.contains(incomingReqID)) {
                System.out.println("Duplicate reqID received");
                return;
            }
            findRequests.add(incomingReqID);
            
            File directory = new File(filesPath);
            File[] fileList = directory.listFiles();
            boolean found = false;

            for (int i = 0; i < fileList.length; ++i) {
                String tempName = fileList[i].toString().substring(filesPath.length() + 1,
                fileList[i].toString().length());

                if (tempName.equals(fileName)) { //file was found
                    found = true;
                    byte[] buf;
                    InetAddress address;
                    DatagramPacket packet;
                    try {
                        buf = ("file " + fileName + " is at " + ip + " " +
                            Integer.toString(lPort) + " (tcp port: <" + 
                            Integer.toString(ftPort) + ">) ").getBytes();
                        address = InetAddress.getByName(initIP);
                        packet = new DatagramPacket(buf, buf.length, address, initPort);
                        socket.send(packet);
                        GUI.displayLU("Sent: " + new String(packet.getData(), 0,
                            packet.getLength()));

                    }
                    catch (IOException e) {
                        System.err.println(e.getMessage());
                        System.out.println("Failed to connect to the network");
                    }
                    break;
                }
                    
            }

            if (!found) {
                byte[] buf;
                InetAddress address;
                DatagramPacket packet;
                String reqID = name + "#" + seqNumber++;
                try {
                    buf = ("lookup " + fileName + " " + reqID + " " + ip + " " + lPort + 
                    " " + initIP + " " + initPort).getBytes();
                    for (Neighbor n : neighbors) { System.out.println(n.ip + " " + n.port);}
                    for (Neighbor n : neighbors) {
                        address = InetAddress.getByName(n.ip);
                        packet = new DatagramPacket(buf, buf.length, address, n.port);
                        if (senderIP.equals(n.ip) && senderPort == n.port) {
                            continue;
                        }
                        socket.send(packet);
                        GUI.displayLU("Sent: " + new String(packet.getData(), 0,
                        packet.getLength()));
                    }
                    
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.out.println("Failed to leave network");
                }
            }
                    
                    

        }// processLookup method

    }// LookupThread class
    
    class FileTransferThread extends Thread
    {
        ServerSocket serverSocket = null;   // TCP listening socket
        Socket clientSocket = null;         // TCP socket to a client
        DataInputStream in = null;          // input stream from client
        DataOutputStream out = null;        // output stream to client
        String request, reply;

        /* this is the implementation of the peer's File Transfer
           thread, which first creates a listening socket (or welcome
           socket or server socket) and then continuously waits for
           connections. For each connection it accepts, the newly
           created client socket waits for a single request and processes
           it using the helper method below (and is finally closed).
        */      
        public void run()
        {
            try {
                serverSocket = new ServerSocket(ftPort);
                
                clientSocket = serverSocket.accept();
                openStreams();
                while (true) {
                    request = in.readUTF();
                    process(request);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }


        }// run method
        
        /* Process the given request received by the TCP client
           socket.  This request must be a "get" message (the only
           command that uses the TCP sockets). If the requested
           file is stored locally, read its contents (as a String)
           using the helper method below and send them to the other side
           in a "fileFound" message. Otherwise, send back a "fileNotFound"
           message.
         */
        void process(String request)
        {        
            String requestedFileName = request.split(" ")[1];
            requestedFileName = requestedFileName.substring(1, requestedFileName.length() - 1);
            File directory = new File(filesPath);
            File[] fileList = directory.listFiles();

            for (int i = 0; i < fileList.length; ++i) {
                String tempName = fileList[i].toString().substring(filesPath.length() + 1,
                fileList[i].toString().length());
                if (requestedFileName.equals(tempName)) {
                    String fileContents = "";
                    
                    try {
                        Scanner fileScanner = new Scanner(fileList[i]);
                        while (fileScanner.hasNextLine()) {
                        fileContents += fileScanner.nextLine();
                        }
                        fileScanner.close();
                    }
                    catch (FileNotFoundException e) {
                        System.err.println(e.getMessage());
                    }
                    try {
                        out.writeUTF(requestedFileName + ":\n " + fileContents);
                        GUI.displayFT("fileFound " + fileContents);
                    }
                    catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                    return;
                }
                
            }
            try {
                out.writeUTF("fileNotFound");
                GUI.displayFT("fileNotFound");
            }
            catch (IOException e) {
                System.err.println(e.getMessage());
            }
            return;

        }// process method

        /* Given a File object for a file that we know is stored at this
           peer, return the contents of the file as a byte array.
        */
        byte[] readFile(File file)
        {
            /* to be completed */

            return null;
        }// readFile method

        /* Open the necessary I/O streams and initialize the in and out
           variables; this method does not catch any exceptions.
        */
        void openStreams() throws IOException
        {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

        }// openStreams method

        /* close all open I/O streams and the client socket
         */
        void close()
        {
            try {
                if (in != null) {
                  in.close();
                }
                if (out != null) {
                  out.close();
                }
                if (clientSocket != null) {
                  clientSocket.close();
                }
              } catch (IOException e) {
                System.err.println("Error in closing streams: " + e.getMessage());
              }

        }// close method
        
    }// FileTransferThread class

}// Peer class
