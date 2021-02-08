package mainpackage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.sql.*;

// The tutorial can be found just here on the SSaurel's Blog : 
// https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java
// Each Client Connection will be managed in a dedicated Thread
public class JavaHTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String PAGE_MOVED_PERMANENTLY = "ciccio.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	static final int PORT = 3000;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	private Socket connect;
	
	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) throws IOException {
		connectToDatabase ();
		jsonToXML("./puntiVendita.json");
		jsonToXML(".//db//cars.json");
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				
				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			// we get file requested
			fileRequested = parse.nextToken().toLowerCase();
			
			// we support only GET and HEAD methods, we check
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				// we send HTTP Headers with data to client
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: Java HTTP Server from SSaurel : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); // blank line between headers and content, very important !
				out.flush(); // flush character output stream buffer
				// file
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
			} else {
				// GET or HEAD method
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
				
				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);
				
				if (method.equals("GET")) { // GET method so we return content
						byte[] fileData = readFileData(file, fileLength);
						
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: Java HTTP Server from SSaurel : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println(); // blank line between headers and content, very important !
						out.flush(); // flush character output stream buffer
						
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
					
					if (verbose) {
						System.out.println("File " + fileRequested + " of type " + content + " returned");
					}
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private static void jsonToXML (String filejson) throws IOException
	{
		Path path = Paths.get(filejson);
		String str = Files.readString(path, StandardCharsets.US_ASCII);
		JSONObject json = new JSONObject(str);
		String xml = XML.toString(json);
		System.out.println(xml);
		filejson = filejson.replace(".json", ".xml");
		Path xmlpath = Paths.get(filejson);
		Files.writeString(xmlpath, xml);
	}
	
	private static void connectToDatabase ()
	{	
		JSONObject mainObj = null;
		Car car = new Car(); // nuova istanza oggetto car
        String mySQLDriver = "com.mysql.cj.jdbc.Driver"; // selezione driver aggiornato
        
        try
        {
            Class.forName(mySQLDriver);
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Driver not found " + e);
            System.exit(0);
        }
        // jdbc:mysql://indirizzo/schema?[args]
        String url_db = "jdbc:mysql://localhost:3306/cars?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        // query per marca e modello dalla table car_table
        String query = "SELECT marca, modello FROM car_table";
        System.out.println("Connettendo presso: " + url_db); // output per tentativo di connessione con db
        Connection connection = null; // inizializzazione variabile
        
        try
        {
            connection = DriverManager.getConnection(url_db, "root", "root"); // connessione a url tramite user e password
        }
        catch (Exception e)
        {
            System.out.println("Errore durante la connessione: " + e);
            System.exit(0);
        }
        
        try
        {
            Statement statement = connection.createStatement(); // creazione statement
            ResultSet resultset = statement.executeQuery(query); // query presso database
            mainObj = new JSONObject();
    		ObjectMapper Obj = new ObjectMapper();
    		JSONArray ja = new JSONArray();
            while(resultset.next())
            {
            	JSONObject jo = new JSONObject();
            	car.setMarca (resultset.getString(1));
            	car.setModello (resultset.getString(2));
                System.out.println("Risultati");
                System.out.println("Marca: " + car.getMarca());
                System.out.println("Modello: " + car.getModello());
                //String jsonStr = Obj.writeValueAsString(car);
                //System.out.println(jsonStr);
                //ja.put(jsonStr);
                jo.put("marca", car.getMarca());
                jo.put("modello", car.getModello());
                ja.put(jo);
                mainObj.put("cars", ja);
                System.out.println(mainObj);
            }
            FileWriter fw = new FileWriter(".\\db\\cars.json", false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter output = new PrintWriter(bw);
        	output.println(mainObj);
        	output.close();
        }
        catch(Exception e)
        {
            System.out.println("Eccezione: " + e);
            System.exit(0);
        }
        finally
        {
            if(connection != null)
            {
                try
                {
                    connection.close();
                }
                catch(Exception e)
                {
                    System.out.println("Errore durante chiusura connessione: " + e);
                }
            }
        }
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else if (fileRequested.endsWith(".json"))
			return "application/json";
		else
			return "text/plain";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		
		if ( (!(fileRequested.endsWith("/")))&&!(fileRequested.endsWith(".html"))&&!(fileRequested.endsWith(".xml"))&&!(fileRequested.endsWith(".json")) ) // Se non finisce con .html e non finisce con /
		{
			File file = new File(WEB_ROOT, PAGE_MOVED_PERMANENTLY);
			int fileLength = (int) file.length();
			String content = "text/html";
			byte[] fileData = readFileData(file, fileLength);
			out.println("HTTP/1.1 301 Moved Permanently"); // Errore 301
			out.println("Server: Java HTTP Server from SSaurel : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			out.println("Content-length: " + fileLength);
			out.println("Location: " + fileRequested + "/"); // Invio nuova location, disponibile per ciccio
			out.println(); // blank line between headers and content, very important !
			out.flush(); // flush character output stream buffer
			
			dataOut.write(fileData, 0, fileLength);
			dataOut.flush();
			System.out.println("File " + fileRequested + " moved");
		}
		else {
			File file = new File(WEB_ROOT, FILE_NOT_FOUND);
			int fileLength = (int) file.length();
			String content = "text/html";
			byte[] fileData = readFileData(file, fileLength);
			
			
			out.println("HTTP/1.1 404 File Not Found");
			out.println("Server: Java HTTP Server from SSaurel : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			out.println("Content-length: " + fileLength);
			out.println(); // blank line between headers and content, very important !
			out.flush(); // flush character output stream buffer
			
			dataOut.write(fileData, 0, fileLength);
			dataOut.flush();
			
			if (verbose) {
				System.out.println("File " + fileRequested + " not found");
			}
		}
		
	}
	
}
