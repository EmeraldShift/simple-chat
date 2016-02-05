package com.lithia.net.server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JOptionPane;

import com.lithia.net.server.ClientHandler.Client;
import com.lithia.net.util.Logger;
import com.lithia.net.util.ReaderUtil;
import com.lithia.net.util.WriterUtil;

public class Server
{

	ServerSocket socket;
	int port;
	
	private static String prefix = "Server";
	private static Scanner scan = new Scanner(System.in);
	private static Thread commandThread;

	public Server(int port) throws IOException
	{
		try
		{
			Logger.log(prefix, "Creating socket...");
			socket = new ServerSocket(port);
			Logger.log(prefix, "...complete!");
			
			Runnable r = () ->
			{
				while(true)
				{
					try
					{
						String chat;
						while((chat = scan.nextLine()) != null)
						{
							ClientHandler.getInstance().processCommand(chat, null);
						}
					}
					catch(Exception e)
					{
					}
				}
			};
			
			commandThread = new Thread(r, "Command");
			commandThread.start();
		}
		catch(BindException e)
		{
			Logger.log(prefix, "Could not bind to port " + port + ", trying port " + (port + 1) + ".");
			new Server(port + 1).start();
		}

		this.port = port;
	}

	public void start() throws IOException
	{
		Logger.log(prefix, "Listening for clients.");
		while (!socket.isClosed())
		{
			processClient(socket.accept());
		}
	}

	private void processClient(Socket socket)
	{
		Logger.log(prefix, "Connection attempt found, processing...");
		int id;
		final Client client = ClientHandler.getInstance()
				.addClient(id = ClientHandler.Client.generateNewClientID(), socket);
		
		Logger.log(prefix, "Creating client with id " + id);
		new Thread(() -> client.run()).start();
	}

	public static void main(String[] args)
	{
		ReaderUtil.debug = Boolean.parseBoolean(args[0]);
		WriterUtil.debug = Boolean.parseBoolean(args[0]);
		int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port:"));
		
		Logger.log(prefix, "Creating server on port " + port);

		try
		{
			Server server = new Server(port);
			server.start();
		}
		catch (IOException e)
		{
			System.err.println("Failed to start server: " + e.getMessage());
			System.err.println("Server will now shut down.");
			System.exit(0);
		}
	}

}
