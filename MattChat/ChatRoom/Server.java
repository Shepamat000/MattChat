package ChatRoom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
	
	private ArrayList<ConnectionHandler> connections;
	private ServerSocket server;
	private boolean done;
	private ExecutorService threadPool;
	
	public Server () {
		connections = new ArrayList<>();
		done = false;
	}

	@Override
	public void run() {
		try {
			server = new ServerSocket (9999);
			threadPool = Executors.newCachedThreadPool();
			System.out.println("Server Opened");
			while (!done) {
				Socket client = server.accept();
				ConnectionHandler handler = new ConnectionHandler(client);
				connections.add(handler);
				threadPool.execute(handler);
			}
		} catch (Exception e) {
			shutdown();
		}
	}
	
	// Broadcast a message to the whole server
	public void broadcast (String message) {
		for (ConnectionHandler ch : connections) {
			if (ch != null) {
				ch.sendMessage(message);
			}
		}
	}
	
	// Broadcast a message to the whole server
	public boolean privateMessage (String origin, String user, String message) {
		for (ConnectionHandler ch : connections) {
			if (user.equals(ch.nickname)) {
				ch.sendMessage("[" + origin + " whispers]: " + message);
				return true;
			}
		}
		return false;
	}
	
	// Find time for /time command
	public String fetchServerTime () {
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		return currentDate.toString();
	}
	
	// Shutdown the server
	public void shutdown () {
		try {
			System.out.println("Server shutting down");
			done = true;
			threadPool.shutdown();
			// Shutdown server
			if (!server.isClosed()) {
				server.close();
			} 
			// Shutdown connections
			for (ConnectionHandler ch : connections) {
				ch.shutdown();
			}
		} catch (IOException e) {
			System.out.println ("Server encountered error during shutdown");
		}
	}
	
	class ConnectionHandler implements Runnable {
		
		private Socket client;
		private BufferedReader in;
		private PrintWriter out;
		private String nickname;
		
		public ConnectionHandler (Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			try {
				out = new PrintWriter(client.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				
				out.println("Please enter a nickname: ");
				nickname = in.readLine();
				System.out.println(nickname + " connected");
				broadcast(nickname + " has joined MattChat!");
				
				String message;
				while ((message = in.readLine()) != null) {					
					if (message.startsWith("/nick ")) {
						// Nickname command
						String[] messageSplit = message.split(" ", 2);
						if (messageSplit.length == 2) {
							broadcast(nickname + " renamed themselves to " + messageSplit[1]);
							System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
							nickname = messageSplit[1];
							out.println("Nickname sucessfully changed to " + nickname);
						} else {
							out.println("Command failed: No nickname provided");
						}
						
					} else if (message.startsWith("/quit")) {
						// Quit command
						broadcast(nickname + " has left the chat!");
						shutdown();
					} else if (message.startsWith("/msg ")) {
						// Msg command
						String[] messageSplit = message.split(" ", 3);
						String msgName = messageSplit[1];
						String msgContent = messageSplit[2];
						if(!privateMessage(nickname, msgName, msgContent)) out.println("User " + messageSplit[1] + " not found");
					} else if (message.startsWith("/time")) {
						broadcast(nickname + ": /time\n" + fetchServerTime());
					}else {
						broadcast(nickname + ": " + message);
					}
				}
			} catch (Exception e) {
				shutdown();
			}
		}
		
		public void sendMessage (String message) {
			out.println(message);
		}
		
		public void shutdown () {
			try {
				in.close();
				out.close();
				if(!client.isClosed()) {
					client.close();
				}
			} catch (IOException e) {
				// Ignore
			}
		}
		
	}
	
	public static void main (String[] args) {
		Server server = new Server();
		server.run();
	}

}
