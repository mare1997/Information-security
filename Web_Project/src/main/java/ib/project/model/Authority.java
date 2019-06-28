package ib.project.dto;

import java.io.Serializable;

import ib.project.model.User;

public class UserDTO implements Serializable{

	private int id;
	private String email;
	private String password;
	private String certificate;
	private boolean active;
	
	
	public UserDTO() {
		
	}


	public UserDTO(int id, String email, String password, String certificate, boolean active) {
		super();
		this.id = id;
		this.email = email;
		this.password = password;
		this.certificate = certificate;
		this.active = active;
	}
	
	public UserDTO(User user) {
		this(user.getId(),
		user.getEmail(),
		user.getPassword(),
		user.getCertificate(),
		user.isActive());
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getCertificate() {
		return certificate;
	}


	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}


	public boolean isActive() {
		return active;
	}


	public void setActive(boolean active) {
		this.active = active;
	}
	
	
}
