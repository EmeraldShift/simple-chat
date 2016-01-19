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
		if(debug) Logger.log(prefix, "Fetching writer...");
		if(!writers.containsKey(s))
		{
			if(debug) Logger.log(prefix, "No writer found, creating one.");
			writers.put(s, new PrintWriter(s.getOutputStream()));
		}
		if(debug) Logger.log(prefix, "Writer found, writing [" + o + "] to socket.");
		
		writers.get(s).println(o);
		if(debug) Logger.log(prefix, "...complete!");
		writers.get(s).flush();
		if(debug) Logger.log(prefix, "Writer flushed, write complete.");
	}
	
}
