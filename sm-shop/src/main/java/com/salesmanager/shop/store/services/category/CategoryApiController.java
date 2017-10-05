package com.salesmanager.shop.store.services.category;

import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.category.CategoryDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.populator.catalog.ReadableCategoryPopulator;
import com.salesmanager.shop.store.controller.category.facade.CategoryFacade;
import com.salesmanager.shop.store.services.BaseApiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class CategoryApiController extends BaseApiController{

    @Inject
    private LanguageService languageService;

    @Inject
    private MerchantStoreService merchantStoreService;

    @Inject
    private CategoryService categoryService;

    @Inject
    private ProductService productService;

    @Inject
    private CategoryFacade categoryFacade;

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryApiController.class);

    @RequestMapping(value = "/{store}/category/{id}", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getCategory(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore merchantStore = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = merchantStore.getDefaultLanguage();

        try {
            Category dbCategory = categoryService.getByLanguage(id, language);

            if(dbCategory==null || dbCategory.getMerchantStore().getId().intValue()!=merchantStore.getId().intValue()){
                responseMap.put("data", "");
                responseMap.put("meta", getMeta(0, 503, "Invalid category id"));
                setResponse(response, responseMap);
                return response;
            }
            ReadableCategoryPopulator populator = new ReadableCategoryPopulator();
            ReadableCategory category = populator.populate(dbCategory, new ReadableCategory(), merchantStore, merchantStore.getDefaultLanguage());

            responseMap.put("data", category);
            responseMap.put("meta", getMeta(0, 200, ""));
            setResponse(response, responseMap);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Error while saving category",e);
            responseMap.put("data", "");
            responseMap.put("meta", getMeta(0, 503, "Error in Retrieving Category"));

        }
        setResponse(response, responseMap);
        return response;
    }

    @RequestMapping( value="/{store}/category/{id}/delete", method=RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse deleteCategory(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore merchantStore = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Category category = categoryService.getById(id);

        if(category != null && category.getMerchantStore().getCode().equalsIgnoreCase(merchantStore.getCode())){
            categoryService.delete(category);
            responseMap.put("data", new HashMap<>());
            responseMap.put("meta", getMeta(0, 200, "Deleted"));
            setResponse(response, responseMap);
            return response;
        }else{
            HashMap map = getErrorResponse(getMeta(1002, 400, "Category not found"));
            setResponse(response, map);
            return response;
        }
    }

    @RequestMapping( value="/{store}/category", method=RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse createCategory(@Valid @ModelAttribute("category") Category category,
                                 BindingResult result, HttpServletRequest request, HttpServletResponse servletResponse) throws Exception {

        HashMap<String, Object> response = new HashMap<>();
        MerchantStore merchantStore = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Map<String,Language> langs = languageService.getLanguagesMap();

        if(category.getId() != null && category.getId() >0) { //edit entry
            //get from DB
            Category currentCategory = categoryService.getById(category.getId());
            if(currentCategory==null || currentCategory.getMerchantStore().getId().intValue()!=merchantStore.getId().intValue()) {
                HashMap map = getErrorResponse(getMeta(1002, 400, "Category not found"));
                setResponse(servletResponse, map);
                return servletResponse;
            }
        }

        List<CategoryDescription> descriptions = category.getDescriptions();
        if(descriptions!=null) {
            for(CategoryDescription description : descriptions) {
                Language l = langs.get("en");
                description.setLanguage(l);
                description.setCategory(category);
            }
        }
        //save to DB
        category.setMerchantStore(merchantStore);
        //}

        if (result.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            List<ObjectError> allErrors = result.getAllErrors();
            for (ObjectError allError : allErrors) {
                errors.append(allError.getDefaultMessage()).append(", ");
            }

            HashMap map = getErrorResponse(getMeta(1001, 400, errors.toString()));
            setResponse(servletResponse, map);
            return servletResponse;
        }

        //check parent
        if(category.getParent()!=null) {
            if(category.getParent().getId()==-1) {//this is a root category
                category.setParent(null);
                category.setLineage("/");
                category.setDepth(0);
            }
        }

        category.getAuditSection().setModifiedBy(request.getRemoteUser());
        categoryService.saveOrUpdate(category);

        //adjust lineage and depth
        if(category.getParent()!=null && category.getParent().getId()!=-1) {
            Category parent = new Category();
            parent.setId(category.getParent().getId());
            parent.setMerchantStore(merchantStore);

            categoryService.addChild(parent, category);
        }

        response.put("meta", getMeta(0, 201, ""));
        HashMap<String, Object> categoryData = new HashMap<>();
        categoryData.put("id", category.getId());

        response.put("data", categoryData);
        setResponse(servletResponse, response);
        return servletResponse;
    }

    @RequestMapping(value = "/{store}/category", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getCategories(HttpServletRequest request, HttpServletResponse servletResponse) throws Exception {
        HashMap<String, Object> responseMap = new HashMap<>();

        Language language = (Language) request.getAttribute("LANGUAGE");
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);
        List<Category> categories = categoryService.listByStore(store, language);;
        List<Map> categoryList = new ArrayList<>();

        for (Category category : categories) {
            @SuppressWarnings("rawtypes")
            Map entry = new HashMap();
            entry.put("categoryId", category.getId());
            CategoryDescription description = category.getDescriptions().get(0);
            entry.put("name", description.getName());
            entry.put("code", category.getCode());
            entry.put("visible", category.isVisible());
            categoryList.add(entry);
        }
        responseMap.put("data", categoryList);
        responseMap.put("meta", getMeta(0, 200, ""));
        setResponse(servletResponse, responseMap);
        return servletResponse;


    }

}
