package Client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

import Data.Game;
import Data.Message;
import User.User;

public class GameClient extends Thread{
	
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	Scanner scan;
	
	int loginStatus = 0;
	
	public GameClient(String hostname, int port) {
		Socket s = null;
		try {
			System.out.println("Trying to connect to " + hostname + ": " + port);
			s = new Socket(hostname, port);
			System.out.println("Connected! ");
			
			//has to instantiate in the opposite order than in the server
			oos = new ObjectOutputStream(s.getOutputStream());
			ois = new ObjectInputStream(s.getInputStream());
			
			scan = new Scanner(System.in);
			
			String username = "";
			String password;
			
			while (true) {
				System.out.print("Username: ");
				username = scan.nextLine();
				System.out.println();
				System.out.print("Password: ");
				password = scan.nextLine();
				System.out.println();			
				
				User user = new User(username, password);
				oos.writeObject(user);
				oos.flush();
				
				

				Message message = null;
				try {
					message = (Message)ois.readObject();
				} catch (IOException ioe) {
					System.out.println("ioe: " + ioe.getMessage());
				} catch(ClassNotFoundException cnfe) {
					System.out.println("cnfe: " + cnfe.getMessage());
				}
				
				if (message.getMessage().equals("create account")) {
					System.out.println("No account exists with those credentials.");
					String response = "";
					while (response == "" && !response.equals("Yes") && !response.equals("No")) {
						System.out.print("Would you like to create a new account?");
						response = scan.nextLine();
					}
					if (response.equals("Yes")) {
						String response2 = "";
						while(response2 == "" && !response2.equals("Yes") && !response2.equals("No")) {
							System.out.println("Would you like to use the username and password above? ");
							response2 = scan.nextLine();
						}
						if (response2.equals("Yes")) {
							Message messageFromClient = new Message("create account");
							oos.writeObject(messageFromClient);
							oos.flush();
							break;
						}
					}
					else if (response.equals("No")) {
						//Terminate
						System.out.println("Terminated");
					}
					
					
				}
				else if (message.getMessage().equals("incorrect password")) {
					System.out.println("Wrong password, cannot sign in.");
				}
				else if (message.getMessage().equals("correct password")) {
					//loginStatus = 1;
					Message messageFromClient = new Message("logged in");
					oos.writeObject(messageFromClient);
					oos.flush();
					break;
				}
			}	
			
			System.out.println("Great! You are now logged in as " + username + "\n");
			System.out.println(username + "'s Record");
			System.out.println("----------------------");
			Vector<String> userInfo = (Vector<String>)ois.readObject();
			System.out.println("Wins -- " + userInfo.get(2));
			System.out.println("Losses -- " + userInfo.get(3) + "\n");
				
			String startOrJoin = "";
			while (startOrJoin == "" && !startOrJoin.equals("1") && !startOrJoin.equals("2")) {
				System.out.println("    1) Start a game");
				System.out.println("    2) Join a game");
				System.out.println("Would you like to start a game or join a game? \n");
				startOrJoin = scan.nextLine();
			}
			
			String nameOfGame = "";
			int numPlayers = 0;
			Vector<String> gameInfoFromClient = new Vector<String>();
			if (startOrJoin.equals("1")) {
				//user wants to start a game
				System.out.println("What is the name of the game? ");
				nameOfGame = scan.nextLine();
				while (numPlayers < 1 || numPlayers > 4) {
					System.out.println("How many users will be playing? (1-4) \n");
					numPlayers = scan.nextInt();
				}
				
				
				gameInfoFromClient.add(startOrJoin);
				gameInfoFromClient.add(nameOfGame);
				//gameInfoFromClient.add(numPlayers);
				
			}
			else if (startOrJoin.equals("2")) {
				//user wants to join a game
				System.out.println("What is the name of the game? ");
				nameOfGame = scan.nextLine();
				gameInfoFromClient.add(startOrJoin);
				gameInfoFromClient.add(nameOfGame);
				
			}
			oos.writeObject(gameInfoFromClient);
			oos.flush();
			oos.writeInt(numPlayers);
			oos.flush();
			
			while (true) {
				String startStatus = (String)ois.readObject();
				if (startStatus.equals("start game")) {
					break;
				}
			}
			
			//after verifying the number of users
			System.out.println("All users have joined. \n\nDetermining secret word... \n");
			
			String secretWord = (String)ois.readObject();
			//System.out.println("secret word is " + secretWord);
			String initialMask = (String)ois.readObject();
			System.out.println("Secret Word " + initialMask + "\n");
			String gameOver = "false";
			String win = "false";
			
			//receiving a game object
			Game game = (Game)ois.readObject();
			
			int remainingGuesses = 7;
			
			
			//???confused about the order that these codes happen (codes in the while true loop)
			while (true) {
				String gameStatus = (String)ois.readObject();
				if (gameStatus.equals("game ends")) {
					break;
				}
				else if (gameStatus.equals("game in progress")) {
					//continues to execute the following code
				}
//				while (true) { //wait if it is not your turn
//					String yourTurn = (String)ois.readObject();
//					if (yourTurn.equals("true")) break;
//				}
				String yourTurn = (String)ois.readObject();
				
//				//receiving remaining guesses or the game object again
//				game = (Game)ois.readObject();
//				remainingGuesses = game.remainingGuesses;
//				
				if (yourTurn.equals("true")) {
					//reads in the remainingGuesses
					String remainingGuessesExistStatus = (String)ois.readObject();
					if (remainingGuessesExistStatus.equals("remainingGuessesNotExist")) {
						break;
					}
					else {
						
					}
					//execute the following code when it is your turn
					System.out.println("You have " + remainingGuesses + " incorrect guesses remaining. ");
					System.out.println("    (1) Guess a letter."
							+ "\n    (2) Guess the word. \n");
					Vector<String> guessInfoFromClient = new Vector<String>();
					String letterOrWord = "";
					while(letterOrWord == "" || (!letterOrWord.equals("1") && !letterOrWord.equals("2"))) {
						System.out.println("What would you like to do? ");
						letterOrWord = scan.next();
					}
					guessInfoFromClient.add(letterOrWord);
					String letterGuessed;
					String wordGuessed;
					if (letterOrWord.equals("1")) {
						System.out.println("Letter to guess - ");
						letterGuessed = scan.next(); 
						guessInfoFromClient.add(letterGuessed);
						if (game.secretWord.contains(letterGuessed)) {
							System.out.println("The letter '" + letterGuessed + "' is in the secret word.");
						}
						else {
							System.out.println("The letter '" + letterGuessed + "' is not in the secret word.");
						}
					}
					else if (letterOrWord.equals("2")) {
						System.out.println("What is the secret word? ");
						wordGuessed = scan.next();
						guessInfoFromClient.add(wordGuessed);
					}
					
					oos.writeObject(guessInfoFromClient);
					oos.flush();
					
					//receiving remaining guesses
					remainingGuesses = (int)ois.readObject();
					
					
					Vector<String> currentMaskAndGameStatus = (Vector<String>)ois.readObject();
					String currentMask = currentMaskAndGameStatus.get(0);
					System.out.println("Secret Word: " + currentMask + "\n");
					
					gameOver = currentMaskAndGameStatus.get(1);
					
					if (gameOver.equals("true")) {
						break;
					}
				}
				else if (yourTurn.equals("false")) {
					//do nothing for this loop
					//actually no, have to be receiving broadcast message
					/*
					String broadcastMsg = (String)ois.readObject();
					System.out.println("Secret Word: " + broadcastMsg);
					*/
					
				}			
				
			}
			
			
			String[] gameResult = (String[])ois.readObject();
			gameOver = gameResult[0];
			win = gameResult[1];
			
			if (win.equals("true")) {
				System.out.println("That is correct! You win! \n");
				
			}
			else {
				System.out.println("You lose. \n");
			}
			
			System.out.println("Secret word was " + secretWord + "\n");
			
			//Get the user's record from input stream
			//int[] userRecord = (int[])ois.readObject();
			System.out.println(username + "'s Record");
			System.out.println("----------------------");
			userInfo = (Vector<String>)ois.readObject();
			System.out.println("Wins -- " + userInfo.get(2));
			System.out.println("Losses -- " + userInfo.get(3));
			
			this.start();
			
			
			
			
		}catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		}
		finally {
			try {
				if (s != null) {
					s.close();
				}
			}catch(IOException ioe) {
				System.out.println("ioe: " + ioe.getMessage());
			}
		}
	}
	
	//have to overwrite run() function
	public void run() {
		try {
			while(true) {
				String broadcastMsg = (String)ois.readObject();
				System.out.println("Secret Word: " + broadcastMsg);
			}
			
			
		}catch(IOException ioe){
			System.out.println("ioe: " + ioe.getMessage());
		}catch(ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		}

		
	}
	
	public static void main(String[] args) {
		String config_file = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		PrintWriter pw = new PrintWriter(System.out);
		Properties prop = new Properties();
		
		while(true) {
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
		
		
		String serverHostname = prop.getProperty("ServerHostname");
		String serverPort = prop.getProperty("ServerPort");
		String DBConnection = prop.getProperty("DBConnection");
		String DBUsername = prop.getProperty("DBUsername");
		String DBPassword = prop.getProperty("DBPassword");
		String secretWordFile = prop.getProperty("SecretWordFile");
		
		//Print out the parameters on the command line
		System.out.println("Server Hostname - " + serverHostname);
		System.out.println("Server Port - " + serverPort);
		System.out.println("Database Connection String - " + DBConnection);
		System.out.println("Database Username - " + DBUsername);
		System.out.println("Database Password - " + DBPassword);
		System.out.println("Secret Word File - " + secretWordFile);
		
		//Socket ???
		
		
		//Connect to Server Port
		int port = Integer.parseInt(serverPort);
		new GameClient(serverHostname, port);
		
		
		//***LOG IN FUNCTIONALITY NOT IMPLEMENTED
		
		
	}
}
