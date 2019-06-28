package ib.project.service;

import java.util.List;

import ib.project.model.User;





public interface UserServiceInterface {

	User getById(Integer id);
	User save(User user);
	User getByEmail(String email);
	List<User> getAll();
	
	List<User> getActiveByEmail(String email);
	
	List<User> getInactiveByEmail(String email);
	
	List<User> getByActiveTrue();
	
	List<User> getByActiveFalse();
	
	
}
