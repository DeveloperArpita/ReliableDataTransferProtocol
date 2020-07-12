import java.net.*; 
import java.io.*;
 import java.util.*; 
 

 
public class FServer22 { 
 
 public static final byte[] RDT = new String ("RDT").getBytes();    
 public static final byte[] END = new String ("END").getBytes();    
 public static final byte[] CRLF = new String ("\t\n").getBytes(); 
 
 public static void main (String[] args) { 
 
  final int CONSIGNMENT = 512; // max length of payload  
  final String transferStart = new String ("REQUEST"), crlf = new String (CRLF); 
 
  DatagramSocket ss = null;  
  DatagramPacket rp, sp = null; 
  FileInputStream fis = null; 
  byte[] rd, sd, payload, myLastData;  
  InetAddress ip; 
  int port, bytesRead = 0, seq = 0, i;   
  String filename, strConsignment;  
  boolean flag = true;
  byte b;  
 
  try {   
  ss = new DatagramSocket (Integer.parseInt(args[0]));   
  ss.setSoTimeout (30); // set timeout to 30 ms  
  System.out.print ("\nSERVER IS UP..."); 
 
	while (true) { 
    rd = new byte [100];   
	rp = new DatagramPacket (rd, rd.length);         
	try {    
	ss.receive (rp);   
	}    
	catch (SocketTimeoutException ex) {  
    if (sp != null) {    
	System.out.println ("Timeout");      
	ss.send (sp);      
	System.out.println ("Sent Consignment " + seq);    
	}     
	continue;   
	} 
 
    // get client's consignment request from DatagramPacket   
	ip = rp.getAddress();     
	port = rp.getPort();       
	strConsignment = new String (rp.getData());      
	if (strConsignment.startsWith (transferStart)) {      
	filename = new String (strConsignment.substring (transferStart.length(), strConsignment.indexOf (crlf)));     
	System.out.println ("\n\nReceived request for " + filename + " from " + ip + " port " + port);  
    fis = new FileInputStream (filename);  // read file into buffer   
	seq = 0;    
	}    
	else {   
	seq = (rd[RDT.length]); 
    b=(byte)seq;	
	System.out.println ("\nReceived ACK " + seq);    
	if (seq == 0 && bytesRead < CONSIGNMENT) {  // last consignment    
	System.out.println ("END");     
	sp = null;    
	}   
	} 
 
    // prepare data    
	payload = new byte [CONSIGNMENT]; 
    bytesRead = fis.read (payload);   
	if (bytesRead > -1) {  
    if (bytesRead < CONSIGNMENT) {  
	// last consignment      // make a special byte array that exactly fits the number of bytes read otherwise the consignment may be padded with junk data                            
	 myLastData = new byte [bytesRead]; 


 
                           for (i = 0; i < bytesRead; i++)                               
						   myLastData[i] = payload[i];                         
						   sd = readyEndMsg (new byte [] {(byte)seq}, myLastData);                  
						   }      else if (fis.available() == 0)  // last consignment 
						   sd = readyEndMsg (new byte [] {(byte)seq}, payload);        
						   else // intermediate consignment  
						   sd = readyMsg (new byte [] {(byte)seq}, payload);        
						   sp = new DatagramPacket (sd, sd.length, ip, port); 
 
     // hard-coded to drop Frame 6 once    
	 if (seq == 6 && flag) {   
	 flag = false;    
	 System.out.println ("Forgot CONSIGNMENT " + seq);     
	 continue;    
	 } 
 
     ss.send (sp);     
	 System.out.println ("Sent CONSIGNMENT " + seq);      
	 rp = null;  
	 }   
	 }   
	 }     
	 catch (IOException ex)
	 {    System.out.println (ex.getMessage());  
	 }      finally {  
	 try {     if (fis != null)   
	 fis.close();   
	 }    catch (IOException ex) 
	 {     System.out.println(ex.getMessage());   
	 }   
	 }   
	 } 
 
 public static byte[] readyMsg (byte[] seq, byte[] payload) {          
 byte[] result = new byte [RDT.length + seq.length + payload.length + CRLF.length];    
 System.arraycopy (RDT, 0, result, 0, RDT.length);           
 System.arraycopy (seq, 0, result, RDT.length, seq.length); 
 System.arraycopy (payload, 0, result, RDT.length + seq.length, payload.length);        
 System.arraycopy (CRLF, 0, result, RDT.length + seq.length + payload.length, CRLF.length);      
 return result;    
 }         
 public static byte[] readyEndMsg (byte[] seq, byte[] payload)
 {          
 byte[] result = new byte [RDT.length + seq.length + payload.length + END.length + CRLF.length];          
 System.arraycopy (RDT, 0, result, 0, RDT.length);        
 System.arraycopy (seq, 0, result, RDT.length, seq.length);         
 System.arraycopy (payload, 0, result, RDT.length + seq.length, payload.length);       
 System.arraycopy (END, 0, result, RDT.length + seq.length + payload.length, END.length);        
 System.arraycopy (CRLF, 0, result, RDT.length + seq.length + payload.length + END.length, CRLF.length);
 return result;     
 } 
 } 