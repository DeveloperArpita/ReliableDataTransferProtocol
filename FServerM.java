import java.net.*;
import java.io.*;
import java.util.*;



public class FServerM {
	public static void main (String[] args) {

		final String transferStart = new String ("REQUEST");
		final int maxFrames = 3;

		DatagramSocket ss;
		DatagramPacket rp;
		byte[] rd;
		String strRequest;
		byte b;
		int i, count = 0, dropFrames[] = new int [maxFrames];

		for (i = 0; i < args.length - 2 && i < maxFrames; i++)
			dropFrames[i] = Integer.parseInt (args[2 + i]);
		for (;i < maxFrames; i++) 
			dropFrames[i] = -1;

		try {
			ss = new DatagramSocket (Integer.parseInt(args[1]));
			System.out.print ("\nSERVER IS UP...");

			while (true) {
				rd = new byte [100];
				rp = new DatagramPacket (rd, rd.length);
				ss.receive (rp);
				strRequest = new String (rd);				
				if (strRequest.startsWith (transferStart)) {
					FServerMWorker worker = new FServerMWorker (rp.getAddress(), rp.getPort(), new String (args[0] + "\\\\" + strRequest.substring (transferStart.length(), strRequest.indexOf ("\r\n"))), dropFrames, count++);
					worker.start();
				}
			}
		}
		
		catch (IOException ex) {
			System.out.println (ex.getMessage());
		}

	}
}