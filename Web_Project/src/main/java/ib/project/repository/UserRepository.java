package ib.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ib.project.model.Authority;



public interface AuthorityRepository extends JpaRepository<Authority, Integer> {

	Authority getByName(String name);
}
