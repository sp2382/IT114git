package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
//import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
//import javax.swing.SwingConstants;

public class ClientUI extends JFrame implements Event {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	CardLayout card;
	ClientUI self;
	JPanel textArea;
	JPanel userPanel;
	List<User> users = new ArrayList<User>();
	private final static Logger log = Logger.getLogger(ClientUI.class.getName());
	Dimension windowSize = new Dimension(450, 500);

	public ClientUI(String title) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(windowSize);
		setLocationRelativeTo(null);
		self = this;
		setTitle(title);
		card = new CardLayout();
		setLayout(card);
		createConnectionScreen();
		createUserInputScreen();
		createPanelRoom();
		createPanelUserList();
		showUI();
	}

	void createConnectionScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel hostLabel = new JLabel("Host:");
		JTextField host = new JTextField("127.0.0.1");
		panel.add(hostLabel);
		panel.add(host);
		JLabel portLabel = new JLabel("Port:");
		JTextField port = new JTextField("3000");
		panel.add(portLabel);
		panel.add(port);
		JButton button = new JButton("Next");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String _host = host.getText();
				String _port = port.getText();
				if (_host.length() > 0 && _port.length() > 0) {
					try {
						connect(_host, _port);
						self.next();
					} catch (IOException e1) {
						e1.printStackTrace();
						// TODO handle error properly
						log.log(Level.SEVERE, "Error connecting");
					}
				}
			}

		});
		panel.add(button);
		this.add(panel);
	}

	void createUserInputScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel userLabel = new JLabel("Username:");
		JTextField username = new JTextField();
		panel.add(userLabel);
		panel.add(username);
		JButton button = new JButton("Join");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String name = username.getText();
				if (name != null && name.length() > 0) {
					SocketClient.setUsername(name);
					self.next();
				}
			}

		});
		panel.add(button);
		this.add(panel);
	}

	void createPanelRoom() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		textArea = new JPanel();
		textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
		textArea.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scroll, BorderLayout.CENTER);

		JPanel input = new JPanel();
		input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
		JTextField text = new JTextField();
		input.add(text);
		JButton button = new JButton("Send");
		text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendAction");
		text.getActionMap().put("sendAction", new AbstractAction() {

			public void actionPerformed(ActionEvent actionEvent) {

				button.doClick();
			}
		});

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (text.getText().length() > 0) {
					SocketClient.sendMessage(text.getText());
					text.setText("");
				}
			}

		});
		input.add(button);
		panel.add(input, BorderLayout.SOUTH);
		this.add(panel);

		JButton xprtButton = new JButton("Chat Archive");
		xprtButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				chatLog();
			}
		});
		input.add(xprtButton);

		panel.add(input, BorderLayout.SOUTH);
		this.add(panel);
	}

	void createPanelUserList() {
		userPanel = new JPanel();
		userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
		userPanel.setAlignmentY(Component.TOP_ALIGNMENT);

		JScrollPane scroll = new JScrollPane(userPanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		Dimension d = new Dimension(100, windowSize.height);
		scroll.setPreferredSize(d);

		textArea.getParent().getParent().getParent().add(scroll, BorderLayout.WEST);
	}

	void chatLog() {
		StringBuilder sb = new StringBuilder();
		Component[] components = textArea.getComponents();
		for (Component comp : components) {
			JEditorPane jedit = (JEditorPane) comp;
			if (jedit != null) {
				sb.append(jedit.getText() + System.lineSeparator());
			}
		}
		String chatlog = sb.toString();
		try {
			File chatLog = new File("ChatLog.txt");
			FileWriter writer = new FileWriter("ChatLog.txt");
			writer.write(chatlog);
			writer.close();

		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	void addClient(String name) {
		User u = new User(name);
		Dimension p = new Dimension(userPanel.getSize().width, 35);
		u.setPreferredSize(p);
		u.setMinimumSize(p);
		u.setMaximumSize(p);
		userPanel.add(u);
		users.add(u);
		pack();
	}

	void removeClient(User client) {
		userPanel.remove(client);
		client.removeAll();
		userPanel.revalidate();
		userPanel.repaint();
	}

	/***
	 * Attempts to calculate the necessary dimensions for a potentially wrapped
	 * string of text. This isn't perfect and some extra whitespace above or below
	 * the text may occur
	 * 
	 * @param str
	 * @return
	 */
	int calcHeightForText(String str) {
		FontMetrics metrics = self.getGraphics().getFontMetrics(self.getFont());
		int hgt = metrics.getHeight();
		int adv = metrics.stringWidth(str);
		final int PIXEL_PADDING = 7;
		Dimension size = new Dimension(adv, hgt + PIXEL_PADDING);
		final float PADDING_PERCENT = 1.3f;
		// calculate modifier to line wrapping so we can display the wrapped message
		int mult = (int) Math.floor(size.width / (textArea.getSize().width * PADDING_PERCENT));
		mult++;
		return size.height * mult;
	}

	void addMessage(String str) {
		JEditorPane entry = new JEditorPane("text/html", "");
		entry.setEditable(false);

		entry.setText(str);

		Dimension d = new Dimension(textArea.getSize().width, calcHeightForText(str));
		// attempt to lock all dimensions
		entry.setMinimumSize(d);
		entry.setPreferredSize(d);
		entry.setMaximumSize(d);
		// entry.add(s);
		textArea.add(entry);

		pack();
		System.out.println(entry.getSize());
		JScrollBar sb = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}

	void next() {
		card.next(this.getContentPane());
	}

	void previous() {
		card.previous(this.getContentPane());
	}

	void connect(String host, String port) throws IOException {
		SocketClient.callbackListener(this);
		SocketClient.connectAndStart(host, port);
	}

	void showUI() {
		pack();
		Dimension lock = textArea.getSize();
		textArea.setMaximumSize(lock);
		lock = userPanel.getSize();
		userPanel.setMaximumSize(lock);
		setVisible(true);
	}

	@Override
	public void onClientConnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		addClient(clientName);
		if (message != null && !message.isBlank()) {
			self.addMessage(String.format("<b>%s:</b> %s", clientName, message));
		}
	}

	@Override
	public void onClientDisconnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			if (u.getName() == clientName) {
				removeClient(u);
				iter.remove();
				self.addMessage(String.format("<i>%s:</i> %s", clientName, message));
				break;
			}

		}
	}

	@Override
	public void onMessageReceive(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		self.addMessage(String.format("<i>%s:</i> %s", clientName, message));
	}

	@Override
	public void onChangeRoom() {
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			removeClient(u);
			iter.remove();
		}
	}

	public static void main(String[] args) {
		ClientUI ui = new ClientUI("ChatApp");
		if (ui != null) {
			log.log(Level.FINE, "Started");
		}
	}
}