package com.salesmanager.shop.store.services.customer;

import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionValueService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.business.services.user.GroupService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.customer.CustomerEntity;
import com.salesmanager.shop.model.customer.ReadableCustomer;
import com.salesmanager.shop.populator.customer.ReadableCustomerPopulator;
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.EmailTemplatesUtils;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class CustomerApiController extends BaseApiController{

    @Inject
    private CustomerService customerService;

    @Inject
    private CustomerOptionValueService customerOptionValueService;

    @Inject
    private CustomerOptionService customerOptionService;


    @Inject
    private MerchantStoreService merchantStoreService;

    @Inject
    private LanguageService languageService;


    @Inject
    private CountryService countryService;

    @Inject
    private GroupService groupService;

    @Inject
    private ZoneService zoneService;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    EmailService emailService;

    @Inject
    private LabelUtils messages;

    @Inject
    private EmailTemplatesUtils emailTemplatesUtils;

    @RequestMapping(value = "/api/{store}/customers", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> saveCustomer(@Valid @ModelAttribute("customer") Customer customer,
                                            BindingResult result,
                                            HttpServletRequest request) throws Exception{


        HashMap<String, Object> response = new HashMap<>();

        String email_regEx = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";
        Pattern pattern = Pattern.compile(email_regEx);
        Locale locale = Locale.ENGLISH;

        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

        Customer newCustomer = new Customer();

        if(customer.getId() != null && customer.getId()>=0) {
            newCustomer = customerService.getById(customer.getId());
            if(newCustomer == null) {
                return getErrorResponse(getMeta(1002, 400, "Customer not found"));
            }
        }

        newCustomer.setMerchantStore(store);


        if(!StringUtils.isBlank(customer.getEmailAddress() ) ){
            java.util.regex.Matcher matcher = pattern.matcher(customer.getEmailAddress());

            if(!matcher.find()) {
                ObjectError error = new ObjectError("customerEmailAddress",messages.getMessage("Email.customer.EmailAddress", locale));
                result.addError(error);


            }
        }else{
            ObjectError error = new ObjectError("customerEmailAddress",messages.getMessage("NotEmpty.customer.EmailAddress", locale));
            result.addError(error);
        }



        if( StringUtils.isBlank(customer.getBilling().getFirstName() ) ){
            ObjectError error = new ObjectError("billingFirstName", messages.getMessage("NotEmpty.customer.billingFirstName", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(customer.getBilling().getLastName() ) ){
            ObjectError error = new ObjectError("billingLastName", messages.getMessage("NotEmpty.customer.billingLastName", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(customer.getBilling().getAddress() ) ){
            ObjectError error = new ObjectError("billingAddress", messages.getMessage("NotEmpty.customer.billingStreetAddress", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(customer.getBilling().getCity() ) ){
            ObjectError error = new ObjectError("billingCity",messages.getMessage("NotEmpty.customer.billingCity", locale));
            result.addError(error);
        }

        /*if( customer.getShowBillingStateList().equalsIgnoreCase("yes" ) && customer.getBilling().getZone().getCode() == null ){
            ObjectError error = new ObjectError("billingState",messages.getMessage("NotEmpty.customer.billingState", locale));
            result.addError(error);

        }else if( customer.getShowBillingStateList().equalsIgnoreCase("no" ) && customer.getBilling().getState() == null ){
            ObjectError error = new ObjectError("billingState",messages.getMessage("NotEmpty.customer.billingState", locale));
            result.addError(error);

        }*/

        if( StringUtils.isBlank(customer.getBilling().getPostalCode() ) ){
            ObjectError error = new ObjectError("billingPostalCode", messages.getMessage("NotEmpty.customer.billingPostCode", locale));
            result.addError(error);
        }

        //check if error from the @valid
        if (result.hasErrors()) {


            StringBuilder errors = new StringBuilder();
            List<ObjectError> allErrors = result.getAllErrors();
            for (ObjectError allError : allErrors) {
                errors.append(allError.getDefaultMessage()).append(", ");
            }

           return getErrorResponse(getMeta(1001, 400, errors.toString()));
        }


        newCustomer.setEmailAddress(customer.getEmailAddress() );

        //get Customer country/zone
        //Country deliveryCountry = countryService.getByCode( customer.getDelivery().getCountry().getIsoCode());
        Country billingCountry  = countryService.getByCode( customer.getBilling().getCountry().getIsoCode()) ;

        //Zone deliveryZone = customer.getDelivery().getZone();
       // Zone billingZone  = customer.getBilling().getZone();



		/*if (customer.getShowDeliveryStateList().equalsIgnoreCase("yes" )) {
			deliveryZone = zoneService.getByCode(customer.getDelivery().getZone().getCode());
			customer.getDelivery().setState( null );

		}else if (customer.getShowDeliveryStateList().equalsIgnoreCase("no" )){
			deliveryZone = null ;
			customer.getDelivery().setState( customer.getDelivery().getState() );
		}

		if (customer.getShowBillingStateList().equalsIgnoreCase("yes" )) {
			billingZone = zoneService.getByCode(customer.getBilling().getZone().getCode());
			customer.getBilling().setState( null );

		}else if (customer.getShowBillingStateList().equalsIgnoreCase("no" )){
			billingZone = null ;
			customer.getBilling().setState( customer.getBilling().getState() );
		}*/



        newCustomer.setDefaultLanguage(customer.getDefaultLanguage() );

        /*customer.getDelivery().setZone(  deliveryZone);
        customer.getDelivery().setCountry(deliveryCountry );*/
        newCustomer.setDelivery( customer.getDelivery() );

        //customer.getBilling().setZone( billingZone);
        customer.getBilling().setCountry(billingCountry );
        newCustomer.setBilling( customer.getBilling()  );

        customerService.saveOrUpdate(newCustomer);
        response.put("meta", getMeta(0, 201, ""));
        HashMap<String, Object> customerData = new HashMap<>();
        customerData.put("id", newCustomer.getId());


        response.put("data", customerData);

        return response;

    }

    @RequestMapping(value = "/api/{store}/customers", method = RequestMethod.GET)
    @ResponseBody
    public HashMap<String, Object> getCustomers(HttpServletRequest request, HttpServletResponse response) {
        HashMap<String, Object> responseMap = new HashMap<>();


        List<Customer> customers =
                customerService.listByStore((MerchantStore) request.getAttribute(Constants.ADMIN_STORE));
        List<Map> customerList = new ArrayList<>();

        for(Customer customer : customers) {
            @SuppressWarnings("rawtypes")
            Map entry = new HashMap();
            entry.put("id", customer.getId());
            entry.put("firstName", customer.getBilling().getFirstName());
            entry.put("lastName", customer.getBilling().getLastName());
            entry.put("email", customer.getEmailAddress());
            entry.put("country", customer.getBilling().getCountry().getIsoCode());
            customerList.add(entry);
        }
        responseMap.put("data", customerList);
        responseMap.put("meta", getMeta(0, 200, ""));


        return responseMap;
    }

    @RequestMapping(value = "/api/{store}/customers/{id}", method = RequestMethod.GET)
    @ResponseBody
    public HashMap<String, Object> getCustomer(@PathVariable Long id, HttpServletRequest request) throws Exception{
        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore merchantStore = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

        Customer customer = customerService.getById(id);

        ReadableCustomerPopulator populator = new ReadableCustomerPopulator();
        ReadableCustomer readableCustomer = new ReadableCustomer();
        populator.populate(customer, readableCustomer, merchantStore, merchantStore.getDefaultLanguage());

        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableCustomer);

        return responseMap;
    }

    @RequestMapping(value =  "/api/{store}/customers/{id}/edit", method = RequestMethod.POST)
    @ResponseBody
    public HashMap<String, Object> editCustomer(@PathVariable Long id, HttpServletRequest request) throws Exception {
        HashMap<String, Object> responseMap = new HashMap<>();

        MerchantStore merchantStore = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Customer customer = customerService.getById(id);

        return responseMap;
    }

    @RequestMapping(value =  "/api/{store}/customers/{id}/delete", method = RequestMethod.POST)
    @ResponseBody
    public HashMap<String, Object> deleteCustomer(@PathVariable Long id, HttpServletRequest request) throws Exception {
        HashMap<String, Object> responseMap = new HashMap<>();

        Customer customer = customerService.getById(id);

        if(customer == null) {
            return getErrorResponse(getMeta(1002, 400, "Customer not found"));
        }
        customerService.delete(customer);
        responseMap.put("data", new HashMap<>());
        responseMap.put("meta", getMeta(0, 200, "Deleted"));
        return responseMap;
    }


}
