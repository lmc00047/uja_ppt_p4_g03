package ujaen.git.ppt.smtp;

public class RFC5321 {
	
	public static String MSG_WELCOME 	= "Protocolos de Transporte - Servidor SMTP listo";
	public static String MSG_BYE 		= "Hasta otra!";
	public static String MSG_READY 		= "Mensaje listo para ser enviado";
	
	public static final String[] SMTP_REPLY_CODES = {"220","221","250","354"};
	
	public static final String[] SMPT_REPLY_MESSAGES ={ "Servicio OK","El servicio está cerrando el canal de transmisión","Ok","Inicie el envío del correo; termine con <CRLF>.<CRLF>"};
	public static final int R_220=0;
	public static final int R_221=1;
	public static final int R_250=2;
	public static final int R_354=3;
	
	public static final String[] SMTP_COMMANDS = {"HELO","MAIL FROM","RCPT TO","DATA","RSET","QUIT"};
	
	/** Commands Names*/
	public static final String N_HELO = "HELO";
	public static final String N_MAIL = "MAIL FROM";
	public static final String N_RCPT = "RCPT TO";
	public static final String N_DATA = "DATA";
	public static final String N_RSET = "RSET";
	public static final String N_QUIT = "QUIT";
	
	/** Commands IDs*/
	public static final int C_NOCOMMAND = -1;
	public static final int C_HELO = 0;
	public static final int C_MAIL = 1;
	public static final int C_RCPT = 2;
	public static final int C_DATA = 3;
	public static final int C_RSET = 4;
	public static final int C_QUIT = 5;
	
	/**
	 * Errors
	 */
	public static final String SMTP_ERROR_CODES[]={"500","503","551"};
	/**
	 * Error messages
	 */
	public static final String[] SMPT_ERROR_MESSAGES ={ "Error de sintaxis","Secuencia errónea de comandos","Usuario no local"};
	
	/** index to the list os error strings*/
	public static final int E_500_SINTAXERROR = 0;
	public static final int E_503_BADSEQUENCE = 1;
	public static final int E_551_USERNOTLOCAL = 2;
	
	
	public static String getCommand(int i)
	{
		if(i<SMTP_COMMANDS.length)
			return SMTP_COMMANDS[i];
		else
		return null; 
	}
	
	public static String getReply(int i)
	{
		if(i<SMTP_REPLY_CODES.length)
			return SMTP_REPLY_CODES[i];
		else
		return ""; 
	}
	
	public static String getReplyMsg(int i)
	{
		if(i<SMPT_REPLY_MESSAGES.length)
			return SMPT_REPLY_MESSAGES[i];
		else
		return ""; 
	}
	
	public static String getError(int i)
	{
		if(i<SMTP_ERROR_CODES.length)
			return SMTP_ERROR_CODES[i];
		else
		return ""; 
	}
	
	public static String getErrorMsg(int i)
	{
		if(i<SMPT_ERROR_MESSAGES.length)
			return SMPT_ERROR_MESSAGES[i];
		else
		return ""; 
	}
}
