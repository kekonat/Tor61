import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Tor61ProxyServer {
	private int PROXY_PORT;
	private int TOR_PORT;
	private InetAddress TOR_ADDRESS;
	private boolean LISTENING;
	private ProxyServerThread SERVER;
	private Socket TOR_SOCKET;
	
	// Set proxy and tor ports
	public Tor61ProxyServer(int proxy_port, int tor_port, InetAddress address) {
		this.PROXY_PORT = proxy_port;
		this.TOR_PORT = tor_port;
		this.TOR_ADDRESS = address;
		if (PROXY_PORT < 1024 || PROXY_PORT > 49151)
			terminate();
		SERVER = null;
		TOR_SOCKET = createProtocol();
	}
	
	/**
	 * Create a tcp connection with the Tor Router and follow all Tor Message Protocols
	 * @return a tcp socket
	 */
	private Socket createProtocol() {
		Socket s = null;
		try {
			s = new Socket(TOR_ADDRESS, TOR_PORT);
		} catch (IOException e) {
			System.out.println("Failed Creating a Socket with Tor Router at ip: " + TOR_ADDRESS + " and port: " + TOR_PORT);
			System.exit(1);
		}
		
		// THINGS TO DO: 4
		/*
		 * Things to send on new tcp connection with router:
		 * 1. Open command
		 * 		Wait to receive opened reply
		 * 		Must handle Open Failed
		 * 2. Create command
		 * 		Must choose a Circuit ID
		 * 		Must wait to receive Created reply
		 * 		Must Handle if receive Create Fail
		 */
		return s;
	}

	// Start up proxy service on designated port
	// Return true if successfully started, and false otherwise
    public boolean start() {
		if (!LISTENING && SERVER == null) {
			LISTENING = true;
			SERVER = new ProxyServerThread();
			SERVER.start();
			return true;
		} else {
			System.out.println("PROXY SERVER IS ALREADY RUNNING ON PORT: " + PROXY_PORT);
			return false;
		}
    }

    // Returns true when Server successfully terminates and false otherwise
    // Ideally, the Server should terminate within the next 20 seconds
    public boolean quit() {
    	LISTENING = false;
    	System.out.println("Proxy Server is Terminating. Please note that this operation can take up to 20 seconds");
    	try {
    		SERVER.join();
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    		System.out.println("Interrupted when trying to quit in Proxy Server");
    		return false;
    	}
    	return true;
    }
    
    /**
     * Sends Tor Router Replay Extend Message to the address stored in Entry
     * @param e Entry object that stores the address of the next hop router
     * @return true if successful, false otherwise
     */
    public boolean extend(Entry e) {
    	// THINGS TO DO: 5
    	/*
    	 * Send relay extend message to Tor Router
    	 * Must wait for extended reply
    	 * Return true if successful, and false otherwise
    	 */
    	return false;
    }

	private void terminate() {
		System.err.println("Usage: <port number, ranges from 1024 to 49151>");
		System.err.println(PROXY_PORT + " is an invalid Port Number");
		System.exit(1);
	}
	
	public class ProxyServerThread extends Thread {
		public ProxyServerThread() {
		}
		
		public void run() {
			ServerSocket serverSocket = null;

			try {
				serverSocket = new ServerSocket(PROXY_PORT);
				System.out.println("Proxy listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + PROXY_PORT);
			} catch (IOException e) {
				System.err.println("Could not listen on port: " + PROXY_PORT);
				System.exit(1);
			}
		
			while (LISTENING) {
				// Set timeout to be 20 seconds
				try {
					serverSocket.setSoTimeout(20000);
					new Tor61ProxyThread(serverSocket.accept(), PROXY_PORT, TOR_SOCKET).start();
				} catch (SocketException e) {
					System.out.println("SocketException when trying to listen to Proxy Server");
					System.exit(1);
				} catch (IOException e) {
					continue;
				}
			}
			try {
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("IOException: Proxy No Longer Listening, but failed to close serverSocket");
				System.exit(1);
			}
		}
	}
}