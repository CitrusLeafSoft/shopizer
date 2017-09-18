package com.salesmanager.shop.store.services.manufacturer;

import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.manufacturer.ManufacturerDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.admin.controller.ControllerConstants;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.populator.manufacturer.ReadableManufacturerPopulator;
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.util.*;

@Controller
@RequestMapping("/api")
public class ManufacturerApiController extends BaseApiController {

    @Inject
    private ManufacturerService manufacturerService;

    @Inject
    private LanguageService languageService;

    @Inject
    private CoreConfiguration configuration;

    @Inject
    private LabelUtils messages;

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

    @RequestMapping(value = "/{store}/manufacturers/{id}", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getMerchant(@PathVariable Long id, HttpServletRequest request, HttpServletResponse servletResponse) throws Exception {

        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = (Language) request.getAttribute(Constants.LANGUAGE);
        Manufacturer manufacturer = manufacturerService.getById(id);

        if(manufacturer==null) {
            setResponse(servletResponse, getErrorResponse(getMeta(4001, 400, "Manufacturer not found")));
            return servletResponse;
        }

        if(manufacturer.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
            setResponse(servletResponse, getErrorResponse(getMeta(4001, 400, "Manufacturer does not belong to the store")));
            return servletResponse;
        }

        ReadableManufacturerPopulator populator = new ReadableManufacturerPopulator();
        ReadableManufacturer readableManufacturer = new ReadableManufacturer();
        populator.populate(manufacturer, readableManufacturer, store, language);

        HashMap<String, Object> responseMap = new HashMap<>();
        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableManufacturer);

        setResponse(servletResponse, responseMap);
        return servletResponse;
    }

    @RequestMapping(value = "/{store}/manufacturers", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse saveMerchant(@Valid @ModelAttribute("manufacturer")
                                                        com.salesmanager.shop.admin.model.catalog.Manufacturer manufacturer,
                                            BindingResult result,
                                            HttpServletRequest request,
                                            HttpServletResponse servletResponse) throws Exception {


        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        List<Language> languages = languageService.getLanguages();
        Locale locale = Locale.ENGLISH;

        if(manufacturer.getImage()!=null && !manufacturer.getImage().isEmpty()) {

            try {

                String maxHeight = configuration.getProperty("PRODUCT_IMAGE_MAX_HEIGHT_SIZE");
                String maxWidth = configuration.getProperty("PRODUCT_IMAGE_MAX_WIDTH_SIZE");
                String maxSize = configuration.getProperty("PRODUCT_IMAGE_MAX_SIZE");

                BufferedImage image = ImageIO.read(manufacturer.getImage().getInputStream());

                if(!StringUtils.isBlank(maxHeight)) {

                    int maxImageHeight = Integer.parseInt(maxHeight);
                    if(image.getHeight()>maxImageHeight) {
                        ObjectError error = new ObjectError("image",messages.getMessage("message.image.height", locale) + " {"+maxHeight+"}");
                        result.addError(error);
                    }
                }

                if(!StringUtils.isBlank(maxWidth)) {

                    int maxImageWidth = Integer.parseInt(maxWidth);
                    if(image.getWidth()>maxImageWidth) {
                        ObjectError error = new ObjectError("image",messages.getMessage("message.image.width", locale) + " {"+maxWidth+"}");
                        result.addError(error);
                    }
                }

                if(!StringUtils.isBlank(maxSize)) {

                    int maxImageSize = Integer.parseInt(maxSize);
                    if(manufacturer.getImage().getSize()>maxImageSize) {
                        ObjectError error = new ObjectError("image",messages.getMessage("message.image.size", locale) + " {"+maxSize+"}");
                        result.addError(error);
                    }
                }

            } catch (Exception e) {

            }

        }

        if (result.hasErrors()) {
            StringBuilder stringBuilder = new StringBuilder();
            List errors = result.getAllErrors();
            for (Object error : errors) {
                stringBuilder.append(", ").append(error.toString());
            }
            setResponse(servletResponse, getErrorResponse(getMeta(4003, 400, stringBuilder.toString())));
            return servletResponse;
        }

        Manufacturer newManufacturer = manufacturer.getManufacturer();

        if ( manufacturer.getManufacturer().getId() !=null && manufacturer.getManufacturer().getId()  > 0 ){

            newManufacturer = manufacturerService.getById( manufacturer.getManufacturer().getId() );

            if(newManufacturer.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
                setResponse(servletResponse, getErrorResponse(getMeta(4004, 400, "Manufacturer does not belong to store")));
                return servletResponse;
            }

        }

        Set<ManufacturerDescription> descriptions = new HashSet<ManufacturerDescription>();
        if(manufacturer.getDescriptions()!=null && manufacturer.getDescriptions().size()>0) {

            for(ManufacturerDescription desc : manufacturer.getDescriptions()) {

                desc.setManufacturer(newManufacturer);
                descriptions.add(desc);
            }
        }
        newManufacturer.setDescriptions(descriptions );
        newManufacturer.setOrder( manufacturer.getOrder() );
        newManufacturer.setMerchantStore(store);
        newManufacturer.setCode(manufacturer.getCode());

        manufacturerService.saveOrUpdate(newManufacturer);
        HashMap<String, Object> response = new HashMap<>();

        response.put("meta", getMeta(0, 201, ""));
        HashMap<String, Object> data = new HashMap<>();
        data.put("id", newManufacturer.getId());


        response.put("data", data);

        setResponse(servletResponse, response);

        return servletResponse;
    }


    @RequestMapping(value = "/{store}/manufacturers/{id}/delete", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse deleteManufacturer(HttpServletRequest request, HttpServletResponse servletResponse) throws Exception {
        //TODO: Implement this refer: ManufacturerController.deleteManufacturer();
        return servletResponse;
    }


}
