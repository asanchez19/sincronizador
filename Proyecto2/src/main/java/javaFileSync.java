import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;

public class javaFileSync {
	private static final int PORT_NUMBER = 8889;
	private static String localName;
	private static String fullPathName;
	
	public static void main(String[] args) {
		try {
			if(args.length > 0) {
				if (args[0].equalsIgnoreCase("-s")) {
					server();
				} else if (args[0].equalsIgnoreCase("-c") && args.length > 1 && args.length < 4) {
					localName = cleanUpInput(args[2]);
					client(localName, args[2], args[1]);
				} else {
					System.out.println("Entrada incorrecta. Ejemplo: java javaFileSync -s [-c [IP del server] [directorio o -ls]]");
				}
			} else {
				System.out.println("Entrada incorrecta. Ejemplo: java javaFileSync -s [-c [IP del server] [directorio o -ls]]");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private int port;                   //  Puerto

	private ZContext ctx;
	private Socket pipe;
	private ZmqConfig config;
	private Socket router;
	private Map<String, Client> clients;
	public void ServerZMQ (ZContext ctx, Socket pipe)
	{
		this.ctx = ctx;
		this.pipe = pipe;
		router = ctx.createSocket (ZMQ.ROUTER);
		clients = new HashMap<String, Client>();
	}

	private void controlMessage ()
	{
		ZMsg msg = ZMsg.recvMsg (pipe);
		String method = msg.popString ();
		if (method.equals ("BIND")) {
			String endpoint = msg.popString ();
			pipe.send (String.format ("%d", port));
		}
		else
		if (method.equals ("PUBLISH")) {
			String location = msg.popString ();
			String alias = msg.popString ();
		}
		else
		if (method.equals ("SET ANONYMOUS")) {
			long enabled = Long.parseLong (msg.popString ());
			//  Enable anonymous access without a config file
			config.setPath ("security/anonymous", enabled > 0 ? "1" :"0");
		}
		else
		if (method.equals ("CONFIG")) {
			String config_file = msg.popString ();
			config.destroy ();
			config = ZmqConfig.load (config_file);

		}
		else
		if (method.equals ("SETOPTION")) {
			String path = msg.popString ();
			String value = msg.popString ();
			config.setPath (path, value);
		}
		else
		if (method.equals ("STOP")) {
			pipe.send ("OK");
		}
		msg.destroy ();
		msg = null;
	}

	public static void server() throws Exception {
		Server s = new Server(PORT_NUMBER);
		s.startServer();
	}
	
	public static void client(String dirName, String fullDirName, String serverIP) throws Exception {
		Client c = new Client(dirName, fullDirName, serverIP, PORT_NUMBER);
		c.runClient();
	}
	
	public static void testing(File dirName, String fullPathName) throws Exception {
		File f = new File("./");
		String[] children = f.list();
        for (int i=0; i<children.length; i++) {
        	File newF = new File(f, children[i]);
        	if(newF.isDirectory())
        		System.out.print(newF.getName() + " ");
        }
        System.out.println();
	}
	
	public static String cleanUpInput(String userInput) throws Exception {

		if(userInput.equalsIgnoreCase("-ls"))
			return userInput;

		File f = new File(userInput);

		if(!f.isDirectory()) {
			System.out.println("Por favor introduzca un directorio en lugar de un archivo.");
			Thread.sleep(10);
			System.exit(0);
		}

		String localDirName = userInput; //cleaning up the users input
		if(userInput.contains("/")){
			if(userInput.lastIndexOf("/") != (userInput.length() - 1)) {
				localDirName = userInput.substring(userInput.lastIndexOf("/"));
			} else {
				localDirName = userInput.substring(0, (userInput.length() - 1));
				if(localDirName.contains("/"))
					localDirName = localDirName.substring(localDirName.lastIndexOf("/"));
			}
		}

		if(localDirName.equals(".")){
			System.out.println("Por favor introduzca el nombre del server en vez de ./ o .");
			Thread.sleep(10);
			System.exit(0);
		}
		
		if(!localDirName.startsWith("./")){ //Limpiando
			if(localDirName.startsWith("/"))
				localDirName = "." + localDirName;
			else
				localDirName = "./" + localDirName;
		}
		return localDirName;
	}
	
	// Procesando todos los archivos en el directorio
	public static void visitAllDirsAndFiles(File dir) {
	    //process(dir);
		System.out.println("NOMBRE: " + dir.getName() + " MODIFICADO: " + dir.lastModified() + " TAMANO: " + dir.length());
		System.out.println(fullPathName + dir.getAbsolutePath().substring((dir.getAbsolutePath().indexOf(fullPathName)  + fullPathName.length())) + " Directory? " + dir.isDirectory());
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            visitAllDirsAndFiles(new File(dir, children[i]));
	        }
	    }
	}

	// Procesando solo directorios bajo el directorio actual
	public static void visitAllDirs(File dir) {
	    if (dir.isDirectory()) {
		    //process(dir);
			System.out.println("NOMBRE: " + dir.getName() + " MODIFICADO: " + dir.lastModified() + " TAMANO: " + dir.length());
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            visitAllDirs(new File(dir, children[i]));
	        }
	    }
	}

	// PROCESANDO SOLO ARCHIVOS BAJO EL DIRECTORIO
	public static void visitAllFiles(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i=0; i<children.length; i++) {
	            visitAllFiles(new File(dir, children[i]));
	        }
	    } else {
		    //process(dir);
			System.out.println("NOMBRE: " + dir.getName() + " MODIFICADO: " + dir.lastModified() + " TAMANO: " + dir.length());
	    }
	}
	
}
