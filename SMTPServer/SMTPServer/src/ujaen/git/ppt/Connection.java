package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

//algo
public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;
	public static final int S_EHLO = 1;
	public static final int S_MAIL = 2;
	public static final int S_RCPT = 3;
	public static final int S_DATA = 4;
	public static final int S_RSET = 5;
	public static final int S_QUIT = 6;

	protected Socket mSocket;
	protected int mEstado = S_HELO;;
	private boolean mFin = false;
	int orden = -1;

	public Connection(Socket s) {
		mSocket = s;
		mEstado = S_HELO;
		mFin = false;
	}

	@Override
	public void run() {

		String inputData = null;
		String outputData = "";

		if (mSocket != null) {
			try {
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Envío del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP
						+ RFC5321.MSG_WELCOME + RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null)){

					System.out.println("Servidor [Recibido]> " + inputData);

					//Análisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					
					// TODO: Máquina de estados del protocolo
					if(Acceso(m)){
						switch (mEstado){
						case S_HELO:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ "Welcome " + Server.TCP_CLIENT_IP +
									", pleased to meet you"+ CRLF;
							break;
						case S_MAIL:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ "Sender" + SP + "`" + m.getArguments() + "`" 
									+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							break;
						case S_RCPT:
						
							break;
						case S_DATA:
							break;
						case S_RSET:
							break;
						case S_QUIT:
							outputData = RFC5321.getReply(RFC5321.R_221) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_221);
							this.mFin = true;
							break;
						default:
							break;
						}
					}
					else{
						outputData = RFC5321.getError(RFC5321.E_500_SINTAXERROR) 
								+ CRLF;
					}

					// TODO montar la respuesta
					// El servidor responde con lo recibido
					output.write(outputData.getBytes());
					output.flush();

				}//Fin del while
				System.out.println("Servidor [Conexión finalizada]> "
						+ mSocket.getInetAddress().toString() + ":"
						+ mSocket.getPort());

				input.close();
				output.close();
				mSocket.close();
			} catch (SocketException se) {
				se.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	public Boolean Acceso(SMTPMessage m){
		Boolean acceso = false;
		if(mEstado == S_HELO && m.getCommandId() == 0 && this.orden == -1){
			acceso = true;
			this.orden = 0;
		}
		else if(mEstado == S_HELO && m.getCommandId() == 1 && this.orden == 0){
			this.mEstado = S_MAIL;
			acceso = true;
			this.orden = 1;
		}
		else if(mEstado == S_MAIL && m.getCommandId() == 2 && this.orden == 1){
			this.mEstado = S_RCPT;
			acceso = true;
			this.orden = 2;
		}
		else if(mEstado == S_RCPT && m.getCommandId() == 3 && this.orden == 2){
			this.mEstado = S_DATA;
			acceso = true;
			this.orden = 3;
		}
		else if(m.getCommandId() == 4 && this.orden > -1){
			this.mEstado = S_HELO;
			acceso = true;
			this.orden = 0;
		}
		else if(m.getCommandId() == 5){
			this.mEstado = S_QUIT;
			acceso = true;
			this.orden = -1;
		}
		return acceso;
	}
}
