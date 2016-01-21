package com.lithia.net.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class WriterUtil
{
	
	private static HashMap<Socket, PrintWriter> writers = new HashMap<>();
	public static boolean debug = true;
	private static String prefix = "Writer";
	
	public static void write(Socket s, String o) throws IOException
	{
		if(!writers.containsKey(s))
		{
			writers.put(s, new PrintWriter(s.getOutputStream()));
		}
		
		if(debug) Logger.log(prefix, "Writing [" + o + "] to socket.");
		
		writers.get(s).println(o);
		writers.get(s).flush();
	}
	
}
