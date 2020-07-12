import java.net.*;
import java.io.*;
import java.util.*;



public class FServerMWorker extends Thread {

	public static final byte[] RDT = new String ("RDT").getBytes();
    	public static final byte[] END = new String ("END").getBytes();
    	public static final byte[] CRLF = new String ("\r\n").getBytes();

	InetAddress ip;
	String filename;
	int port, number, dropFrames[];

	public FServerMWorker (InetAddress ip, int port, String filename, int dropFrames[], int number) {
		this.ip = ip;
		this.port = port;
		this.filename = filename;
		this.dropFrames = dropFrames;
		this.number = number;
	}

	public void run() {

  		System.out.println ("\n\nS" + this.number + ": Received request for " + filename + " from " + ip + " port " + port);

		final int CONSIGNMENT = 512; // max length of payload
		final String crlf = new String (CRLF);
		
		DatagramSocket ss;
		DatagramPacket rp, sp;
		FileInputStream fis = null;
		byte[] rd, sd, payload, myLastData;
		int bytesRead = 0, seq = 0, i, index = 0;
		boolean end = false;
		byte b;

		try {
			ss = new DatagramSocket ();
			ss.setSoTimeout (30); // set timeout to 30 ms
			fis = new FileInputStream (filename);  // read file into buffer
			while (!end) {
		
				// prepare data
				payload = new byte [CONSIGNMENT];
				bytesRead = fis.read (payload);
				if (bytesRead > -1) {
					if (bytesRead < CONSIGNMENT) {  // last consignment
					// make a special byte array that exactly fits the number of bytes read otherwise the consignment may be padded with junk data
                        			myLastData = new byte [bytesRead];
                        			for (i = 0; i < bytesRead; i++)
                            				myLastData[i] = payload[i];
                        			sd = prepareLastMsg (new byte [] {(byte)seq}, myLastData);
                    			}
					else if (fis.available() == 0)  // last consignment
						sd = prepareLastMsg (new byte [] {(byte)seq}, payload);
		    			else // intermediate consignment
						sd = prepareMsg (new byte [] {(byte)seq}, payload);
					
					sp = new DatagramPacket (sd, sd.length, ip, port);
					
					// hard-coded to drop the specified Frames once
					if (index < dropFrames.length && dropFrames[index] != -1 && seq == dropFrames[index]) {
						index++;
						System.out.println ("S" + this.number + ": Forgot CONSIGNMENT " + seq);
					}
					else {
						ss.send (sp);
						System.out.println ("S" + this.number + ": Sent CONSIGNMENT " + seq);
					}

					rd = new byte [100];
					rp = new DatagramPacket (rd, rd.length);
					
					while (true) {
						try {
							ss.receive (rp);
							break;
						}
						catch (SocketTimeoutException ex) {
							if (sp != null) {
								System.out.println ("S" + this.number + ": Timeout");
								ss.send (sp);
								System.out.println ("S" + this.number + ": Sent CONSIGNMENT " + seq);
								continue;
							}
						}
					}

					seq =  (rd[RDT.length]);
					b=(byte)seq;
					System.out.println ("\nS" + this.number + ": Received ACK " + seq);
					if (seq == 0 && bytesRead < CONSIGNMENT) { // last consignment
						System.out.println ("S" + this.number + ": END");
						end = true;
						sp = null;
					}
				}
			}
		}
		
		catch (IOException ex) {
			System.out.println ("S" + this.number + ": " + ex.getMessage());
		}
		
		finally {
			try {
				if (fis != null)
					fis.close();
			}
			catch (IOException ex) {
				System.out.println ("S" + this.number + ": " + ex.getMessage());
			}
		}
		
	}

	public static byte[] prepareMsg (byte[] seq, byte[] payload) {
        	byte[] result = new byte [RDT.length + seq.length + payload.length + CRLF.length]; 
        	System.arraycopy (RDT, 0, result, 0, RDT.length); 
        	System.arraycopy (seq, 0, result, RDT.length, seq.length);
        	System.arraycopy (payload, 0, result, RDT.length + seq.length, payload.length);
        	System.arraycopy (CRLF, 0, result, RDT.length + seq.length + payload.length, CRLF.length);
        	return result;
    	}
    
    	public static byte[] prepareLastMsg (byte[] seq, byte[] payload) {
        	byte[] result = new byte [RDT.length + seq.length + payload.length + END.length + CRLF.length]; 
        	System.arraycopy (RDT, 0, result, 0, RDT.length); 
        	System.arraycopy (seq, 0, result, RDT.length, seq.length);
        	System.arraycopy (payload, 0, result, RDT.length + seq.length, payload.length);
        	System.arraycopy (END, 0, result, RDT.length + seq.length + payload.length, END.length);
        	System.arraycopy (CRLF, 0, result, RDT.length + seq.length + payload.length + END.length, CRLF.length);
		return result;
    	}
}