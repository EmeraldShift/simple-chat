package com.lithia.net.server;

import static com.lithia.net.util.ReaderUtil.read;
import static com.lithia.net.util.WriterUtil.write;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import com.lithia.net.util.Logger;
import com.lithia.net.util.WriterUtil;

public class ClientHandler
{

	private static ClientHandler instance;
	private Vector<Client> clients;

	private static String prefix = "Client Handler";

	private ClientHandler()
	{
		clients = new Vector<>();
	}

	public static ClientHandler getInstance()
	{
		if (instance == null) instance = new ClientHandler();

		return instance;
	}

	public Client addClient(int id, Socket socket)
	{
		Client client = new Client(socket, id);
		client.handler = this;

		clients.add(client);
		return client;
	}

	public void processCommand(String command)
	{
		if (command.contains("kick"))
		{
			String name = command.split(" ")[1];

			for (Client c : clients)
			{
				if (c.name.equalsIgnoreCase(name))
				{
					c.active = false;

					try
					{
						write(c.socket, "quit");
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		else if (command.contains("list"))
		{
			Logger.log("Chat", "There are " + clients.size() + " clients.");
			
			int conn = 0;
			for (Client c : clients)
			{
				if(c.name != null)
				{
					Logger.log("Client", c.name);
				}
				else conn++;
			}

			Logger.log("Client", "+ " + conn + " connecting.");
		}
	}

	public static class Client implements Runnable
	{
		private static int index;

		private String name;
		private Socket socket;
		private int id;
		private volatile boolean active;
		private ClientHandler handler;

		BufferedReader in;
		PrintWriter out;

		public Client(Socket socket, int id)
		{
			Logger.log(prefix, "Generating client token...");
			this.socket = socket;
			this.id = id;

			Logger.log(prefix, "...complete!");
		}

		public void run()
		{
			try
			{
				ClientHandler.getInstance().handshake(this);

				if (active)
				{
					for (Client c : handler.clients)
					{
						if (WriterUtil.debug) Logger.log(prefix, "Writing to " + c.name + "'s socket.");
						write(c.socket, "cnct:" + name);
					}
				}

			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			Logger.log(prefix, "Client initialized.");

			if (active) try
			{
				Logger.log(prefix, "Listening on client socket...");

				long chatTime = 0;
				String lastChat = "";
				
				while (active)
				{
					String line = read(socket);

					if (line.contains("name:"))
					{
						this.name = line.substring(5);
					}

					if (line.contains("abort"))
					{
						Logger.log(prefix, "Client " + line.substring(6) + " disconnected.");
						socket.close();
					}

					if (line.equals("ping"))
					{
						write(socket, "pong");
						if (WriterUtil.debug) Logger.log("Ping", "Received ping from " + name + ".");
					}

					if (line.contains(":chat:"))
					{
						String name = line.substring(0, line.indexOf('|'));
						String msg = line.substring(line.indexOf(':') + 6);
						msg = msg.trim();

						if (msg.length() > 100 || msg.length() == 0 || System.currentTimeMillis() - chatTime < 2000
								|| lastChat.equalsIgnoreCase(msg))
						{
							if (msg.length() > 100 || msg.length() == 0)
							{
								write(socket, "chat:Server:Your message must be between 1-100 characters.");
							}
							else if (lastChat.equalsIgnoreCase(msg))
							{
								write(socket, "chat:Server:You may not repeat the same message twice.");
							}
							else
							{
								write(socket, "chat:Server:You must wait 2 seconds between messages.");
							}

							continue;
						}

						chatTime = System.currentTimeMillis();
						lastChat = msg;

						if (msg.equals("quit"))
						{
							active = false;
							write(socket, "quit");
							continue;
						}

						for (Client c : handler.clients)
						{
							if (WriterUtil.debug) Logger.log(prefix, "Writing to " + c.name + "'s socket.");
							write(c.socket, "chat:" + name + ":" + msg);
						}
					}

					if (socket.isClosed() || !socket.isConnected() || !socket.isBound()) active = false;

					Thread.sleep(50);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					for (Client c : handler.clients)
					{
						write(c.socket, "chdc:" + name);
					}
					
					socket.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			
			Logger.log(prefix, "Client with id " + id + " terminated.");
			ClientHandler.getInstance().clients.remove(this);
		}

		public static int generateNewClientID()
		{
			return index++;
		}

		public void setHandler(ClientHandler handler)
		{
			this.handler = handler;
		}

		public String getName()
		{
			return name;
		}

		public int getID()
		{
			return id;
		}

	}

	private void handshake(Client c) throws InterruptedException, IOException
	{
		Socket s = c.socket;

		Logger.log(prefix, "Creating handshake: chpr:" + c.id);

		write(s, "chpr:" + c.id);

		String line;
		long timeout = System.currentTimeMillis();
		while ((line = read(s)).length() == 0 || line.equals("ping") || line.contains(":") || line.contains("|") || line.length() > 16 || match(line) || System.currentTimeMillis() - timeout > 15000)
		{
			if(line.contains(":") || line.contains("|"))
			{
				write(s, "chat:Server:Your name contains invalid characters.");
				write(s, "chat:Server:Please restart your client.");
				return;
			}
			if (line.length() > 16)
			{
				write(s, "chat:Server:Your name cannot exceed 16 characters.");
				write(s, "chat:Server:Please restart your client.");
				return;
			}
			
			if(match(line))
			{
				write(s, "chat:Server:Your name cannot match that of another user.");
				write(s, "chat:Server:Please restart your client.");
				return;
			}
			
			if(System.currentTimeMillis() - timeout > 15000)
			{
				write(s, "chat:Server:You took to long to log in.");
				write(s, "chat:Server:Please restart your client.");
				return;
			}
			
			Thread.sleep(1000);
		}
		

		c.name = line;
		c.active = true;

		Logger.log(prefix, "Received heartbeat, handshake complete.");
		Logger.log(prefix, line + " connected.");
	}
	
	private boolean match(String name)
	{
		for(Client c : clients)
		{
			if(c.name == null) continue;
			if(c.name.equalsIgnoreCase(name)) return true;
		}
		
		return false;
	}

}
