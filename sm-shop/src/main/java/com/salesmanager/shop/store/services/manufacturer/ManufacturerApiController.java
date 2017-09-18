package com.salesmanager.shop.store.services.manufacturer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.manufacturer.ManufacturerDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.services.BaseApiController;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class ManufacturerApiController extends BaseApiController {

    @Inject
    private ManufacturerService manufacturerService;
    final HttpHeaders httpHeaders= new HttpHeaders();

    @RequestMapping(value = "/{store}/manufacturers", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getManufacturers(HttpServletRequest request, HttpServletResponse response) throws Exception{
        HashMap<String, Object> responseMap = new HashMap<>();

        Language language = (Language)request.getAttribute("LANGUAGE");
        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

        List<Manufacturer> manufacturers = null;
        manufacturers = manufacturerService.listByStore(store, language);
        List<Map> manufacturerList = new ArrayList<>();


        for(Manufacturer manufacturer : manufacturers) {

            @SuppressWarnings("rawtypes")
            Map entry = new HashMap();
            entry.put("id", manufacturer.getId());

            ManufacturerDescription description = manufacturer.getDescriptions().iterator().next();

            entry.put("name", description.getName());
            entry.put("code", manufacturer.getCode());
            entry.put("order", manufacturer.getOrder());

            manufacturerList.add(entry);

        }

        responseMap.put("data", manufacturerList);
        responseMap.put("meta", getMeta(0, 200, ""));

        setResponse(response, responseMap);

        return response;
    }


}
