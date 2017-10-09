package com.salesmanager.shop.store.services.product;

import com.salesmanager.core.business.services.catalog.product.relationship.ProductRelationshipService;
import com.salesmanager.core.model.catalog.product.relationship.ProductRelationship;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.services.BaseApiController;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class ProductGroupApiController extends BaseApiController {

    @Inject
    private ProductRelationshipService productRelationshipService;

    @RequestMapping(value = "/{store}/products/groups", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse saveGroup(@ModelAttribute("group") ProductRelationship group, BindingResult result,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {

        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

        List<ProductRelationship> groups = productRelationshipService.getGroups(store);
        for(ProductRelationship grp : groups) {
            if(grp.getCode().equalsIgnoreCase(group.getCode())) {
                setResponse(response, getErrorResponse(getMeta(400, 400, "Code already in use")));
                return response;
            }
        }

        group.setActive(true);
        group.setStore(store);

        productRelationshipService.addGroup(store,group.getCode());

        Map meta = getMeta(0, 200, "");
        Map data = new HashMap();
        HashMap responseMap = new HashMap();
        responseMap.put("meta", meta);
        responseMap.put("data", data);

        setResponse(response, responseMap);

        return response;

    }

}
