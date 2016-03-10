import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;

/**

This currently sends all traffic directly to the destination
Tor_port is provided, but is never used
We must packaged all http requests into TOR packages, and send to Tor_port

**/

//TODO 
/**
 * 
 * 	Current problem is: 1 Tor_Socket
	Everyone is writing to Tor_Socket, and reading from Tor_Socket
	ProxyThread should only get PackOutputStream, and Buffer In from client
	Reads from clients, and writes to proxy thread
	Proxy server will do all reading from tor and writing to clients
	
	What we need is a table of ‘client’ ports
	ONLY 1 THREAD READS FROM TOR_SOCKET
	It determines which ‘client’ port gets the data
 *
 */


public class Tor61ProxyThread extends Thread {
	private Socket TOR_SOCKET;
    private Socket socket = null;
    private static final int BUFFER_SIZE = 32768;
    
    // Set socket and tor_port number
    public Tor61ProxyThread(Socket socket, int proxy_port, Socket tor_socket) {
        this.socket = socket;
        this.TOR_SOCKET = tor_socket;
    }

    public void run() {
        //get input from user
        //send request to server
        //get response from server
        //send response to user

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Set Timer To 10 Minutes
            // If header is not processed within 10 minutes, assume client is dead
         	socket.setSoTimeout(10 * 1000);
            
            ArrayList<String> header = processRequestHeader(in);
            
            if (header.isEmpty()) {
            	socket.close();
            }
            
            // Kill Timer
         	socket.setSoTimeout(0);
         			
            String get = header.get(0).trim();            
            String request = get.split("\\s+")[0];
                        
            System.out.println(get);

			// Send Server Request
			Socket server = null;
            try {
				if (request.toLowerCase().equals("connect")) {
					try {
						server = TOR_SOCKET;
						out.write("HTTP/1.0 200 OK\r\n\r\n".getBytes());
					} catch (ConnectException e){
						out.write("HTTP/1.0 502 Bad Gateway\r\n\r\n".getBytes());
					}
					out.flush();
					
					stream(in, out, server);
				} else {
					server = TOR_SOCKET;
					PrintWriter serverRequest = new PrintWriter(server.getOutputStream());

					for (String s: header) {
						serverRequest.print(s + "\r\n");
					}
					serverRequest.print("\r\n");
					serverRequest.flush();

					InputStream is = server.getInputStream();
					
					// Set timer to 10 minutes
					// If no input is read in 10 minutes, assume connection is dead
					server.setSoTimeout(10 * 1000);
					
					// Send Server Response to Client
					byte b[] = new byte[BUFFER_SIZE];
					int read = is.read(b, 0, BUFFER_SIZE);
					while (read != -1) {
					  // If no input is read in 10 minutes, assume connection is dead
					  server.setSoTimeout(10 * 1000);
					  
					  out.write(b, 0, read);
					  read = is.read(b, 0, BUFFER_SIZE);
					  out.flush();
					}
					
					// Kill timer
					server.setSoTimeout(0);
				}          
            } catch (Exception e) {
				// Satisfy Client Request by terminating
                out.writeBytes("");
            }
                        
            if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
			if (server != null) {
				server.close();
			}
			if (socket != null) {
				socket.close();
			}

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    // Stream client and server input and output
    private void stream(BufferedReader cin, DataOutputStream cout, Socket server) {
    	try {
    		DataOutputStream sout = new DataOutputStream(server.getOutputStream());
        	BufferedReader sin = new BufferedReader(new InputStreamReader(server.getInputStream()));
        	
        	Boolean done = false;
        	
        	Thread cs = new StreamThread(cin, sout, done);
        	Thread sc = new StreamThread(sin, cout, done);
        	
        	cs.run();
        	sc.run();
        	
        	cs.join();
        	sc.join();
        	
        	if (sout != null)
        		sout.close();
        	if (sin != null)
        		sin.close();
        } catch (Exception e) {
        	
        }
    }
    
    private static ArrayList<String> processRequestHeader(BufferedReader br) throws IOException {
		ArrayList<String> header = new ArrayList<String>();
		String temp;

		while ((temp = br.readLine()) != null && !temp.equals("") && !temp.equals("\n")) {
			header.add(temp);
		}

		header = updateConnectionFieldandHttpVersion(header);
				
		return header;
	}

	private static ArrayList<String> updateConnectionFieldandHttpVersion(ArrayList<String> list) {
		for(int i = 0; i < list.size(); i++) {
			String s = list.get(i);
			String s1 = "Connection: ";
			String s2 = "Proxy-connection: ";
			String s3 = "close";
			if (s.length() >= s1.length() && s.substring(0, s1.length()).toLowerCase().equals(s1.toLowerCase())) {
				list.set(i,s1 + s3);
			} else if (s.length() >= s2.length() && s.substring(0, s2.length()).toLowerCase().equals(s2.toLowerCase())) {
				list.set(i,s2 + s3);
			} else if (s.contains("HTTP/1.1")) {
				list.set(i,s.replaceAll("HTTP/1.1","HTTP/1.0"));
			}
		}
		return list;
	}
	
	public class StreamThread extends Thread {
		private BufferedReader in;
		private DataOutputStream out;
		private Boolean done;
		
		public StreamThread(BufferedReader in, DataOutputStream out, Boolean done) {
			this.in = in;
			this.out = out;
			this.done = done;
		}
		
		public void run() {
			while (!done) {
				try {
					String next;
					if ((next = in.readLine()) != null)
						out.write(next.getBytes());
					else
						done = true;
					out.flush();
				} catch (IOException e) {
					done = true;
				}
			}
		}
	}
}