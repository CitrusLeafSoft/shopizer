package com.salesmanager.shop.store.services.order;

import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.order.orderproduct.OrderProductDownloadService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.order.orderproduct.OrderProductDownload;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.shop.admin.controller.orders.OrderControler;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.EmailUtils;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class OrderApiController extends BaseApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderControler.class);

    @Inject
    private LabelUtils messages;

    @Inject
    private OrderService orderService;

    @Inject
    CountryService countryService;

    @Inject
    ZoneService zoneService;

    @Inject
    PaymentService paymentService;

    @Inject
    CustomerService customerService;

    @Inject
    PricingService pricingService;

    @Inject
    TransactionService transactionService;

    @Inject
    EmailService emailService;

    @Inject
    private EmailUtils emailUtils;

    @Inject
    OrderProductDownloadService orderProdctDownloadService;


    @RequestMapping( value="/{store}/orders", method= RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public HttpServletResponse saveOrder(@Valid @ModelAttribute("order") com.salesmanager.shop.admin.model.orders.Order entityOrder, BindingResult result, HttpServletRequest request, HttpServletResponse servletResponse) throws Exception {

        HashMap<String, Object> response = new HashMap<>();
        String email_regEx = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";
        Pattern pattern = Pattern.compile(email_regEx);

        Language language = (Language)request.getAttribute("LANGUAGE");
        Locale locale = Locale.ENGLISH;

        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

        //set the id if fails
        entityOrder.setId(entityOrder.getOrder().getId());

        Set<OrderProduct> orderProducts = new HashSet<OrderProduct>();
        Set<OrderTotal> orderTotal = new HashSet<OrderTotal>();
        Set<OrderStatusHistory> orderHistory = new HashSet<OrderStatusHistory>();

        Date date = new Date();
        if(!StringUtils.isBlank(entityOrder.getDatePurchased() ) ){
            try {
                date = DateUtil.getDate(entityOrder.getDatePurchased());
            } catch (Exception e) {
                ObjectError error = new ObjectError("datePurchased",messages.getMessage("message.invalid.date", locale));
                result.addError(error);
            }

        } else{
            date = null;
        }


        if(!StringUtils.isBlank(entityOrder.getOrder().getCustomerEmailAddress() ) ){
            java.util.regex.Matcher matcher = pattern.matcher(entityOrder.getOrder().getCustomerEmailAddress());

            if(!matcher.find()) {
                ObjectError error = new ObjectError("customerEmailAddress",messages.getMessage("Email.order.customerEmailAddress", locale));
                result.addError(error);
            }
        }else{
            ObjectError error = new ObjectError("customerEmailAddress",messages.getMessage("NotEmpty.order.customerEmailAddress", locale));
            result.addError(error);
        }


        if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getFirstName() ) ){
            ObjectError error = new ObjectError("billingFirstName", messages.getMessage("NotEmpty.order.billingFirstName", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getFirstName() ) ){
            ObjectError error = new ObjectError("billingLastName", messages.getMessage("NotEmpty.order.billingLastName", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getAddress() ) ){
            ObjectError error = new ObjectError("billingAddress", messages.getMessage("NotEmpty.order.billingStreetAddress", locale));
            result.addError(error);
        }

        if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getCity() ) ){
            ObjectError error = new ObjectError("billingCity",messages.getMessage("NotEmpty.order.billingCity", locale));
            result.addError(error);
        }

        if( entityOrder.getOrder().getBilling().getZone()==null){
            if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getState())){
                ObjectError error = new ObjectError("billingState",messages.getMessage("NotEmpty.order.billingState", locale));
                result.addError(error);
            }
        }

        if( StringUtils.isBlank(entityOrder.getOrder().getBilling().getPostalCode() ) ){
            ObjectError error = new ObjectError("billingPostalCode", messages.getMessage("NotEmpty.order.billingPostCode", locale));
            result.addError(error);
        }

        com.salesmanager.core.model.order.Order newOrder = orderService.getById(entityOrder.getOrder().getId() );


        //get capturable
//        if(newOrder.getPaymentType().name() != PaymentType.MONEYORDER.name()) {
//            Transaction capturableTransaction = transactionService.getCapturableTransaction(newOrder);
//            if(capturableTransaction!=null) {
//                model.addAttribute("capturableTransaction",capturableTransaction);
//            }
//        }


        //get refundable
//        if(newOrder.getPaymentType().name() != PaymentType.MONEYORDER.name()) {
//            Transaction refundableTransaction = transactionService.getRefundableTransaction(newOrder);
//            if(refundableTransaction!=null) {
//                model.addAttribute("capturableTransaction",null);//remove capturable
//                model.addAttribute("refundableTransaction",refundableTransaction);
//            }
//        }


        if (result.hasErrors()) {
            //  somehow we lose data, so reset Order detail info.
            entityOrder.getOrder().setOrderProducts( orderProducts);
            entityOrder.getOrder().setOrderTotal(orderTotal);
            entityOrder.getOrder().setOrderHistory(orderHistory);

            StringBuilder errors = new StringBuilder();
            List<ObjectError> allErrors = result.getAllErrors();
            for (ObjectError allError : allErrors) {
                errors.append(allError.getDefaultMessage()).append(", ");
            }

            HashMap map = getErrorResponse(getMeta(1001, 400, errors.toString()));
            setResponse(servletResponse, map);
            return servletResponse;
        }

        OrderStatusHistory orderStatusHistory = new OrderStatusHistory();




//        Country deliveryCountry = countryService.getByCode( entityOrder.getOrder().getDelivery().getCountry().getIsoCode());
        Country billingCountry  = countryService.getByCode( entityOrder.getOrder().getBilling().getCountry().getIsoCode()) ;
        Zone billingZone = null;
        Zone deliveryZone = null;
        if(entityOrder.getOrder().getBilling().getZone()!=null) {
            billingZone = zoneService.getByCode(entityOrder.getOrder().getBilling().getZone().getCode(), billingCountry);
        }

//        if(entityOrder.getOrder().getDelivery().getZone()!=null) {
//            deliveryZone = zoneService.getByCode(entityOrder.getOrder().getDelivery().getZone().getCode(), deliveryCountry);
//        }

        newOrder.setCustomerEmailAddress(entityOrder.getOrder().getCustomerEmailAddress() );
        newOrder.setStatus(entityOrder.getOrder().getStatus() );

        newOrder.setDatePurchased(date);
        newOrder.setLastModified( new Date() );

        if(!StringUtils.isBlank(entityOrder.getOrderHistoryComment() ) ) {
            orderStatusHistory.setComments( entityOrder.getOrderHistoryComment() );
            orderStatusHistory.setCustomerNotified(1);
            orderStatusHistory.setStatus(entityOrder.getOrder().getStatus());
            orderStatusHistory.setDateAdded(new Date() );
            orderStatusHistory.setOrder(newOrder);
            newOrder.getOrderHistory().add( orderStatusHistory );
            entityOrder.setOrderHistoryComment( "" );
        }

        newOrder.setDelivery( entityOrder.getOrder().getDelivery() );
        newOrder.setBilling( entityOrder.getOrder().getBilling() );
        newOrder.setCustomerAgreement(entityOrder.getOrder().getCustomerAgreement());

//        newOrder.getDelivery().setCountry(deliveryCountry );
        newOrder.getBilling().setCountry(billingCountry );

        if(billingZone!=null) {
            newOrder.getBilling().setZone(billingZone);
        }

        if(deliveryZone!=null) {
            newOrder.getDelivery().setZone(deliveryZone);
        }

        orderService.saveOrUpdate(newOrder);
        entityOrder.setOrder(newOrder);
        entityOrder.setBilling(newOrder.getBilling());
        entityOrder.setDelivery(newOrder.getDelivery());
//        model.addAttribute("order", entityOrder);

        Long customerId = newOrder.getCustomerId();

        if(customerId!=null && customerId>0) {

            try {

                Customer customer = customerService.getById(customerId);
                if(customer!=null) {
//                    model.addAttribute("customer",customer);
                }


            } catch(Exception e) {
                LOGGER.error("Error while getting customer for customerId " + customerId, e);
            }

        }

        List<OrderProductDownload> orderProductDownloads = orderProdctDownloadService.getByOrderId(newOrder.getId());
        if(CollectionUtils.isNotEmpty(orderProductDownloads)) {
//            model.addAttribute("downloads",orderProductDownloads);
        }


        /**
         * send email if admin posted orderHistoryComment
         *
         * **/

        if(StringUtils.isBlank(entityOrder.getOrderHistoryComment())) {

//            try {
//
//                Customer customer = customerService.getById(newOrder.getCustomerId());
//                Language lang = store.getDefaultLanguage();
//                if(customer!=null) {
//                    lang = customer.getDefaultLanguage();
//                }
//
//                Locale customerLocale = LocaleUtils.getLocale(lang);
//
//                StringBuilder customerName = new StringBuilder();
//                customerName.append(newOrder.getBilling().getFirstName()).append(" ").append(newOrder.getBilling().getLastName());


//                Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(request.getContextPath(), store, messages, customerLocale);
//                templateTokens.put(EmailConstants.EMAIL_CUSTOMER_NAME, customerName.toString());
//                templateTokens.put(EmailConstants.EMAIL_TEXT_ORDER_NUMBER, messages.getMessage("email.order.confirmation", new String[]{String.valueOf(newOrder.getId())}, customerLocale));
//                templateTokens.put(EmailConstants.EMAIL_TEXT_DATE_ORDERED, messages.getMessage("email.order.ordered", new String[]{entityOrder.getDatePurchased()}, customerLocale));
//                templateTokens.put(EmailConstants.EMAIL_TEXT_STATUS_COMMENTS, messages.getMessage("email.order.comments", new String[]{entityOrder.getOrderHistoryComment()}, customerLocale));
//                templateTokens.put(EmailConstants.EMAIL_TEXT_DATE_UPDATED, messages.getMessage("email.order.updated", new String[]{DateUtil.formatDate(new Date())}, customerLocale));


//                Email email = new Email();
//                email.setFrom(store.getStorename());
//                email.setFromEmail(store.getStoreEmailAddress());
//                email.setSubject(messages.getMessage("email.order.status.title",new String[]{String.valueOf(newOrder.getId())},customerLocale));
//                email.setTo(entityOrder.getOrder().getCustomerEmailAddress());
//                email.setTemplateName(ORDER_STATUS_TMPL);
//                email.setTemplateTokens(templateTokens);
//


//                emailService.sendHtmlEmail(store, email);

//            } catch (Exception e) {
//                LOGGER.error("Cannot send email to customer",e);
//            }
//
        }

//        model.addAttribute("success","success");

        response.put("meta", getMeta(0, 201, ""));
        HashMap<String, Object> orderData = new HashMap<>();
        orderData.put("id", newOrder.getId());


        response.put("data", orderData);

        setResponse(servletResponse, response);
        return servletResponse;

    }


}
