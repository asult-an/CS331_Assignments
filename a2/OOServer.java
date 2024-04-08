/* Server program for the OnlineOrder app
   server only runs for 5 minutes
   @author Mitchell Bricco, Yeakpan Kopah, Adeel Sultan

   @version CS 391 - Spring 2024 - A2
*/

import java.net.*;
import java.io.*;
//import java.util.*;

public class OOServer
{
    static ServerSocket serverSocket = null;  // listening socket
    static int portNumber = 55555;            // port on which server listens
    static Socket clientSocket = null;        // socket to a client

    /* Start the server then repeatedly wait for a connection request, accept,
       and start a new thread to handle one online order
    */
    public static void main(String[] args)
    {
      ServerSocket serverSocket = null;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started: " + serverSocket);
            
        } catch (IOException e) {
            e.printStackTrace();

        }
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (5 * 60 * 1000); 
        while (System.currentTimeMillis() < endTime) {
            try {
               socket = serverSocket.accept();
               System.out.println("Connection established: " + serverSocket);
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            new Thread(new OO(socket)).start();
        }   
        try {
         serverSocket.close();
         } catch (IOException e) {
            System.out.println("I/O error: " + e);
         }
    }// main method

}// OOServer class

class OO implements Runnable
{
    static final int MAIN = 0;          // M state
    static final int PIZZA_SLICE = 1;   // PS state
    static final int HOT_SUB = 2;       // HS state
    static final int DISPLAY_ORDER = 3; // DO state
    static final Menu mm =              // Main Menu
        new Menu( new String[] { "Main Menu:", "Pizza Slices", "Hot Subs",
        "Display order" } );
    static final Menu psm =             // Pizza Slice menu
        new Menu( new String[] { "Choose a Pizza Slice:", "Cheese", "Pepperoni",
        "Sausage", "Back to Main Menu", "Display Order" } );
    static final Menu hsm =             // Hot Sub menu
        new Menu( new String[] { "Choose a Hot Sub:", "Italian", "Meatballs",
        "Back to Main Menu", "Display Order"  } );
    static final Menu dom =             // Display Order menu
        new Menu( new String[] { "What next?", "Proceed to check out",
        "Go Back to Main Menu"  } );
    int state;                          // current state
    Order order;                        // current order
    Socket clientSocket = null;         // socket to a client
    DataInputStream in = null;          // input stream from client
    DataOutputStream out = null;        // output stream to client

    /* Init client socket, current state, and order, and open the necessary
       streams
     */
    OO(Socket clientSocket)
    {
      this.clientSocket = clientSocket;
      state = MAIN;
      order = new Order();
      try{
         openStreams(clientSocket);
      }
      catch (IOException e) {
         System.out.println("Server encountered an I/O error. Shutting down.");
       }
        
    }// OO constuctor

    /* each execution of this thread corresponds to one online ordering session
     */
    public void run()
    {
      try{
         placeOrder();
      } catch (SecurityException e) {
         System.err.println("Connection blocked by system: " + e.getMessage());
         System.exit(1);
       } catch (UTFDataFormatException e) {
         System.err.println("Malformed data: " + e.getMessage());
         System.exit(1);
       } catch (IOException e) {
         System.out.println("Server encountered an I/O error.");
         System.out.println(e);
       }
      
      
        
    }// run method

    /* implement the OO protocol as described by the FSM in the handout
       Note that, before reading the first query (i.e., option), the server
       must display the welcome message shown in the trace in the handout,
       followed by the main menu.
     */
    void placeOrder() throws IOException
    {
      String welcomeMessage = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n"+
      "      Welcome to Hot Subs & Wedges!";
      String request = "",reply = welcomeMessage;
         out.writeUTF(reply);
         boolean isFirst = true;
         boolean isInvalid = false;
         while(!reply.equals("Thank you for your visit!")){        
            if(!isInvalid){
               reply = "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
            } else {
               reply = "";
               isInvalid = false;
            }
            switch (state) {
               case MAIN:
                  if(isFirst){
                     isFirst = false;
                     reply += "\n";
                  }
                  reply += mm;
                  out.writeUTF(reply);
                  request = in.readUTF();
                  switch (request) {
                     case "1":
                        state = PIZZA_SLICE;
                        break;
                     case "2":
                        state = HOT_SUB;
                        break;
                     case "3":
                        state = DISPLAY_ORDER;
                        break;
                     default:
                        isInvalid = true;
                        reply = "Invalid option!";
                        out.writeUTF(reply);
                        break;
                  }
                  break;
               case PIZZA_SLICE:
                  reply += psm;
                  out.writeUTF(reply);
                  request = in.readUTF();
                  switch (request) {
                     case "1":
                        order.addItem("Cheese");
                        break;
                     case "2":
                        order.addItem("Pepperoni");
                        break;
                     case "3":
                        order.addItem("Sausage");
                        break;
                     case "4":
                        state = MAIN;
                        break;
                     case "5":
                        state = DISPLAY_ORDER;
                        break;
                     default:
                        isInvalid = true;
                        reply = "Invalid option!";
                        out.writeUTF(reply);
                        break;
                  }
                  break;
               case HOT_SUB:
                  reply += hsm;
                  out.writeUTF(reply);
                  request = in.readUTF();
                  switch (request) {
                     case "1":
                        order.addItem("Italian");
                        break;
                     case "2":
                        order.addItem("Meatballs");
                        break;
                     case "3":
                        state = MAIN;
                        break;
                     case "4":
                        state = DISPLAY_ORDER;
                        break;
                     default:
                        isInvalid = true;
                        reply = "Invalid option!";
                        out.writeUTF(reply);
                        break;
                  }
                  break;
               case DISPLAY_ORDER:
                  reply += order.toString();
                  reply += "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
                  reply += dom;
                  out.writeUTF(reply);
                  request = in.readUTF();
                  switch (request) {
                     case "2":
                        state = MAIN;
                        break;
                     case "1":
                        reply = "Thank you for your visit!";
                        out.writeUTF(reply);
                        close();
                        System.out.println("One more order processed!");
                        break;
                     default:
                        isInvalid = true;
                        reply = "Invalid option!";
                        out.writeUTF(reply);
                        break;
                  }
                  break; 
            }
         }  
    }// placeOrder method

   /* open the necessary I/O streams and initialize the in and out
      static variables; this method does not catch any exceptions
    */
    void openStreams(Socket socket) throws IOException
    {
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
        
    }// openStreams method

    /* close all open I/O streams and sockets
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

}// OO class
