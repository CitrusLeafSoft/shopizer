package com.salesmanager.core.business.services.customer;


import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.common.Address;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.CustomerCriteria;
import com.salesmanager.core.model.customer.CustomerList;
import com.salesmanager.core.model.merchant.MerchantStore;

import java.util.List;



public interface CustomerService  extends SalesManagerEntityService<Long, Customer> {

	public List<Customer> getByName(String firstName);

	List<Customer> listByStore(MerchantStore store);

	Customer getByNick(String nick);
	void saveOrUpdate(Customer customer) throws ServiceException ;

	CustomerList listByStore(MerchantStore store, CustomerCriteria criteria);

	Customer getByNick(String nick, int storeId);

	Customer getByTelephone(String telephone, int storeId);

	/**
	 * Return an {@link com.salesmanager.core.business.common.model.Address} object from the client IP address. Uses underlying GeoLocation module
	 * @param store
	 * @param ipAddress
	 * @return
	 * @throws ServiceException
	 */
	Address getCustomerAddress(MerchantStore store, String ipAddress)
			throws ServiceException;


}
