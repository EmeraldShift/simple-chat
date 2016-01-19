package com.lithia.net.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

public class ReaderUtil
{
	
	private static HashMap<Socket, BufferedReader> readers = new HashMap<>();
	public static boolean debug = true;
	
	private static String prefix = "Reader";
	
	public static String read(Socket s) throws IOException
	{
		
		if(!readers.containsKey(s))
		{
			readers.put(s, new BufferedReader(new InputStreamReader(s.getInputStream())));
		}
		
		String line = null;
		
		if(readers.get(s).ready())
		{
			line = readers.get(s).readLine();
			if(debug) Logger.log(prefix, "Grabbed [" + line + "] from reader.");
		}
		
		return line == null ? "" : line;
	}
	
}
