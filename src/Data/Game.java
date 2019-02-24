package Data;

//import java.util.LinkedList;
//import java.util.Queue;
import java.util.Vector;

import java.io.Serializable;


import Server.ServerThread;
import User.User;



public class Game implements Serializable{
	public static final long serialVersionUID = 2;

	private int numPlayers;
	private Vector<String> players;
	public String secretWord;
	public String nameOfGame;
	public String currentMask;
	transient public Vector<ServerThread> st;
	public Vector<User> playersVec; 
	public boolean finished;
	public int remainingGuesses;
	
	public Game() {
		
	}
	
	public Game(String nameOfGame, int numPlayers, Vector<String> players, String secretWord, 
			String currentMask, boolean finished, int remainingGuesses) {
		this.nameOfGame = nameOfGame;
		this.numPlayers = numPlayers;
		this.players = players;
		this.secretWord = secretWord;
		this.currentMask = currentMask;
		this.finished = finished;
		this.remainingGuesses = remainingGuesses;
		
		st = new Vector<ServerThread>();
	}
	
	public int getNumPlayers() {
		return numPlayers;
	}
	public void setNumPlayers(int numPlayers) {
		this.numPlayers = numPlayers;
	}
	
	public Vector<String> getPlayers(){
		return players;
	}
	public void setPlayers(Vector<String> players) {
		this.players = players;
	}
	public void addAPlayer(String username) {
		this.players.addElement(username);
	}
	
	public String getSecretWord() {
		return secretWord;
	}
	public void setSecretWord(String secretWord) {
		this.secretWord = secretWord;
	}
	
	public String getCurrentMask() {
		return currentMask;
	}
	public void setCurrentMask(String newMask) {
		this.currentMask = newMask;
	}
	public void changeMask(char guessedLetter) {
		
	}
	
	public void addPlayerToST(ServerThread st) {
		this.st.addElement(st);
	}

}
