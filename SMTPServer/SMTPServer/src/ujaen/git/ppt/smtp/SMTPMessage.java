package ujaen.git.ppt.smtp;

import ujaen.git.ppt.Connection;

public class SMTPMessage implements RFC5322 {

	protected String mCommand = null;
	protected int mCommandId = RFC5321.C_NOCOMMAND;
	protected String mArguments = null;
	protected String[] mParameters = null;
	protected boolean mHasError = false;
	protected int mErrorCode = 0;
	protected boolean mdata = false;

	/**
	 * The input string is processed to analyze the format of the message
	 * 
	 * @param data
	 */
	public SMTPMessage(String data) {

		if(data.length()>998){
			mHasError = true;
		}
		else
			mHasError = parseCommand(data);

	}

	/**
	 * 
	 * @param data
	 * @return true if there were errors
	 */
	protected boolean parseCommand(String data){
		boolean caso = true;
		//Para comandos con :
		if(data.indexOf(":") > 0){
			String[] commandParts = data.split(":");// Se busca los comandos con varias palabras MAIL, FROM:
			//Se recibe el comando MAIL:
			if(commandParts[0].equalsIgnoreCase(RFC5321.N_MAIL)){
				String[] commandParameters = data.split(" ");
				setParameters(commandParameters);
				checkCommand(RFC5321.N_MAIL);
				caso = false;
				this.mdata = false;
			}
			//Se recibe el comando RCPT
			else if(commandParts[0].equalsIgnoreCase(RFC5321.N_RCPT)){
				String[] commandParameters = data.split(" ");
				setParameters(commandParameters);
				checkCommand(RFC5321.N_RCPT);
				caso = false;
			}
			else if(Connection.Estado() == Connection.S_DATA){
				setArguments(data);
				caso = false;
			}
		}
		//Para comandos sin :
		//Se recibe el comando HELO
		else if(data.equalsIgnoreCase(RFC5321.N_HELO)){
			checkCommand(RFC5321.N_HELO);
			caso = false;
		}
		//Se recibe el comando DATA
		else if(data.equalsIgnoreCase(RFC5321.N_DATA)){
			checkCommand(RFC5321.N_DATA);
			caso = false;
			this.mdata = true;
		}
		//Se recibe el comando RSET
		else if(data.equalsIgnoreCase(RFC5321.N_RSET)){
			checkCommand(RFC5321.N_RSET);
			caso = false;
			this.mdata = false;
		}
		//Se recibe el comando QUIT
		else if(data.equalsIgnoreCase(RFC5321.N_QUIT)){
			checkCommand(RFC5321.N_QUIT);
			caso = false;
		}
		else if(Connection.Estado() == Connection.S_DATA){
			setArguments(data);
			caso = false;
		}
		//Recibo datos
		
		return caso;
	}

	public String toString() {
		if (!mHasError) {
			String result = "";
			result = this.mCommand;
			if (this.mCommandId == RFC5321.C_MAIL
					|| this.mCommandId == RFC5321.C_RCPT)
				result = result + ":";
			if (this.mArguments != null)
				result = result + this.mArguments;
			if (this.mParameters != null)
				for (String s : this.mParameters)
					result = result + SP + s;

			result = result + CRLF;
			//opcional
			result=result+"id="+this.mCommandId;
			return result;
		} else
			return "Error";
	}

	/**
	 * 
	 * @param data
	 * @return The id of the SMTP command
	 */
	protected int checkCommand(String data) {
		int index = 0;

		this.mCommandId = RFC5321.C_NOCOMMAND;

		for (String c : RFC5321.SMTP_COMMANDS) {
			if(data.compareToIgnoreCase(c) == 0){
				this.mCommandId = index;
			}
			index++;
		}

		if (mCommandId != RFC5321.C_NOCOMMAND)
			mCommand = RFC5321.getCommand(mCommandId);
		else
			mCommand = null;

		return this.mCommandId;
	}

	public String getCommand() {
		return mCommand;
	}

	public void setCommand(String mCommand) {
		this.mCommand = mCommand;
	}

	public int getCommandId() {
		return mCommandId;
	}

	public void setCommandId(int mCommandId) {
		this.mCommandId = mCommandId;
	}

	public String getArguments() {
		return mArguments;
	}

	public void setArguments(String mArguments) {
		this.mArguments = mArguments;
	}

	public String[] getParameters() {
		return mParameters;
	}

	public void setParameters(String[] mParameters) {
		this.mParameters = mParameters;
	}

	public boolean hasError() {
		return mHasError;
	}

	
	public int getErrorCode() {
		return mErrorCode;
	}

	public void setErrorCode(int mErrorCode) {
		this.mErrorCode = mErrorCode;
	}

}
