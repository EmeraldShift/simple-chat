package com.lithia.net.client;

import static com.lithia.net.util.ReaderUtil.read;
import static com.lithia.net.util.WriterUtil.write;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.lithia.net.util.ReaderUtil;
import com.lithia.net.util.WriterUtil;

public class Client extends JFrame implements ActionListener
{

	private static final long serialVersionUID = 1L;

	private String screenName = "LiTHiA Chat Client";

	private JTextArea chatRoom = new JTextArea(30, 64);
	private JTextField chatBar = new JTextField(64);

	static Socket socket;
	private int id;
	private String name;
	private Thread chatThread;

	private static String prefix = "Client";
	
	public Client()
	{
		chatRoom.setEditable(false);
		chatRoom.setBackground(Color.WHITE);
		chatRoom.setLineWrap(true);
		chatRoom.setWrapStyleWord(true);
		chatBar.addActionListener(this);
		
		Container content = getContentPane();
		content.add(new JScrollPane(chatRoom), BorderLayout.CENTER);
		content.add(chatBar, BorderLayout.SOUTH);
		
		setTitle(screenName);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		chatBar.requestFocusInWindow();
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			write(socket, name + "|" + id + ":chat:" + chatBar.getText());
		}
		catch (IOException ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		chatBar.setText("");
		chatBar.requestFocusInWindow();
	}
	
	private void start(String ip, int port)
	{
		log(prefix, "Creating client...");

		try
		{
			if(ip == null) ip = JOptionPane.showInputDialog("Enter IP:");
			if(port == 0) port = Integer.parseInt(JOptionPane.showInputDialog("Enter Port:"));
			log(prefix, "Connecting to " + ip + " on port " + port + ".");
			socket = new Socket(ip, port);
			log(prefix, "Socket created, connection complete.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			log(prefix, "Listening for client assignment from server...");

			boolean active = true;
			long pingTime = System.currentTimeMillis();
			long pongTime = System.currentTimeMillis();

			while (active)
			{
				String line = read(socket);
				
				if (line.contains("chpr:"))
				{
					log(prefix, "Received client assignment from server!");
					id = Integer.parseInt(line.substring(5));
					log(prefix, "Joined server with id " + id + ".");

					name = JOptionPane.showInputDialog("Desired client name:");
					if (name.length() == 0) name = "Anonymous-" + id;

					log(prefix, "Set name to " + name + ".");

					write(socket, name);
				}

				if (line.contains("chat:"))
				{
					line = line.substring(5);
					String name = line.substring(0, line.indexOf(':'));
					String msg = line.substring(line.indexOf(':') + 1);

					log("Chat", name + " > " + msg);
				}

				if (line.contains("cnct:"))
				{
					String name = line.substring(5);
					log("Chat", name + " connected.");
				}

				if (line.contains("chdc:"))
				{
					String name = line.substring(5);
					log("Chat", name + " disconnected.");
				}

				if (line.equals("abort"))
				{
					log(prefix, "Terminating at request of server...");
					write(socket, "abort " + name + "|" + id);

					active = false;
				}

				if (System.currentTimeMillis() - pingTime > 5000)
				{
					write(socket, "ping");
					pingTime = System.currentTimeMillis();
				}

				if (line.equals("pong"))
				{
					pongTime = System.currentTimeMillis();
				}

				if (System.currentTimeMillis() - pongTime > 15000)
				{
					log(prefix, "Server is not responding...");
					log(prefix, "Client will now exit.");

					active = false;
				}

				if (line.equals("quit"))
				{
					active = false;
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
				if (socket != null) socket.close();
				System.in.close();
				chatThread.join();
			}
			catch (IOException | InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		System.exit(0);
	}
	
	public static void main(String[] args)
	{
		Client client = new Client();
		
		ReaderUtil.debug = Boolean.parseBoolean(args[0]);
		WriterUtil.debug = Boolean.parseBoolean(args[0]);

		String ip = null;
		int port = 0;

		try
		{
			ip = args[1];
			port = Integer.parseInt(args[2]);
		}
		catch (Exception e)
		{
		}
		
		client.start(ip, port);
	}
	
	private void log(String prefix, String msg)
	{
		chatRoom.setFont(new Font("Arial", Font.PLAIN, 16));
		chatRoom.insert("[" + prefix + "] " + msg + "\n", chatRoom.getText().length());
		chatRoom.setCaretPosition(chatRoom.getText().length());
	}
}
