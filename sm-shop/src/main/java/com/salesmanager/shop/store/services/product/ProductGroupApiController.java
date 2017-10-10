package com.salesmanager.shop.store.services.product;

import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.relationship.ProductRelationshipService;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.relationship.ProductRelationship;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.services.BaseApiController;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
@RequestMapping("/api")
public class ProductGroupApiController extends BaseApiController {

    @Inject
    private ProductRelationshipService productRelationshipService;
    @Inject
    private ProductService productService;

    @RequestMapping(value = "/{store}/products/groups", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse createGroup(@ModelAttribute("group") ProductRelationship group, BindingResult result,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {

        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);

        List<ProductRelationship> groups = productRelationshipService.getGroups(store);
        for (ProductRelationship grp : groups) {
            if (grp.getCode().equalsIgnoreCase(group.getCode())) {
                setResponse(response, getErrorResponse(getMeta(400, 400, "Code already in use")));
                return response;
            }
        }

        group.setActive(true);
        group.setStore(store);

        productRelationshipService.addGroup(store, group.getCode());

        Map meta = getMeta(0, 200, "");
        Map data = new HashMap();
        HashMap responseMap = new HashMap();
        responseMap.put("meta", meta);
        responseMap.put("data", data);

        setResponse(response, responseMap);

        return response;

    }

    @RequestMapping(value = "/{store}/products/groups/add-product", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse addProduct(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String code = request.getParameter("code");
        String productId = request.getParameter("productId");
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);

        Product product = productService.getById(Long.parseLong(productId));

        ProductRelationship relationship = new ProductRelationship();
        relationship.setActive(true);
        relationship.setCode(code);
        relationship.setStore(store);
        relationship.setRelatedProduct(product);

        productRelationshipService.saveOrUpdate(relationship);

        Map meta = getMeta(0, 200, "");
        Map data = new HashMap();
        HashMap responseMap = new HashMap();
        responseMap.put("meta", meta);
        responseMap.put("data", data);

        setResponse(response, responseMap);

        return response;

    }

    @RequestMapping(value = "/{store}/products/groups/remove-product", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse removeProduct(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String code = request.getParameter("code");
        Long productId = Long.valueOf(request.getParameter("productId"));
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);

        ProductRelationship relationship = null;
        List<ProductRelationship> relationships = productRelationshipService.getByGroup(store, code);

        for (ProductRelationship r : relationships) {
            if (r.getRelatedProduct().getId() == productId.longValue()) {
                relationship = r;
                break;
            }
        }

        if (relationship == null) {
            setResponse(response, getErrorResponse(getMeta(400, 400, "No relationship found")));
            return response;
        }

        productRelationshipService.delete(relationship);

        Map meta = getMeta(0, 200, "");
        Map data = new HashMap();
        HashMap responseMap = new HashMap();
        responseMap.put("meta", meta);
        responseMap.put("data", data);

        setResponse(response, responseMap);

        return response;

    }

    @RequestMapping(value = "/{store}/products/groups/{code}", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getProducts(@PathVariable String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Language language = (Language) request.getAttribute("LANGUAGE");
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);

        HashMap<String, Object> responseMap = new HashMap<>();
        List data = new ArrayList();

        List<ProductRelationship> relationships = productRelationshipService.getByGroup(store, code, language);

        for (ProductRelationship relationship : relationships) {

            Product product = relationship.getRelatedProduct();
            Map entry = new HashMap();
            entry.put("relationshipId", relationship.getId());
            entry.put("productId", product.getId());

            ProductDescription description = product.getDescriptions().iterator().next();
            Set<ProductDescription> descriptions = product.getDescriptions();
            for (ProductDescription desc : descriptions) {
                if (desc.getLanguage().getId().intValue() == language.getId().intValue()) {
                    description = desc;
                }
            }

            entry.put("name", description.getName());
            entry.put("sku", product.getSku());
            entry.put("available", product.isAvailable());

            data.add(entry);
        }

        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", data);

        setResponse(response, responseMap);

        return response;
    }

}
