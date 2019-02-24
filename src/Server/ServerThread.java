package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

import Data.Message;
import User.User;

public class ServerThread extends Thread{

	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private Hangman hm;
	
	public ServerThread(Socket s, Hangman hm) {
		this.hm = hm;
		try {
			ois = new ObjectInputStream(s.getInputStream());
			oos = new ObjectOutputStream(s.getOutputStream());
			this.start();
			
			
			
		}catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}
	
	public void sendBroadcastMsg(String currentMask) {
		try {
			oos.writeObject(currentMask);
			oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
	}
	
	public void run() {
		try {
			User user;
			String username;
			String password;
			Vector<String> userInfo;
			while (true) {
				user = (User)ois.readObject();
				username = user.getUsername();
				password = user.getPassword();
				
				//server output
				System.out.println(username + " - trying to log in with password " + password);
				
				userInfo = hm.getUserInfo(user);
				if (userInfo.size() == 0) {
					//user does not exist
					//server output
					System.out.println(username + " - does not have an account so not successfully logged in. ");
					Message message = new Message("create account");
					oos.writeObject(message);
					oos.flush();
					break;
				}
				else {
					
					if (password.equals(userInfo.get(1))) {
						//password correct -- log in
						Message message = new Message("correct password");
						//server output 
						System.out.println(username + " - Successfully logged in. ");
						oos.writeObject(message);
						oos.flush();
						break;
					}
					else {
						//password incorrect
						Message message = new Message("incorrect password");
						//server output
						System.out.println(username + " - has an account but not successfully logged in. ");

						oos.writeObject(message);
						oos.flush();
					}
				}
			}
			
			
			Message messageFromClient = (Message)ois.readObject();
			if(messageFromClient.getMessage().equals("create account")) {
				
				hm.createUserAccount(user);
				//server output
				System.out.println(user.getUsername() + " - created an account with password " + user.getPassword());
			}
			
			userInfo = hm.getUserInfo(user);
			//server output
			System.out.println(username + " - has record " + userInfo.get(2) + " wins and " + userInfo.get(3) + "losses. ");
			oos.writeObject(userInfo);
			oos.flush();
			
			Vector<String> gameInfoFromClient = (Vector<String>)ois.readObject();
			String startOrJoin = gameInfoFromClient.get(0);
			String nameOfGame = gameInfoFromClient.get(1);
			
			int numPlayers = (int)ois.readInt();
			
			if (startOrJoin.equals("1")) {
				//server output 
				System.out.println(username + " - wants to start a game called " + nameOfGame);
				//start a game below
				hm.startAGame(nameOfGame, username, numPlayers, this);
				
			}
			else if (startOrJoin.equals("2")) {				
				if (hm.games.get(nameOfGame) != null){
					//let the user join a game below
					numPlayers = hm.games.get(nameOfGame).getNumPlayers();
					//server output
					System.out.println(username + " - wants to join a game called " + nameOfGame);
					
					if (hm.games.get(nameOfGame).getPlayers().size() < numPlayers) {
						hm.joinAGame(nameOfGame, username, this);
						//server output 
						System.out.println(username + " - successfully joined game " + nameOfGame);
						
					}
					
					
				}
				else if (hm.games.get(nameOfGame) == null) {
					System.out.println("Game does not exist.");
				}
				else {
					//server output
					System.out.println(username + " - " + nameOfGame + " exists, but " + username + " unable to join because maximum number of players have already joined " + nameOfGame);
				}
			}
			
			//m instanceof loginMessage
			
			//System.out.println("numPlayers is " + numPlayers + ". ");
			//System.out.println("Currently have " + hm.games.get(nameOfGame).getPlayers().size() + " players.");
			
			
			//Wait for all users to join
			if (hm.games.get(nameOfGame) != null) {
				while (true) {
					if (hm.games.get(nameOfGame).getPlayers().size() == numPlayers) {
						oos.writeObject("start game");
						break;
					}
					else {
						/*
						//server output
						int needsPlayerInt = numPlayers - hm.games.get(nameOfGame).getPlayers().size();
						String needsPlayer = Integer.toString(needsPlayerInt);
						System.out.println(username + " - " + nameOfGame + " needs " + needsPlayer + " to start game. ");
						*/
						oos.writeObject("not yet");
					}
					oos.flush();
				}
				
				
				String secretWord = hm.games.get(nameOfGame).getSecretWord();
				oos.writeObject(secretWord);
				oos.flush();
				String currentMask = hm.games.get(nameOfGame).getCurrentMask();
				oos.writeObject(currentMask);
				oos.flush();
				
				//server output
				System.out.println(username + " - " + nameOfGame + " has " + numPlayers + " so starting game. Secret word is " + secretWord);
				
				String gameOver = "false";
				String win = "false";
				
				//sending the game object
				oos.writeObject(hm.games.get(nameOfGame));
				oos.flush();
				
				
				//???confused about the order that these codes happen (codes in the while true loop)
				while (true) {
					if (hm.games.get(nameOfGame).finished) {
						oos.writeObject("game ends");
						oos.flush();
						break;
					}
					else {
						oos.writeObject("game in progress");
						oos.flush();
					}
					
					ServerThread st = null;
					st = hm.games.get(nameOfGame).st.get(0);
					//System.out.println(st);
					
					if (st.equals(this)) {
						oos.writeObject("true");
						oos.flush();
					}
					else {
						oos.writeObject("false");
						oos.flush();
					}	

//					//sending remaining guesses or the game object again
//					oos.writeObject(hm.games.get(nameOfGame));
//					oos.flush();
					
					if (st.equals(this)) {
						if (!hm.remainingGuessesExist(nameOfGame)) {
							gameOver = "true";
							hm.games.get(nameOfGame).finished = true;
							//hm.everyoneLoses(nameOfGame);
							win = "false";
							hm.updateRecord(win, user);
							
							hm.games.get(nameOfGame).st.remove(0);
							
							hm.games.get(nameOfGame).getPlayers().remove(0);
							//System.out.println("Changing to player: " + hm.games.get(nameOfGame).getPlayers().get(0));
							
							oos.writeObject("remainingGuessesNotExist");
							oos.flush();
							
							break;
						}
						else {
							oos.writeObject("remainingGuessesExist");
							oos.flush();
						}
						
						Vector<String> guessInfoFromClient = (Vector<String>)ois.readObject();
						hm.games.get(nameOfGame).remainingGuesses --;
						//System.out.println("remainingGuesses is " + hm.games.get(nameOfGame).remainingGuesses);
						
						
						Vector<String> currentMaskAndGameStatus = new Vector<String>();
						if (guessInfoFromClient.get(0).equals("1")) {
							//user guessed a letter
							String letterGuessed = guessInfoFromClient.get(1);
							
							//sever output
							System.out.println(nameOfGame + " " + username + " - guessed letter " + letterGuessed);
							
							currentMask = hm.createTheMask(letterGuessed, secretWord, nameOfGame, username);
							
							
//							//sending currentMask
//							oos.writeObject(currentMask);
//							oos.flush();
							currentMaskAndGameStatus.addElement(currentMask);
							
//							gameOver = hm.checkIfGameOver(currentMask, secretWord);
//							if (gameOver) {
//								win = hm.checkIfWins();
//								hm.updateRecord(win, user);
//							}
//							oos.writeObject(gameOver);
//							oos.flush();
							if (hm.checkIfGameOver(currentMask, secretWord, nameOfGame)) { //game over
								gameOver = "true";
								hm.games.get(nameOfGame).finished = true;
								if (hm.remainingGuessesExist(nameOfGame)) {
									win = "true";
								}
								hm.updateRecord(win, user);
							}
							else { //game not over
								
							}
							currentMaskAndGameStatus.addElement(gameOver);
							
						}
						else if (guessInfoFromClient.get(0).equals("2")) {
							//user guessed a word
							String wordGuessed = guessInfoFromClient.get(1);
							//server output
							System.out.println(nameOfGame + " " + username + " - guessed word " + wordGuessed);
							if (wordGuessed.equals(secretWord)) {
								//server output
								System.out.println(nameOfGame + " " + username + " - " + wordGuessed + " is correct. " + username + " wins game. ");
								gameOver = "true";
								hm.games.get(nameOfGame).finished = true;
								win = "true";
								hm.updateRecord(win, user);
							}
							else {
								//server output;
								System.out.println(nameOfGame + " " + username + " - " + wordGuessed + " is incorrect. " + username + " has lost and is no longer in the game. "); 
								gameOver = "true";
								win = "false";
								hm.updateRecord(win, user);
								
							}
							currentMaskAndGameStatus.addElement(secretWord);
							currentMaskAndGameStatus.addElement(gameOver);
						}	

						//hm.broadcast(this, nameOfGame);
						
						//sending RG
						oos.writeObject(hm.games.get(nameOfGame).remainingGuesses);
						oos.flush();
						
						oos.writeObject(currentMaskAndGameStatus);
						oos.flush();
					
						if (gameOver.equals("false")) {
							hm.games.get(nameOfGame).st.add(st);
							hm.games.get(nameOfGame).st.remove(0);
							
							hm.games.get(nameOfGame).getPlayers().remove(0);
							hm.games.get(nameOfGame).getPlayers().add(username);
							//System.out.println("Changing to player: " + hm.games.get(nameOfGame).getPlayers().get(0));
							
							st = hm.games.get(nameOfGame).st.get(0);
							
							if(st==null) {
								System.out.println("Null");
								break;
							}
						}
						else if (gameOver.equals("true")) {
							hm.games.get(nameOfGame).st.remove(0);
							
							hm.games.get(nameOfGame).getPlayers().remove(0);
							//System.out.println("Changing to player: " + hm.games.get(nameOfGame).getPlayers().get(0));
							
							break;	
						}
					}
					
				}
				//output the result (game over or not) to the output stream
				//sending the game result 
				String[] gameResult = new String[2];
				gameResult[0] = gameOver;
				gameResult[1] = win;
				oos.writeObject(gameResult);
				oos.flush();
				
				//output the user record to the output stream
				//sending the user record
//				int[] userRecord = hm.getUserRecordFromDatabase(user);
//				oos.writeObject(userRecord);
//				oos.flush();
				userInfo = hm.getUserInfo(user);
				oos.writeObject(userInfo);
				oos.flush();
				
			}
			
		} catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		} catch (IOException ioe) {
			System.out.println("this one ioe: " + ioe.getMessage());
		} 
		
	}
}
