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

public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;
	public static final int S_EHLO = 1;
	public static final int S_MAIL = 2;
	public static final int S_RCPT = 3;
	public static final int S_DATA = 4;
	public static final int S_RSET = 5;
	public static final int S_QUIT = 6;

	protected Socket mSocket;
	protected static int mEstado = S_HELO;;
	private boolean mFin = false;
	int orden = -1;
	Boolean mData, mSend = true;
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
				// Inicialización de los streams de entrada y salida
				DataOutputStream output = new DataOutputStream(
						mSocket.getOutputStream());
				BufferedReader input = new BufferedReader(
						new InputStreamReader(mSocket.getInputStream()));

				// Envío del mensaje de bienvenida
				String response = RFC5321.getReply(RFC5321.R_220) + SP
						+ RFC5321.getReplyMsg(RFC5321.R_220) + SP
						+ RFC5321.MSG_WELCOME + RFC5322.CRLF;
				output.write(response.getBytes());
				output.flush();
				
				

				while (!mFin && ((inputData = input.readLine()) != null)){

					System.out.println("Servidor [Recibido]> " + inputData);

					//Análisis del comando recibido
					SMTPMessage m = new SMTPMessage(inputData);
					
					//Máquina de estados
					if(Acceso(m) && !m.hasError()){
						switch (mEstado){
						case S_HELO:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_250) + SP
									+ "Welcome " + Server.TCP_CLIENT_IP +
									", pleased to meet you"+ CRLF;
							this.orden = 0;
							break;
						case S_MAIL:
							//La sobrecarga de control permite que la aplicación no se cuelgue si no se introduce
							//usuario y clave.
							if(m.getParameters().length != 4){
								if(m.getParameters().length == 2){
									outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
											+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + SP
											+ "`" + "unknown sender" + "`" + CRLF;
								}
								if(m.getParameters().length == 3){
									outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
											+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + SP
											+ "`" + m.getParameters()[2] + "`" + SP
											+ "no password" + CRLF;
								}
								Connection.mEstado = S_HELO;
							}
							else if(Mailbox.checkRecipient(m.getParameters()[2]) && Mailbox.checkKey(m.getParameters()[2], m.getParameters()[3])){
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
										+ RFC5321.getReplyMsg(RFC5321.R_250) + SP
										+ "Sender" + SP + "`" + m.getParameters()[2] + "`" 
										+ CRLF;
								this.orden = 1;
							}
							else{
								outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
										+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + SP
										+ "`" + m.getParameters()[2] + "`" + CRLF;
								Connection.mEstado = S_HELO;
							}
							break;
						case S_RCPT:
							//Igual que S_MAIL
							if(m.getParameters().length != 3){
								outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
										+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + SP
										+ "`" + "unknown recipient" + "`" + CRLF;
								Connection.mEstado = S_MAIL;
							}
							else if(Mailbox.checkRecipient(m.getParameters()[2])){
								outputData = RFC5321.getReply(RFC5321.R_250) + SP
										+ RFC5321.getReplyMsg(RFC5321.R_250) + SP
										+ "Recipient" + SP + "`" + m.getParameters()[2]
										+ CRLF;
								this.orden = 2;
							}
							else{
								outputData = RFC5321.getError(RFC5321.E_551_USERNOTLOCAL) + SP
										+ RFC5321.getErrorMsg(RFC5321.E_551_USERNOTLOCAL) + SP
										+ "`" + m.getParameters()[2] + "`" + CRLF;
								Connection.mEstado = S_MAIL;
							}
							break;
						case S_DATA:
							//Este es el caso de que se envíe el comando DATA
							if(!mData){
								mail = new Mail();
								outputData = RFC5321.getReply(RFC5321.R_354) + SP
										+ RFC5321.getReplyMsg(RFC5321.R_354) + CRLF;
								this.orden = 3;
							}
							//Este es el caso de que se envíen los datos
							else{
								//Si no se recibe el .
								if(!Punto(inputData)){
									AddtoMail(inputData);
									mSend = false;
								}
								//Cuando se recibe .
								else{
									mail.setIp(Server.TCP_CLIENT_IP);
									mail.setHost(Server.TCP_CLIENT_IP);
									mail.setSize();
									mail.Headers();
									mailbox = new Mailbox(mail);
									outputData = RFC5321.getReply(RFC5321.R_250) + SP
											+ RFC5321.getReplyMsg(RFC5321.R_250) + SP
											+ "Message accepted for delivery" + CRLF;
									//Una vez enviado el mail volvemos a las condiciones de después de HELO
									this.orden = 0;
									Connection.mEstado = S_HELO;
									mSend = true;
								}
							}
							break;
						case S_RSET:
							outputData = RFC5321.getReply(RFC5321.R_250) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_250) + CRLF;
							Connection.mEstado = S_HELO;
							this.orden = 0;
							break;
						case S_QUIT:
							outputData = RFC5321.getReply(RFC5321.R_221) + SP
									+ RFC5321.getReplyMsg(RFC5321.R_221) + CRLF;
							this.mFin = true;
							this.orden = -1;
							break;
						default:
							break;
						}
					}
					//Si se envía un comando válido pero no en el orden correcto.
					else if(!m.hasError()){
						outputData = RFC5321.getError(RFC5321.E_503_BADSEQUENCE) + SP
								+ RFC5321.getErrorMsg(RFC5321.E_503_BADSEQUENCE) + CRLF;
					}
					//Si lo que se recibe no está bien escrito.
					else{
						outputData = RFC5321.getError(RFC5321.E_500_SINTAXERROR) + SP
								+ RFC5321.getErrorMsg(RFC5321.E_500_SINTAXERROR) + CRLF;
					}
					//Permite enviar datos solo cuando se desea.
					if(this.mSend){
						output.write(outputData.getBytes());
						output.flush();
					}

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
	//Detecta la recepción de .
	public Boolean Punto(String data){
		if(data.equalsIgnoreCase(".")){
			return true;
		}
		else{
			return false;
		}
	}
	
	public void AddtoMail(String data){
		//Llegan cabeceras
		if(data.indexOf(":") > 0){
			String[] commandParts = data.split(":");
			//Se recibe Date:
			if(commandParts[0].equalsIgnoreCase("Date")){
				this.mail.addMailLine(data);
			}
			//Se recibe To:
			else if(commandParts[0].equalsIgnoreCase("To")){
				this.mail.setMailfrom(data.substring(4));
				this.mail.addMailLine(data);
			}
			//Se recibe From:
			else if(commandParts[0].equalsIgnoreCase("From")){
				String[] parts = data.split(" ");
				if(parts.length > 2){
					String from = parts[0] + " " + parts[1];
					this.mail.addRecipient(from.substring(6));
					this.mail.addMailLine(from);
				}
				else{
					this.mail.addRecipient(data.substring(6));
					this.mail.addMailLine(data);
				}
			}
			//Se recibe Subject:
			else if(commandParts[0].equalsIgnoreCase("Subject")){
				this.mail.addMailLine(data);
			}
		}
		//Llegan datos
		else{
			this.mail.addMailLine(data);
		}
	}
	//Autoriza el acceso a la máquina de estados en función del estado actual, el comando recibido
	//y el orden de estos comandos.
	public Boolean Acceso(SMTPMessage m){
		Boolean acceso = false;
		if(mEstado == S_HELO && m.getCommandId() == 0 && this.orden == -1){
			acceso = true;
		}
		else if(mEstado == S_HELO && m.getCommandId() == 1 && this.orden == 0){
			Connection.mEstado = S_MAIL;
			acceso = true;
		}
		else if(mEstado == S_MAIL && m.getCommandId() == 2 && this.orden == 1){
			Connection.mEstado = S_RCPT;
			acceso = true;
		}
		else if(mEstado == S_RCPT && m.getCommandId() == 3 && this.orden == 2){
			Connection.mEstado = S_DATA;
			acceso = true;
			this.mData = false;
		}
		else if(orden == 3){
			acceso = true;
			this.mData = true;
		}
		else if(m.getCommandId() == 4 && this.orden > -1){
			Connection.mEstado = S_RSET;
			acceso = true;
		}
		else if(m.getCommandId() == 5){
			Connection.mEstado = S_QUIT;
			acceso = true;
		}
		return acceso;
	}
	//devuelve el estado actual de la maquina de estados.
	public static int Estado(){
		return mEstado;
	}
}
