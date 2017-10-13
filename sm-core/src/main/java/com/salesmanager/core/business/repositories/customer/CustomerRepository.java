package com.salesmanager.core.business.repositories.customer;

import com.salesmanager.core.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long>, CustomerRepositoryCustom {

	
	@Query("select c from Customer c join fetch c.merchantStore cm left join fetch c.defaultLanguage cl left join fetch c.attributes ca left join fetch ca.customerOption cao left join fetch ca.customerOptionValue cav left join fetch cao.descriptions caod left join fetch cav.descriptions left join fetch c.groups where c.id = ?1")
	Customer findOne(Long id);
	
	@Query("select c from Customer c join fetch c.merchantStore cm left join fetch c.defaultLanguage cl left join fetch c.attributes ca left join fetch ca.customerOption cao left join fetch ca.customerOptionValue cav left join fetch cao.descriptions caod left join fetch cav.descriptions left join fetch c.groups  where c.billing.firstName = ?1")
	List<Customer> findByName(String name);
	
	@Query("select c from Customer c join fetch c.merchantStore cm left join fetch c.defaultLanguage cl left join fetch c.attributes ca left join fetch ca.customerOption cao left join fetch ca.customerOptionValue cav left join fetch cao.descriptions caod left join fetch cav.descriptions left join fetch c.groups  where c.nick = ?1")
	Customer findByNick(String nick);
	
	@Query("select c from Customer c join fetch c.merchantStore cm left join fetch c.defaultLanguage cl left join fetch c.attributes ca left join fetch ca.customerOption cao left join fetch ca.customerOptionValue cav left join fetch cao.descriptions caod left join fetch cav.descriptions  left join fetch c.groups  where c.nick = ?1 and cm.id = ?2")
	Customer findByNick(String nick, int storeId);
	
	@Query("select c from Customer c join fetch c.merchantStore cm left join fetch c.defaultLanguage cl left join fetch c.attributes ca left join fetch ca.customerOption cao left join fetch ca.customerOptionValue cav left join fetch cao.descriptions caod left join fetch cav.descriptions  left join fetch c.groups  where cm.id = ?1")
	List<Customer> findByStore(int storeId);

	@Query("select c from Customer c " +
			"join fetch c.merchantStore cm " +
			"left join fetch c.defaultLanguage cl " +
			"left join fetch c.attributes ca " +
			"left join fetch ca.customerOption cao " +
			"left join fetch ca.customerOptionValue cav l" +
			"eft join fetch cao.descriptions caod " +
			"left join fetch cav.descriptions  " +
			"left join fetch c.groups  " +
			"where c.billing.telephone = ?1 and cm.id = ?2")
	Customer findByTelephone(String telephone, int storeId);
}
