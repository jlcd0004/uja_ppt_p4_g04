package ujaen.git.ppt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

import ujaen.git.ppt.mail.Mailbox;
import ujaen.git.ppt.smtp.RFC5321;
import ujaen.git.ppt.smtp.RFC5322;
import ujaen.git.ppt.smtp.SMTPMessage;

public class Connection implements Runnable, RFC5322 {

	public static final int S_HELO = 0;

	protected Socket mSocket;
	protected int mEstado = S_HELO;;
	private boolean mFin = false;
	public String origen, destino;

	public Connection(Socket s) {
		mSocket = s;
		mEstado = 0;
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

				while (!mFin ) {

					// Todo análisis del comando recibido
					// SMTPMessage m = new SMTPMessage(inputData);

					// TODO: Máquina de estados del protocolo
					switch (mEstado) {
					case S_HELO:
						
						mEstado = RFC5321.C_MAIL;
						break;
					case RFC5321.C_MAIL:
						inputData = input.readLine();
						if (inputData.startsWith("quit") || inputData.startsWith("QUIT")){
							mFin=true;
						}else{
						if (inputData.startsWith("MAIL FROM: ") || inputData.startsWith("mail from: ")) {
							System.out.println("Servidor [Recibido]> "
									+ inputData);
							origen = inputData.substring(11);
							outputData = RFC5321.getReply(RFC5321.R_220) + SP
									+ origen + SP + "remitente" + CRLF;
							output.write(outputData.getBytes());
							output.flush();
							mEstado = RFC5321.C_RCPT;
						} else{
							output.writeUTF(RFC5321
									.getError(RFC5321.E_500_SINTAXERROR)
									+ SP
									+ RFC5321
											.getErrorMsg(RFC5321.E_500_SINTAXERROR)
									+ CRLF);}}
						break;
					case RFC5321.C_RCPT:
						
						inputData = input.readLine();
						if (inputData.startsWith("quit") || inputData.startsWith("QUIT")){
							mFin=true;
						}else{
						if (inputData.startsWith("RCPT TO: ") || inputData.startsWith("rcpt to: ")) {
							System.out.println("Servidor [Recibido]> "
									+ inputData);
							destino = inputData.substring(11);
							outputData = RFC5321.getReply(RFC5321.R_220) + SP
									+ destino + SP + "destinatario" + CRLF;
							output.write(outputData.getBytes());
							Mailbox a = new Mailbox(destino);
							
							if(a.open(destino)==true){
								output.writeBytes("exito");
							}else output.writeBytes("nada");
							output.flush();
							mEstado = RFC5321.C_RCPT;
						} else{
							output.writeUTF(RFC5321
									.getError(RFC5321.E_500_SINTAXERROR)
									+ SP
									+ RFC5321
											.getErrorMsg(RFC5321.E_500_SINTAXERROR)
									+ CRLF);}}
						
						break;

					case RFC5321.C_DATA:
						break;
					}

					// TODO montar la respuesta
					// El servidor responde con lo recibido
					// outputData = RFC5321.getReply(RFC5321.R_220) + SP +
					// inputData + CRLF;
					// output.write(outputData.getBytes());
					// output.flush();

				}
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
}
