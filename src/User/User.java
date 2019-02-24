package User;

import java.io.Serializable;

public class User implements Serializable {
	public static final long serialVersionUID = 1;
	private String username;
	private String password;
	private int wins;
	private int losses;
	public boolean isYourTurn;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public int getWins() {
		return wins;
	}
	
	public int getLosses() {
		return losses;
	}
}
