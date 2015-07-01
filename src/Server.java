
import java.io.BufferedReader;
import java.net.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.*;


public class Server {

    
    
    private ServerSocket serverSocket;

    /**
     * Make a Server that listens for connections on port.
     * 
     * @param port port number, requires 0 <= port <= 65535
     * @throws IOException if cannot create a listening socket with the provided port
     * 
     * 
     */
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port); //create the socket for listening on the passed in port
        
    }
    
    /**
     * Run the server, listening for client connection and handling them.
     * Never returns unless an exception is thrown.
     * 
     * @throws IOException if the main server socket is broken
     *                     (IOExceptions from individual clients do *not* terminate serve())
     *                     
     * 
     */
    public void serve() throws IOException{

        while(true) {
            Socket socket = null; //socket for a client           
            socket = serverSocket.accept();
            InputStream sis = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(sis));
            String request = br.readLine(); // Now you get GET index.html HTTP/1.1`
            String[] requestParam = request.split(" ");
            String path = requestParam[1];
            System.out.println(request);

            
        }        
    }
    
    
    public static void main(String[] args) {
        //try and run the server
        int port = 10987; //default port
        HttpHandler handler=new HttpHandler(){
            @Override public void handle(    HttpExchange exchange) throws IOException {
                System.out.println("handling");
              System.out.println(exchange.getRequestMethod());
              if (exchange.getRequestMethod().equals("POST")){
                  BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                  String request = br.readLine();
                  System.out.println(request);
                  addMeters(request);
              }
              String objectToReturn=null;
              if (exchange.getRequestMethod().equals("GET")){
                   objectToReturn=getMeters(exchange.getRequestURI().toString());
              }
              byte[] response=null;
              if (objectToReturn==null){
                  String text = "{\"response\": 1234, \"text\":\"hi\" }";
                  response=text.getBytes();
              }
              else{
                  response=objectToReturn.getBytes();
              }
              exchange.getResponseHeaders().add("Access-Control-Allow-Origin" , "http://web.mit.edu");
              exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
              exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,response.length);
              exchange.getResponseBody().write(response);
              exchange.close();
             
            }
          };
        
        try {
            HttpServer jeeves = HttpServer.create(new InetSocketAddress(port), port);
            jeeves.createContext("/", handler);
            jeeves.setExecutor(null); // creates a default executor
            jeeves.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Queue<String> arguments = new LinkedList<String>(Arrays.asList(args));

    }
    
    /**
     * Start a Server running on the specified port. 
     * 
     * @param port The network port on which the server should listen.
     * @throws IOException if the server can't be started
     * 
     * 
     */
    public static void runlServer(int port) throws IOException {
        Server server = new Server(port);
        server.serve();
    }
    
    public static  void addMeters(String payload){
        HashMap<String,String> object =jsonParse(payload);
        String meters= object.get("meters");
        String lastName=object.get("lastName");
        String date=object.get("date");
        int tIndex=date.indexOf("T");
        date= date.substring(0, tIndex);
        String FILE_HEADER = "date,meters";
        String fileName=lastName+ ".csv"; //replace with lastname from payload
        URL serverPath = Server.class.getProtectionDomain().getCodeSource().getLocation();
        int index =serverPath.toString().indexOf("Meters");
        int start=serverPath.toString().indexOf("C:");
        String filePath=serverPath.toString().substring(start, index) + "Meters/Data/"+fileName;
        String teamPath=serverPath.toString().substring(start, index) + "Meters/Data/"+"team.csv";
        
        addMeterstoFile(filePath,date,meters);
        addMeterstoFile(teamPath,date,meters);
        
         
       
    }
    
    public static HashMap<String,String> jsonParse(String json){
        HashMap<String,String> jsonObject = new HashMap<String,String>();
        json= json.replace('{', ' ');
        json= json.replace('}', ' ');
        json=json.trim();
        String[] fields = json.split(",");
        for (String field: fields){
            String key = field.split(":")[0];
            key=key.replace('"',' ').trim();
            String value = field.split(":")[1];
            value=value.replace('"',' ').trim();
            jsonObject.put(key,value);
        }
        return jsonObject;        
    }
    
    public static void addMeterstoFile(String filePath, String date, String meters){
        String NEW_LINE_SEPARATOR = "\n";
        String COMMA_DELIMITER = ",";
        try{
            System.out.println(filePath.toString());
            FileWriter fileWriter = new FileWriter(filePath.toString(),true);
            fileWriter.append(date);
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(meters);
            fileWriter.append(NEW_LINE_SEPARATOR);
            
            fileWriter.flush();
            fileWriter.close();
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
    
    public static String getMeters(String args){
        String returnObj="{\"response\":[";
        args=args.replace('/', ' ').replace('?', ' ').trim();
        String lastName = args.split("=")[1];
        String fileName=lastName+ ".csv"; //replace with lastname from payload
        URL serverPath = Server.class.getProtectionDomain().getCodeSource().getLocation();
        int index =serverPath.toString().indexOf("Meters");
        int start=serverPath.toString().indexOf("C:");
        String filePath=serverPath.toString().substring(start, index) + "Meters/Data/"+fileName;
        
        
        HashMap<String,String> daysToMeters=new HashMap<String,String>();
        try{
            String line;
            BufferedReader fileReader =new BufferedReader(new FileReader(filePath));
            while (( line = fileReader.readLine()) != null) {
                String date=line.split(",")[0];
                String meters=line.split(",")[1];
                if (!daysToMeters.containsKey(line.split(",")[0])){
                    daysToMeters.put(date, meters);
                }
                else{
                    daysToMeters.put(date,  ""+(Integer.parseInt(meters)+ Integer.parseInt(daysToMeters.get(date))) );
                } 
                
                
            }
            fileReader.close();
            
        }
        catch(Exception e){System.out.println(e.getMessage()); System.out.println(e.getClass());}
        for (String date:daysToMeters.keySet()){
            returnObj=returnObj+"{\"date\":\"";
            returnObj=returnObj+date;
            returnObj=returnObj+"\",\"meters\":";
            returnObj=returnObj+daysToMeters.get(date);
            returnObj=returnObj+"},";
        }
        returnObj=returnObj.substring(0, (returnObj.length()-1)); //remove last comma
        returnObj=returnObj+"]}";
        return returnObj;
    }



}
