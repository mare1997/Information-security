package ib.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ib.project.model.User;




public interface UserRepository extends JpaRepository<User, Integer>{
	
	User getByEmail(String email);
	
	@Query(value="SELECT * FROM users AS u WHERE u.active = true",nativeQuery=true)
	List<User> getByActiveTrue();

	@Query(value="SELECT * FROM users AS u WHERE u.active = false",nativeQuery=true)
	List<User> getByActiveFalse();
	
	@Query(value="SELECT * FROM users AS u WHERE u.active = true AND u.email LIKE ?",nativeQuery=true)
	List<User> getActiveByEmail(String email);
	
	@Query(value="SELECT * FROM users AS u WHERE u.active = false AND u.email LIKE ?",nativeQuery=true)
	List<User> getInactiveByEmail(String email);
	
}
