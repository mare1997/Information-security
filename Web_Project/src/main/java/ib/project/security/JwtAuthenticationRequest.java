package ib.project.rest;




import java.security.Principal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ib.project.dto.UserDTO;
import ib.project.model.Authority;
import ib.project.model.User;
import ib.project.service.AuthorityServiceInterface;
import ib.project.service.UserServiceInterface;
import ib.project.util.RootCAGenerator;
import ib.project.util.SignedCertificateGenerator;



@RestController
@RequestMapping(value = "api/users")
@CrossOrigin("*")
public class UserController {

	@Autowired
	private UserServiceInterface userService;
	
	@Autowired
	private AuthorityServiceInterface authorityService;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@RequestMapping("/whoami")
    public User user(Principal user) {
        return this.userService.getByEmail(user.getName());
    }
	@GetMapping
	public List<User> getAll() {
        return this.userService.getAll();
    }
	@GetMapping(value="/active")
	public ResponseEntity<List<UserDTO>> getActive(){
		List<UserDTO> active = new ArrayList<>();
		List<User> users = userService.getByActiveTrue();
		for (User user : users) {
			active.add(new UserDTO(user));
		}
		return new ResponseEntity<List<UserDTO>>(active,HttpStatus.OK);
	}
	
	@GetMapping(value="/inactive")
	public ResponseEntity<List<UserDTO>>  getInactive(){
		List<UserDTO> unactive = new ArrayList<>();
		List<User> users = userService.getByActiveFalse();
		for (User user : users) {
			unactive.add(new UserDTO(user));
		}
		return new ResponseEntity<List<UserDTO>>(unactive,HttpStatus.OK);
	}
	@GetMapping(value="/search/active/{parameter}")
	public ResponseEntity<List<UserDTO>> searchActiveByEmail(@PathVariable("parameter") String parameter){
		System.out.println(parameter);
		parameter = "%"+parameter+"%";
		List<UserDTO> active = new ArrayList<>();
		List<User> users = userService.getActiveByEmail(parameter);
		for (User user : users) {
			active.add(new UserDTO(user));
		}
		return new ResponseEntity<List<UserDTO>>(active,HttpStatus.OK);
	}
	
	@GetMapping(value="/search/inactive/{parameter}")
	public ResponseEntity<List<UserDTO>>searchUnactiveByEmail(@PathVariable("parameter") String parameter){
		parameter = "%"+parameter+"%";
		List<UserDTO> unactive = new ArrayList<>();
		List<User> users = userService.getInactiveByEmail(parameter);
		for (User user : users) {
			unactive.add(new UserDTO(user));
		}
		return new ResponseEntity<List<UserDTO>>(unactive,HttpStatus.OK);
	}
	
	@PostMapping(value="/register", consumes="application/json")
	public ResponseEntity<UserDTO> Register(@RequestBody UserDTO userDTO) {
		Authority authority = authorityService.getByName("REGULAR");
		
		User user = userService.getByEmail(userDTO.getEmail());
		if(user!=null) {
			return new ResponseEntity<UserDTO>(HttpStatus.FORBIDDEN);
		}
		user = new User();
		System.out.println(userDTO.getEmail());
		user.setEmail(userDTO.getEmail());
		user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
		user.setActive(false);
		user.getUser_authorities().add(authority);
		
		user = userService.save(user);
		return new ResponseEntity<UserDTO>(new UserDTO(user),HttpStatus.OK);
	}
	
	@PutMapping(value="/activate/{id}")
	public ResponseEntity<UserDTO> activateUser(@PathVariable("id") int id) throws ParseException{
		User user = userService.getById(id);
		if(user == null) {
			return new ResponseEntity<UserDTO>(HttpStatus.NOT_FOUND);
		}
		user.setActive(true);
		//RootCAGenerator r= new RootCAGenerator();
		//r.generateCA();
		SignedCertificateGenerator sig=new SignedCertificateGenerator(user.getEmail(),"123".toCharArray(),"rootCA".toCharArray());
		user.setCertificate(user.getEmail());
		user = userService.save(user);
		return new ResponseEntity<UserDTO>(new UserDTO(user),HttpStatus.OK);
	}
	
}
