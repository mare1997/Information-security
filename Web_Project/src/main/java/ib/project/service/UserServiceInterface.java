package ib.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ib.project.model.User;
import ib.project.repository.UserRepository;
@Service
public class UserService implements UserServiceInterface {

	@Autowired
	UserRepository userrepository;
	
	

	@Override
	public User save(User user) {
		// TODO Auto-generated method stub
		return userrepository.save(user);
	}

	@Override
	public User getByEmail(String email) {
		// TODO Auto-generated method stub
		return userrepository.getByEmail(email);
	}

	@Override
	public List<User> getAll() {
		// TODO Auto-generated method stub
		return userrepository.findAll();
	}

	@Override
	public User getById(Integer id) {
		// TODO Auto-generated method stub
		return userrepository.getOne(id);
	}

	@Override
	public List<User> getActiveByEmail(String email) {
		// TODO Auto-generated method stub
		return userrepository.getActiveByEmail(email);
	}

	@Override
	public List<User> getInactiveByEmail(String email) {
		// TODO Auto-generated method stub
		return userrepository.getInactiveByEmail(email);
	}

	@Override
	public List<User> getByActiveTrue() {
		// TODO Auto-generated method stub
		return userrepository.getByActiveTrue();
	}

	@Override
	public List<User> getByActiveFalse() {
		// TODO Auto-generated method stub
		return userrepository.getByActiveFalse();
	}

	

}
