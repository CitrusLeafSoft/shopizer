package com.salesmanager.shop.filter;

import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.constants.Constants;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

public class ApiFilter extends HandlerInterceptorAdapter {

    @Inject
    private MerchantStoreService merchantService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String storeCode = request.getParameter("storeCode");
        if(storeCode == null || storeCode == "") {
            storeCode = MerchantStore.DEFAULT_STORE;
        }

        MerchantStore store = merchantService.getByCode(storeCode);
        request.getSession().setAttribute(Constants.ADMIN_STORE, store);
        request.setAttribute(Constants.ADMIN_STORE, store);
        request.setAttribute(Constants.LANGUAGE, Locale.ENGLISH);
        return true;
    }
}
