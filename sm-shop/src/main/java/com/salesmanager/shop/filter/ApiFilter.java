package com.salesmanager.shop.filter;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.GsonBuilder;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.constants.Constants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class ApiFilter extends HandlerInterceptorAdapter {

    @Inject
    private MerchantStoreService merchantService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = new AntPathMatcher().extractPathWithinPattern( "/api/**", request.getRequestURI() );
        String storeCode = null;
        try {
            storeCode = path.split("/")[0];
        }catch(NullPointerException e) {
            storeCode = MerchantStore.DEFAULT_STORE;
        }

        MerchantStore store = merchantService.getByCode(storeCode);
        request.getSession().setAttribute(Constants.ADMIN_STORE, store);
        request.setAttribute(Constants.ADMIN_STORE, store);

        request.setAttribute(Constants.LANGUAGE, store.getDefaultLanguage());

        //Check if the context path contains login, and apply JWT authentication
        final String compactJws = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!request.getRequestURI().contains("login")) {

            try {
                Jwts.parser().setSigningKey("abc123").parseClaimsJws(compactJws);
            } catch (MalformedJwtException | IllegalArgumentException | SignatureException e) {
                response.setStatus(HttpStatusCodes.STATUS_CODE_FORBIDDEN);



                Map responseMap = new HashMap<>();
                Map meta = new HashMap();
                meta.put("error_code", HttpStatusCodes.STATUS_CODE_FORBIDDEN);
                meta.put("http_code", HttpStatusCodes.STATUS_CODE_FORBIDDEN);
                meta.put("message", "Unauthorized access");
                responseMap.put("meta", meta);

                Map data = new HashMap();
                responseMap.put("data", data);

                String jsonString = new GsonBuilder().create().toJson(responseMap);
                IOUtils.write(jsonString.getBytes(), response.getOutputStream());
                IOUtils.closeQuietly(response.getOutputStream());
                return false;
            }

        }


        return true;
    }
}
