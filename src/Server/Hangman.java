package Server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import User.User;
import Data.Game;

public class Hangman {
	//private Vector<ServerThread> serverThreads;
	Vector<ServerThread> serverThreads;
	
	static String serverHostname;
	static String serverPort;
	static String DBConnection;
	static String DBUsername;
	static String DBPassword;
	static String secretWordFile;
	
	Map<String, Game > games = new HashMap<String, Game>();
	Map<String, Integer> gamesToNumPlayers = new HashMap<String, Integer>();
	//int remainingGuesses = 7;
	//Vector<String> users = new Vector<String>();
	
	
	//Constructor 
	public Hangman(int port) {
		ServerSocket ss = null;
		try {
			System.out.println("Tyring to bind to port " + port);
			ss = new ServerSocket(port);
			System.out.println("Bound to port " + port);
			serverThreads = new Vector<ServerThread>();
			while(true) {
				Socket s = ss.accept();
				System.out.println("Connection from " + s.getInetAddress());
				//now need thread to listen lines coming in from clients while at the same time connect new clients
				ServerThread st = new ServerThread(s, this);
				serverThreads.add(st);
				
			}
		}catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}finally {
			try {
				if (ss != null) {
					ss.close();
				}
			}catch(IOException ioe) {
				System.out.println(ioe.getMessage());
			}
		}
	}
	
	public void createUserAccount(User user) {
		String username = user.getUsername();
		String password = user.getPassword();
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false");
			ps = conn.prepareStatement("INSERT INTO user(username, password, wins, losses) VALUES (?,?,?,?)");
			ps.setString(1, username);
			ps.setString(2, password);
			ps.setInt(3, 0);
			ps.setInt(4, 0);
			ps.executeUpdate();
		} catch (SQLException sqle) {
		System.out.println (sqle.getMessage());
		} //catch (ClassNotFoundException cnfe) {
		//System.out.println (cnfe.getMessage());
		//} 
		finally {
			 try{
				 if(conn != null)
					 conn.close();
			 }catch(SQLException ex){
				 System.out.println(ex.getMessage());
			 }
		}
	}
	
	public Vector<String> getUserInfo(User user) {
		Vector<String> userInfo = new Vector<String>();
		String username = user.getUsername();
		String password = user.getPassword();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			//Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false");
			String sql = "SELECT * FROM user WHERE username = '" + username + "' ";
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			int resultCount = 0;
			while (rs.next()) {
				userInfo.add(rs.getString("username"));
				userInfo.add(rs.getString("password"));
				userInfo.add(rs.getString("wins"));
				userInfo.add(rs.getString("losses"));
				
			}
		}catch (SQLException sqle) {
		System.out.println (sqle.getMessage());
		} 
		return userInfo;
	}
	
	public void startAGame(String nameOfGame, String username, int numPlayers, ServerThread st) {
		Vector<String> players = new Vector<String>();
		players.add(username);
		String secretWord = getSecretWord();
		String currentMask = "";
		for (int i = 0; i < secretWord.length(); i++) {
			currentMask += "_";
		}
		boolean gameAlreadyExists = false;
		if (games.containsKey(nameOfGame)) {
			//game exists, cannot create
			gameAlreadyExists = true;
			//server output
			System.out.println(username + " - " + nameOfGame + " already exists, so unable to start " + nameOfGame);
		}			
		
		if (!gameAlreadyExists) {
			Game game = new Game(nameOfGame, numPlayers, players, secretWord, currentMask, false, 7);
			game.addPlayerToST(st);
			games.put(nameOfGame, game);
			System.out.println(username + " - successfully started game " + nameOfGame);
			//gamesToNumPlayers.put(nameOfGame, numPlayers);
		}
		
		
	}
	
	public void joinAGame(String nameOfGame, String username, ServerThread st) {
		games.get(nameOfGame).addAPlayer(username);
		games.get(nameOfGame).addPlayerToST(st);
		//System.out.println("the size of players is " + games.get(nameOfGame).getPlayers().size());

	}
	
	public String getSecretWord() {
		LineNumberReader lineNumReader = null;
		BufferedReader br = null;
		int numLines = 0;
		try {
			lineNumReader  = new LineNumberReader(new FileReader(secretWordFile));
			br = new BufferedReader(new FileReader(secretWordFile));
			while ((lineNumReader.readLine()) != null);
			numLines = lineNumReader.getLineNumber();
		} catch (FileNotFoundException fnfe) {
			System.out.println("fnfe: " + fnfe.getMessage());
		} catch(IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
		
		
		Random rand = new Random();
		int randomLine = rand.nextInt(numLines - 1);
		
//		try {
//			br = new BufferedReader(new FileReader(secretWordFile));
//		} catch (FileNotFoundException fnfe) {
//			System.out.println("fnfe: " + fnfe.getMessage());
//		}
		
		String secretWord = null;
		try {
			for (int i = 0; i < randomLine; i++) {
				br.readLine();
			}
			secretWord = br.readLine();
		} catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
		
		return secretWord;
	}
	
	public boolean checkIfGameOver(String currentMask, String secretWord, String nameOfGame) {
		if (games.get(nameOfGame).currentMask.equals(games.get(nameOfGame).secretWord)) {
			return true;
		}
		if (games.get(nameOfGame).finished) 
			return true;
		else return false;
	}
	
	public boolean remainingGuessesExist(String nameOfGame) {
		if (games.get(nameOfGame).remainingGuesses > 0) {
			return true;
		}
		else return false;
		
	}
//	public void everyoneLoses(String nameOfGame) {
//		for (int i = 0; i < games.get(nameOfGame).st.size(); i++) {
//			games.get(nameOfGame).st.get(i).
//		}
//	}
	
	public void updateRecord(String win, User user) {
		String username = user.getUsername();
		String password = user.getPassword();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			conn = DriverManager.getConnection(DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false");
			String sql = "SELECT * FROM user WHERE username = '" + username + "' ";
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();

			int numWins = 0;
			int numLosses = 0;
			while (rs.next()) {
				numWins = rs.getInt("wins");
				numLosses = rs.getInt("Losses");
			}
			
			//System.out.println("numWins is now" + numWins);
			
			if (win.equals("true")) {
				
				numWins ++;
				//System.out.println("numWins is now" + numWins);
			}
			else 	numLosses ++;
			sql = "UPDATE user u SET u.wins=" + numWins + " WHERE u.username='" + username + "'";
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
			sql = "UPDATE user u SET u.losses=" + numLosses + " WHERE u.username='" + username + "'";
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
		}catch (SQLException sqle) {
		System.out.println ("sqle: " + sqle.getMessage());
		} 
	}
	
	public int[] getUserRecordFromDatabase(User user) {
		int[] userRecord = null;
		String username = user.getUsername();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false");
			String sql = "SELECT * FROM user WHERE username = '" + username + "' ";
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			int numWins;
			int numLosses;
			numWins = rs.getInt("wins");
			numLosses = rs.getInt("Losses");
			userRecord[0] = numWins;
			userRecord[1] = numLosses;
		}catch (SQLException sqle) {
		System.out.println (sqle.getMessage());
		} 	
		return userRecord;
	}
	
	public String createTheMask(String letterGuessed, String secretWord, String nameOfGame, String username) {
		String currentMask = games.get(nameOfGame).getCurrentMask();
		if (secretWord.contains(letterGuessed)){
			for (int i = 0; i < secretWord.length();i++) {
				if (secretWord.charAt(i) == letterGuessed.charAt(0) 
						&& secretWord.charAt(i) != currentMask.charAt(i)) {
					char[] currentMaskChars = currentMask.toCharArray();
					currentMaskChars[i] = secretWord.charAt(i);
					currentMask = String.valueOf(currentMaskChars);
				}
			}
			//server output
			System.out.println(nameOfGame + " " + username + " - " + letterGuessed + " is in " + secretWord + ". Secret word now shows " + currentMask);
			games.get(nameOfGame).setCurrentMask(currentMask);
			return currentMask;
		}
		else {
			//server output
			System.out.println(nameOfGame + " " + username + " - " + letterGuessed + " is not in " + secretWord
					+ ". " + nameOfGame + " now has " + games.get(nameOfGame).remainingGuesses + " guesses remaining. ");
			return currentMask;
		}
	}
	
	public void broadcast(ServerThread currentST, String nameOfGame) {
		for (ServerThread st : games.get(nameOfGame).st) {
			if (st != currentST) {
				String currentMask = games.get(nameOfGame).currentMask;
				st.sendBroadcastMsg(currentMask);
			}
		}
	}
	
	public static void main(String[] args) {
		String config_file = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter pw = new PrintWriter(System.out);
		Properties prop = new Properties();

		while (true) {
			System.out.print("What is the name of the configuration file? ");
			try {
				config_file = br.readLine();
					
			}catch(IOException ioe) {
				System.out.println("ioe: " + ioe.getMessage());
				System.out.println("Configuration file " + config_file + " could not be found.");
			}
			System.out.println("Reading config file...");
			
			//Next, parse the config file
			try {
				prop.load(new FileInputStream(config_file));
				break;
			} catch (FileNotFoundException fnfe) {
				System.out.println("fnfe: " + fnfe.getMessage());
				System.out.println("Configuration file " + config_file + " could not be found.");
			} catch (IOException ioe) {
				System.out.println("ioe: " + ioe.getMessage());
			}
		}
		
		
		serverHostname = prop.getProperty("ServerHostname");
		serverPort = prop.getProperty("ServerPort");
		DBConnection = prop.getProperty("DBConnection");
		DBUsername = prop.getProperty("DBUsername");
		DBPassword = prop.getProperty("DBPassword");
		secretWordFile = prop.getProperty("SecretWordFile");
		
		//Print out the parameters on the command line
		System.out.println("Server Hostname - " + serverHostname);
		System.out.println("Server Port - " + serverPort);
		System.out.println("Database Connection String - " + DBConnection);
		System.out.println("Database Username - " + DBUsername);
		System.out.println("Database Password - " + DBPassword);
		System.out.println("Secret Word File - " + secretWordFile);
		
		//Make the database connection & bind to server port
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			//Class.forName("com.mysql.jdbc.Driver");
			//String sqlConnectionString = DBConnection + ""
			//prop.setProperty("useSSL", "false");
			//conn = DriverManager.getConnection(DBConnection, prop);
			conn = DriverManager.getConnection(DBConnection + "?user=" + DBUsername + "&password=" + DBPassword + "&useSSL=false");
			//conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/hangman?user=root&password=12345678Abc&useSSL=false");
//			ps = conn.prepareStatement("INSERT INTO test(username) VALUES ('hiro')");
//			ps.executeUpdate();
		} catch (SQLException sqle) {
		System.out.println (sqle.getMessage());
		} //catch (ClassNotFoundException cnfe) {
		//System.out.println (cnfe.getMessage());
		//} 
		finally {
			 try{
				 if(conn != null)
					 conn.close();
			 }catch(SQLException ex){
				 System.out.println(ex.getMessage());
			 }
		}
		
		int port = Integer.parseInt(serverPort);
		new Hangman(port);
		
		//**Next Step...
		
		
		
		
	}
}
