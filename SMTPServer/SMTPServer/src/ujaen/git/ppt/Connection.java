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
import ujaen.git.ppt.mail.*;

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
	Boolean mData;
	Mail mail = null;
	Mailbox mailbox = null;

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
				// Inicializaci�n de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Env�o del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP
						+ RFC5321.MSG_WELCOME + RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();

				while (!mFin && ((inputData = input.readLine()) != null)){

					System.out.println("Servidor [Recibido]> " + inputData);

					//An�lisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					
					if(Acceso(m)){
						switch (mEstado){
						case S_HELO:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ "Welcome " + Server.TCP_CLIENT_IP +
									", pleased to meet you"+ CRLF;
							this.orden = 0;
							break;
						case S_MAIL:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ "Sender" + SP + "`" + m.getParameters()[2] + "`" 
									+ SP + RFC5321.getReplyMsg(RFC5321.R_250)
									+ CRLF;
							this.orden = 1;
							break;
						case S_RCPT:
							if(mailbox.checkRecipient(m.getParameters()[2])){
								mail.addRecipient(m.getParameters()[2]);
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
										+ "Recipient" + SP + "`" + m.getParameters()[2]
										+ "`" + SP + RFC5321.getReplyMsg(RFC5321.R_250)
										+ CRLF;
								this.orden = 2;
							}
							else{
								outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
										+ "`" + m.getParameters()[2] + "`" + SP
										+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + CRLF;
								this.mEstado = S_MAIL;
							}
							break;
						case S_DATA:
							if(!mData){
								outputData = RFC5321.getReply(RFC5321.R_354) + SP
										+ RFC5321.getReplyMsg(RFC5321.R_354) + CRLF;
								this.orden = 3;
							}
							else{
								//TODO funci�n para limpiar la ip
								//TODO hacer una funci�n que analice lo que se recibe, si es solo un punto, es el fin del mensaje
								mail = new Mail();
								mail.setMailfrom(m.getParameters()[2]);
								mail.setIp(Ip(Server.TCP_CLIENT_IP));
								mail.setHost(Ip(Server.TCP_CLIENT_IP));
								mail.addMailLine(m.getArguments());
								mail.setSize();
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
										+ "Message accepted for delivery" + SP
										+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							}
							break;
						case S_RSET:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_250);
							this.mEstado = S_HELO;
							this.orden = 0;
							break;
						case S_QUIT:
							outputData = RFC5321.getReply(RFC5321.R_221) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_221);
							this.mFin = true;
							this.orden = -1;
							break;
						default:
							break;
						}
					}
					else{
						outputData = RFC5321.getError(RFC5321.E_500_SINTAXERROR) 
								+ CRLF;
					}

					// El servidor responde con lo recibido
//					mailbox = new Mailbox(mail);
					output.write(outputData.getBytes());
					output.flush();

				}//Fin del while
				System.out.println("Servidor [Conexi�n finalizada]> "
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
	public String Ip(String data){
		String ip = "";
		if(data.indexOf(":") > 0){
			String[] Parts = data.split(":");
			ip = Parts[0];
			ip = ip.substring(1, ip.length());
		}
		else{
			ip = ip.substring(1, ip.length());
		}
		return ip;
	}
	
	public Boolean Acceso(SMTPMessage m){
		Boolean acceso = false;
		if(mEstado == S_HELO && m.getCommandId() == 0 && this.orden == -1){
			acceso = true;
		}
		else if(mEstado == S_HELO && m.getCommandId() == 1 && this.orden == 0){
			this.mEstado = S_MAIL;
			acceso = true;
		}
		else if(mEstado == S_MAIL && m.getCommandId() == 2 && this.orden == 1){
			this.mEstado = S_RCPT;
			acceso = true;
		}
		else if(mEstado == S_RCPT && m.getCommandId() == 3 && this.orden == 2){
			this.mEstado = S_DATA;
			acceso = true;
			this.mData = false;
		}
		else if(orden == 3){
			this.mData = true;
		}
		else if(m.getCommandId() == 4 && this.orden > -1){
			this.mEstado = S_RSET;
			acceso = true;
		}
		else if(m.getCommandId() == 5){
			this.mEstado = S_QUIT;
			acceso = true;
		}
		return acceso;
	}
}
