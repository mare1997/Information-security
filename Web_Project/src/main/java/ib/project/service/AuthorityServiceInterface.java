package ib.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ib.project.model.Authority;
import ib.project.repository.AuthorityRepository;


@Service
public class AuthoritySercive implements AuthorityServiceInterface {

	@Autowired
	AuthorityRepository authorityRepository;
	
	@Override
	public Authority getByName(String name) {
		
		return authorityRepository.getByName(name);
	}

}
