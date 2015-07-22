/*
 * Flip it Socket Control
 * SocketServer Thread
 * Fist create TCP Socket Server 
 * When Client is accepted by the server, it create thread and also open an UDP socket server for user's motion signal
 * Close when boolean running is false  
 * 
 * Author: Mao Chen-Ning 2015.6
 * */
package web;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket/chat")
public class WsSignalServlet {
	
	private static final String GUEST_PREFIX = "playroom";
    private static final AtomicInteger connectionIds = new AtomicInteger(1);
    private static final AtomicInteger sockPortId = new AtomicInteger(6001);//start from
    private static final Set<WsSignalServlet> connections = new CopyOnWriteArraySet<>();

    private Session session;
    private final String nickname;
    private int port;
    private SocketServer sock;
    private Thread SockThread;
    
    //constructor for a websocket requests
    public WsSignalServlet() {
    	nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    	this.port = sockPortId.incrementAndGet();
    }
    
    
    //onOpen even handler, create TCP socket server and send port to browser
    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        //String message = String.format("[Server]%s %s (port:%d)", nickname, "has created.",this.port);
        //broadcast(message);
        
        //initialize socket thread
        try {
        	this.sock = new SocketServer(1,this.port,session);
			this.SockThread = new Thread(this.sock);
			//run Thread
			SockThread.start();
			session.getBasicRemote().sendText(new String("port,"+this.port));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Failed to create Thread.");
		}   
    }
    
    @OnClose
    public void end() {
        connections.remove(this);
        //stop socket thread
        this.sock.stopThread();
        SockThread.interrupt();
        //String message = String.format("[Server]%s %s",nickname, "has disconnected.");
        //broadcast(message);
        
    }

    @OnMessage
    public void incoming(String message) throws IOException {
        // Never trust the client
    	if(message.matches("[0-9],dead")){
    		String[] strArray = message.split(",");
    		this.sock.sendClient(Integer.parseInt(strArray[0]), strArray[1]);
    		session.getBasicRemote().sendText(new String("[Server]"+message));
    	}else if(message.equals("go")){
    		this.sock.broadcast(message);
    	}
    	//String filteredMessage = String.format("%s: %s",nickname, message);
        //broadcast(filteredMessage);
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
       System.out.println("Chat Error: " + t.toString());
       try {
           this.session.close();
       } catch (IOException e1) {
           // Ignore
       }
    }

    private static void broadcast(String msg) {
        for (WsSignalServlet client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
            	System.out.println("Chat Error: Failed to send message to client"+ e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s",client.nickname, "has been disconnected.");
                broadcast(message);
            }
        }
    }
    
   
}