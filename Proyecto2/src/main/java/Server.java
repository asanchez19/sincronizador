import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
	private int PORT_NUMBER;
	private static final String DONE = "DONE";
	private static Socket sock;
	private static ObjectOutputStream oos;
	private static ObjectInputStream ois;
	private static ServerSocket servsock;
	private static String baseDir;

	public Server(int port) {
		PORT_NUMBER = port;
	}

	public void startServer() throws Exception {
		System.out.println("INICIALIZANDO EL SINCRONIZADOR!");

		servsock = new ServerSocket(PORT_NUMBER);
		while (true) {
			sock = servsock.accept();

			ois = new ObjectInputStream(sock.getInputStream());

			baseDir = (String) ois.readObject();

			oos = new ObjectOutputStream(sock.getOutputStream());

			System.out.println("NUEVO CLIENTE CONECTADO. IP: " + sock.getInetAddress().toString() + " Directorio: " + baseDir);

			if(baseDir.equalsIgnoreCase("-ls")) {
				Vector<String> directories = new Vector<String>();
				File serverBase = new File("./");
				String[] children = serverBase.list();
		        for (int i=0; i<children.length; i++) {
		        	File newF = new File(serverBase, children[i]);
		        	if(newF.isDirectory())
		        		directories.add(newF.getName());
		        }
		        oos.writeObject(directories);
		        oos.flush();
			} else {
				File fBaseDir = new File(baseDir);
				Boolean baseDirExists = fBaseDir.exists();

				if(!baseDirExists)
					fBaseDir.mkdir();

				oos.writeObject(new Boolean(baseDirExists));
				oos.flush();

				Boolean isClientDone = false;

				while (!isClientDone) {
					Vector<String> vec = (Vector<String>) ois.readObject();
					reinitConn();

					if(vec.elementAt(0).equals(DONE)) {  // Revisando si se completo la sincronizacion
						isClientDone = true; // if so break out
						break;
					}

					if(vec.size() == 2) { // si el tamano es dos entonces es un directorio
						File newDir = new File(baseDir, vec.elementAt(1));
						if (!newDir.exists())
							newDir.mkdir();

						oos.writeObject(new Boolean(true)); // notificando al cliente que hemos terminado
						oos.flush();
					} else {
						File newFile = new File(baseDir, vec.elementAt(1));
						Integer updateFromClient = 2; // default = no hacer nada

						Long lastModified = new Long(vec.elementAt(2));
						if (!newFile.exists() || (newFile.lastModified() <= lastModified))
							updateFromClient = 1;
						else
							updateFromClient = 0;

						if(newFile.exists() && newFile.lastModified() == lastModified)
							updateFromClient = 2;

						if(updateFromClient == 1) { // If true recibe el archivo del cliente
							newFile.delete();

							oos.writeObject(new Integer(updateFromClient));
							oos.flush();

							receiveFile(newFile);

							newFile.setLastModified(lastModified);

							oos.writeObject(new Boolean(true));
						} else if (updateFromClient == 0) { // if false envia archivo al cliente
							oos.writeObject(new Integer(updateFromClient));
							oos.flush();

							ois.readObject();

							sendFile(newFile);

							ois.readObject();

							oos.writeObject(new Long(newFile.lastModified()));
							oos.flush();
						} else { //updateFromClient == 2 // no hace nada
							oos.writeObject(new Integer(updateFromClient));
							oos.flush();
						}
					}
				}

				File baseDirFile = new File(baseDir);
				if(baseDirExists)
					visitAllDirsAndFiles(baseDirFile);

				oos.writeObject(new String(DONE));
				oos.flush();
				System.out.print("Finalizando sincronizador...");

			}
			oos.close();
			ois.close();
			sock.close();
			System.out.println("Cliente desconectado.");
		}
	}

	private static void visitAllDirsAndFiles(File dir) throws Exception {
		oos.writeObject(new String(dir.getAbsolutePath().substring((dir.getAbsolutePath().indexOf(baseDir) + baseDir.length()))));
		oos.flush();

		ois.readObject();

		Boolean isDirectory = dir.isDirectory();
		oos.writeObject(new Boolean(isDirectory));
		oos.flush();

		if (isDirectory) {
			if (!(Boolean) ois.readObject()) {
				oos.writeObject(new Boolean(true));
				oos.flush();

				Boolean delete = (Boolean) ois.readObject();

				if (delete) {
					deleteAllDirsAndFiles(dir);
					return;
				} //ELSE DO NOTHING
			}
		} else {
			if (!(Boolean) ois.readObject()) {
				oos.writeObject(new Boolean(true));
				oos.flush();

				Integer delete = (Integer) ois.readObject();

				if (delete == 1) {
					dir.delete();
					return;
				} else if (delete == 0) {
					sendFile(dir);

					ois.readObject();

					oos.writeObject(new Long(dir.lastModified()));
					oos.flush();

					ois.readObject();
				} // ELSE DO NOTHING!
			}
		}

		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				visitAllDirsAndFiles(new File(dir, children[i]));
			}
		}
	}

	private static void sendFile(File dir) throws Exception {
		byte[] buff = new byte[sock.getSendBufferSize()];
		int bytesRead = 0;

		InputStream in = new FileInputStream(dir);

		while((bytesRead = in.read(buff))>0) {
			oos.write(buff,0,bytesRead);
		}
		in.close();
		// Despues de enviar el archivo se necesita cerrar el socket y abrirlo de nuevo.
		oos.flush();
		reinitConn();
	}

	private static void receiveFile(File dir) throws Exception {
		FileOutputStream wr = new FileOutputStream(dir);
		byte[] outBuffer = new byte[sock.getReceiveBufferSize()];
		int bytesReceived = 0;
		while((bytesReceived = ois.read(outBuffer))>0) {
			wr.write(outBuffer,0,bytesReceived);
		}
		wr.flush();
		wr.close();

		reinitConn();
	}

	private static void reinitConn() throws Exception {
		oos.close();
		ois.close();
		sock.close();
		sock = servsock.accept();
		oos = new ObjectOutputStream(sock.getOutputStream());
		ois = new ObjectInputStream(sock.getInputStream());
	}

	private static void deleteAllDirsAndFiles(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				deleteAllDirsAndFiles(new File(dir, children[i]));
			}
		}
		dir.delete();
	}
}
