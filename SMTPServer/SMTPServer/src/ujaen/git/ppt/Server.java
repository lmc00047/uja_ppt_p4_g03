package ujaen.git.ppt;


import java.io.IOException;
import java.net.*;

public class Server {

	public static final int TCP_SERVICE_PORT = 5000;
	public static String TCP_CLIENT_IP = "";
	public static int TCP_CONNECTION_ID = 0;

	static ServerSocket server = null;

	public static void main(String[] args) {
			
		System.out.println("Servidor> Iniciando servidor");
		try {
			server = new ServerSocket(TCP_SERVICE_PORT);
			while (true) {
				final Socket newsocket = server.accept();
				System.out.println("Servidor> Conexión entrante desde "
						+ newsocket.getInetAddress().toString() + ":"
						+ newsocket.getPort());
				TCP_CLIENT_IP = Ip(newsocket.getInetAddress().toString());
				TCP_CONNECTION_ID++;
				new Thread(new Connection(newsocket)).start();
			}
		} catch (IOException e) {
			System.err.println("Server "+e.getMessage());
			e.printStackTrace();
		}

	}
	
	public static String Ip(String data){
		String ip = "";
		if(data.indexOf(":") > 0){
			String[] Parts = data.split(":");
			ip = Parts[0];
			ip = ip.substring(1);
		}
		else{
			ip = data.substring(1);
		}
		return ip;
	}
}