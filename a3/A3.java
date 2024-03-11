/** Web server program
 *
 *  @author Mitchell B
 *
 *  @version CS 391 - Fall 2024 - A3
 **/

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;

public class A3
{
    static ServerSocket serverSocket = null;  // listening socket
    static int portNumber = 5555;             // port on which server listens
    static Socket clientSocket = null;        // socket to a client
    
    /* Start the server then repeatedly wait for a connection request, accept,
       and start a new thread to service the request
     */
    public static void main(String args[])
    {
        String percents = "%%";
        String waiting = "Waiting for a client...";
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println(percents + "Server started: " + serverSocket);
            System.out.println(percents + waiting);
            
        } catch (IOException e) {
            e.printStackTrace();

        }
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (5 * 60 * 1000); 
        while (System.currentTimeMillis() < endTime) {
            try {
               clientSocket = serverSocket.accept();
               System.out.println(percents + "New connection established: " + clientSocket);
               
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            // new thread for a client
            new Thread(new WebServer(clientSocket)).start();
            
        }   
        try {
         serverSocket.close();
         } catch (IOException e) {
            System.out.println("I/O error: " + e);
         }

    }// main method
}// A3 class

class WebServer implements Runnable
{
    static int numConnections = 0;           // number of ongoing connections
    Socket clientSocket = null;              // socket to client    
    BufferedReader in = null;                // input stream from client
    DataOutputStream out = null;             // output stream to client
    //added by me
    String percents = "%%";
    String waiting = "Waiting for a client...";
    String stars = "***";
    String spaces = "     ";

    /* Store a reference to the client socket, update and display the
       number of connected clients, and open I/O streams
    **/
    WebServer(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
        numConnections++;
        System.out.println(percents + "[# of connected clients: " + numConnections +"]");
        System.out.println(percents + waiting);
        
        try{
            openStreams(clientSocket);
        } catch (IOException e) {
            System.out.println("Server encountered an I/O error.");
          }
    }// constructor

    /* Each WebServer thread processes one HTTP GET request and
       then closes the connection
    **/
    public void run()
    {
        processRequest();
        System.out.println(percents + "Connection released: " + clientSocket);
        numConnections--;
        System.out.println(percents + "[# of connected clients: " + numConnections +"]");
        System.out.println(percents + waiting);
        close();
    }// run method

    /* Parse the request then send the appropriate HTTP response
       making sure to handle all of the use cases listed in the A3
       handout, namely codes 200, 404, 418, 405, and 503 responses.
    **/
    void processRequest()
    {
        try {
            String[] requestParts = parseRequest();
            if(requestParts == null){
                return;
            }
            String method = requestParts[0];
            String protocol = requestParts[1];
            String pathToFile = requestParts[2].substring(1);
            File file = new File(pathToFile);
            System.out.println(stars+" response "+stars);
            if (file.exists() && file.isFile() && method.equals("GET")) {
                byte[] fileContents = loadFile(file);
                write200Response(protocol, fileContents, pathToFile);
            } else if(!method.equals("GET")){
                writeCannedResponse(protocol, 405, "Method not allowed");
            } else if(pathToFile.equals("coffee")){
                writeCannedResponse(protocol, 418, "I'm a teapot");
            } else if(pathToFile.equals("tea/coffee")){
                writeCannedResponse(protocol, 503, "Coffee is temporarily unavailable");
            }
            else {
                write404Response(protocol, pathToFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }// processRequest method

    /* Read the HTTP request from the input stream line by line up to
       and including the empty line between the header and the
       body. Send to the console every line read (except the last,
       empty line). Then extract from the first line the HTTP command,
       the path to the requested file, and the protocol description string and
       return these three strings in an array.
    **/
    String[] parseRequest() throws IOException
    {
        ArrayList<String> requestLines = new ArrayList<>();
        String line;
        boolean isFirst = true;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if(isFirst){
                isFirst = false;
                System.out.println("\n"+stars+" request "+stars);
            }
            System.out.println(spaces + line);
            requestLines.add(line);
        }
        if(requestLines.isEmpty()){
            return null;
        }
        String[] firstLineParts = requestLines.get(0).split("\\s+");
        String method = firstLineParts[0];
        String pathToFile = firstLineParts[1];
        String protocol = firstLineParts[2];
        return new String[]{method, protocol, pathToFile};  
    }// parseRequest method

    /* Given a File object for a file that we know is stored on the
       server, return the contents of the file as a byte array
    **/
    byte[] loadFile(File file)
    {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fileInputStream.read(fileBytes);
            fileInputStream.close();
            return fileBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }// loadFile method

    /* Given an HTTP protocol description string, a byte array, and a file
       name, send back to the client a 200 HTTP response whose body is the
       input byte array. The file name is used to determine the type of
       Web resource that is being returned. The set of required header
       fields and file types is spelled out in the A3 handout.
    **/
    void write200Response(String protocol, byte[] body, String pathToFile)
    {
        try {
            String status = protocol+" 200 OK\r\n";
            String type = "Content-Length: " + body.length + "\r\n";
            System.out.println(spaces+status.substring(0,status.length()-4));
            System.out.println(spaces+type);
            String s = new String(body);
            System.out.println(spaces + s);
            System.out.println("");
            out.writeBytes(status);
            out.writeBytes(type);
            out.writeBytes("\r\n");
            out.write(body);
            out.writeBytes("\r\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }// write200Response method

    /* Given an HTTP protocol description string and a path that does not refer
       to any of the existing files on the server, return to the client a 404 
       HTTP response whose body is a dynamically created page whose content
       is spelled out in the A3 handout. The only HTTP header to be included
       in the response is "Content-Type".
    **/
    void write404Response(String protocol, String pathToFile)
    {
        try {
            String status = "HTTP/1.1 404 Not Found\r\n";
            String type = "Content-Type: text/html\r\n";
            String responseBody = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>"+
            "Page not found</title></head><body><h1>HTTP Error 404 Not Found</h1>"+
            "<h2>The file<span style=\"color: red\">"+pathToFile+"</span>"+
            "does not exist on this server.</h2></html></body>\n";
            out.writeBytes(status);
            System.out.println(spaces + status.substring(0,status.length()-4));
            out.writeBytes(type);
            System.out.println(spaces + type);
            out.writeBytes("\r\n");
            out.writeBytes(spaces + responseBody);
            System.out.println(spaces + responseBody);
            out.writeBytes("\r\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// write404Response method

    /* Given an HTTP protocol description string, a byte array, and a file
       name, send back to the client a 200 HTTP response whose body is the
       input byte array. The file name is used to determine the type of
       Web resource that is being returned. The only HTTP header to be included
       in the response is "Content-Type".
    **/
    void writeCannedResponse(String protocol, int code, String description)
    {
        try {
            String status = protocol + code + " " + description + "\r\n";
            String type = "Content-Type: text/html\r\n";
            byte[] fileContents = loadFile(new File("html/"+code+".html"));
            System.out.println(spaces + status.substring(0,status.length()-2));
            System.out.println(spaces + type);
            System.out.println(spaces + fileContents+"\n");
            out.writeBytes(status);
            out.writeBytes(type);
            out.writeBytes("\r\n");
            out.write(fileContents);
            out.writeBytes("\r\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }// writeCannedResponse method

    /* open the necessary I/O streams and initialize the in and out
       variables; this method does not catch any IO exceptions.
    **/    
    void openStreams(Socket clientSocket) throws IOException
    {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new DataOutputStream(clientSocket.getOutputStream());

    }// openStreams method

    /* close all open I/O streams and sockets; also update and display the
       number of connected clients.
    **/
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

}// WebServer class
