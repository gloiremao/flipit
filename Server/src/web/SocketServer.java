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
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.Session;

public class SocketServer implements Runnable{
	
	private int id; //useless
	private int port; //port for TCP server
	ServerSocket TCPServer;
	DatagramSocket datagramSocket;
	private Session playroom; //Associated websocket session 
	private boolean running = true;
	private ArrayList<ClientService> clients; //activated client array
	int maxCli; 
	Timer isClosedChecker;
	
	
	//Constructor 
	public SocketServer(int id,int port,Session s) throws IOException{
		this.id = id;
		this.port = port;
		this.playroom = s;
		this.TCPServer = new ServerSocket(port);// for TCP 
		this.datagramSocket = new DatagramSocket(port+1000);// for UDP
		
		//Clients handle
		clients = new ArrayList<ClientService>();
		maxCli = 0;
		
		//Periodically Checking session is alive or not
		/*isClosedChecker = new Timer();
		isClosedChecker.schedule(new TimerTask(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(!playroom.isOpen()){
					stopThread();
					cancel();
				}
			}
		}, 500000, 60000);*/
	}
	
	public int getPort(){
		return this.port;
	}
	
	// Send to specific user (TCP)
	public void sendClient(int clientId,String msg){
		this.clients.get(clientId - 1).send(msg);
	}
	
	// broadcast message to all the users (TCP)
	public void broadcast(String msg){
		for(ClientService c : clients){
			c.send(msg);
		}
	}
	
	public void stopThread(){
		this.running = false;
		try {
			TCPServer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(){
		System.out.println("Socket Server start ...");
		while(running){
			try {
				
				if(maxCli >= 4)continue; //max user = 4
				Socket con;
				synchronized (TCPServer){
					con = TCPServer.accept();
				}
				//Accept, Server Start here
				
				//handle new user
				maxCli++;
				//send Id to browser
				playroom.getBasicRemote().sendText(maxCli+",login");
				//to client 
				PrintWriter out = new PrintWriter(con.getOutputStream(),true);
				out.println(""+maxCli);
				out.close();
				
				//Create new thread for each client's connection
				ClientService cs = new ClientService(maxCli,playroom, con, datagramSocket);
				clients.add(cs);
				Thread cliThread = new Thread(cs);
				cliThread.start();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("[Debug]Exception");
			}
		}
		
		//close socket
		try {
			TCPServer.close();
			System.out.println("TCP server stop!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//Class for Client Thread (Thread for each cell phone user)
	class ClientService implements Runnable{
		
		public int clientId;
		private Session playRoomSession;
		private Socket sock;
		private DatagramSocket usock;
		
		public ClientService(int clientId,Session playRoomSession, Socket sock, DatagramSocket usock) throws SocketException {
			super();
			this.clientId = clientId;
			this.playRoomSession = playRoomSession;
			this.sock = sock;
			this.usock = usock;
		}
		
		//Sent TCP data
		public void send(String msg){
			try {
				PrintWriter dos = new PrintWriter(this.sock.getOutputStream());
				dos.println(new String(msg));
				dos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error");
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			byte[] receiveBuffer = new byte[48];
			
			try {
				DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				do {
					this.usock.receive(receivePacket);
					//String signal = "["+receivePacket.getAddress()+"]"+ new String(receivePacket.getData());
					playRoomSession.getBasicRemote().sendText(new String(receivePacket.getData()));
				} while (running);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//close socket 
			try {
				send("[Server]End");
				this.usock.close();
				this.sock.close();
				System.out.println("[Debug]Cleint Service stop!");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
