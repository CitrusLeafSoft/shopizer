package com.salesmanager.shop.store.services.product;

import com.google.gson.Gson;
import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService;
import com.salesmanager.core.business.services.tax.TaxClassService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.business.utils.ajax.AjaxPageableResponse;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.ProductCriteria;
import com.salesmanager.core.model.catalog.product.ProductList;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.image.ProductImageDescription;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription;
import com.salesmanager.core.model.catalog.product.type.ProductType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.tax.taxclass.TaxClass;
import com.salesmanager.shop.admin.controller.products.ProductController;
import com.salesmanager.shop.admin.model.web.Menu;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductList;
import com.salesmanager.shop.model.catalog.product.ReadableProductPrice;
import com.salesmanager.shop.populator.catalog.ReadableProductPopulator;
import com.salesmanager.shop.populator.catalog.ReadableProductPricePopulator;
import com.salesmanager.shop.populator.catalog.ReadableProductReviewPopulator;
import com.salesmanager.shop.model.catalog.product.ReadableProductReview;
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping(value = "/api")
public class ProductApiController extends BaseApiController{

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    @Inject
    private ProductService productService;

    @Inject
    private ManufacturerService manufacturerService;

    @Inject
    private ProductTypeService productTypeService;

    @Inject
    private ProductImageService productImageService;

    @Inject
    private TaxClassService taxClassService;

    @Inject
    private ProductPriceUtils priceUtil;

    @Inject
    LabelUtils messages;

    @Inject
    private CoreConfiguration configuration;

    @Inject
    CategoryService categoryService;


    @RequestMapping(value = "/create-product", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse saveProduct(@Valid @ModelAttribute("product") com.salesmanager.shop.admin.model.catalog.Product  product, BindingResult result, Model model, HttpServletRequest request, HttpServletResponse servletResponse, Locale locale) throws Exception {


       // Language language = new Language("en");//(Language)request.getAttribute("LANGUAGE");

        //display menu
        //setMenu(model,request);
        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();

        List<Manufacturer> manufacturers = manufacturerService.listByStore(store, language);

        List<ProductType> productTypes = productTypeService.list();

        List<TaxClass> taxClasses = taxClassService.listByStore(store);

        List<Language> languages = store.getLanguages();

        /*model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("productTypes", productTypes);
        model.addAttribute("taxClasses", taxClasses);
*/
        //validate price
        BigDecimal submitedPrice = null;
        try {
            submitedPrice = priceUtil.getAmount(product.getProductPrice());
        } catch (Exception e) {
            ObjectError error = new ObjectError("productPrice",messages.getMessage("NotEmpty.product.productPrice", locale));
            result.addError(error);
        }
        Date date = new Date();
        if(!StringUtils.isBlank(product.getDateAvailable())) {
            try {
                date = DateUtil.getDate(product.getDateAvailable());
                product.getAvailability().setProductDateAvailable(date);
                product.setDateAvailable(DateUtil.formatDate(date));
            } catch (Exception e) {
                ObjectError error = new ObjectError("dateAvailable",messages.getMessage("message.invalid.date", locale));
                result.addError(error);
            }
        }



        //validate image
        if(product.getImage()!=null && !product.getImage().isEmpty()) {

            try {

                String maxHeight = configuration.getProperty("PRODUCT_IMAGE_MAX_HEIGHT_SIZE");
                String maxWidth = configuration.getProperty("PRODUCT_IMAGE_MAX_WIDTH_SIZE");
                String maxSize = configuration.getProperty("PRODUCT_IMAGE_MAX_SIZE");


                BufferedImage image = ImageIO.read(product.getImage().getInputStream());


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
                    if(product.getImage().getSize()>maxImageSize) {
                        ObjectError error = new ObjectError("image",messages.getMessage("message.image.size", locale) + " {"+maxSize+"}");
                        result.addError(error);
                    }

                }



            } catch (Exception e) {
                LOGGER.error("Cannot validate product image", e);
            }

        }



        if (result.hasErrors()) {
            return servletResponse;
        }

        Product newProduct = product.getProduct();
        ProductAvailability newProductAvailability = null;
        ProductPrice newProductPrice = null;

        Set<ProductPriceDescription> productPriceDescriptions = null;

        //get tax class
        //TaxClass taxClass = newProduct.getTaxClass();
        //TaxClass dbTaxClass = taxClassService.getById(taxClass.getId());
        Set<ProductPrice> prices = new HashSet<ProductPrice>();
        Set<ProductAvailability> availabilities = new HashSet<ProductAvailability>();

        if(product.getProduct().getId()!=null && product.getProduct().getId().longValue()>0) {


            //get actual product
            newProduct = productService.getById(product.getProduct().getId());
            if(newProduct!=null && newProduct.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
                return servletResponse;
            }

            //copy properties
            newProduct.setSku(product.getProduct().getSku());
            newProduct.setRefSku(product.getProduct().getRefSku());
            newProduct.setAvailable(product.getProduct().isAvailable());
            newProduct.setDateAvailable(date);
            newProduct.setManufacturer(product.getProduct().getManufacturer());
            newProduct.setType(product.getProduct().getType());
            newProduct.setProductHeight(product.getProduct().getProductHeight());
            newProduct.setProductLength(product.getProduct().getProductLength());
            newProduct.setProductWeight(product.getProduct().getProductWeight());
            newProduct.setProductWidth(product.getProduct().getProductWidth());
            newProduct.setProductVirtual(product.getProduct().isProductVirtual());
            newProduct.setProductShipeable(product.getProduct().isProductShipeable());
            newProduct.setTaxClass(product.getProduct().getTaxClass());
            newProduct.setSortOrder(product.getProduct().getSortOrder());

            Set<ProductAvailability> avails = newProduct.getAvailabilities();
            if(avails !=null && avails.size()>0) {

                for(ProductAvailability availability : avails) {
                    if(availability.getRegion().equals(com.salesmanager.core.business.constants.Constants.ALL_REGIONS)) {


                        newProductAvailability = availability;
                        Set<ProductPrice> productPrices = availability.getPrices();

                        for(ProductPrice price : productPrices) {
                            if(price.isDefaultPrice()) {
                                newProductPrice = price;
                                newProductPrice.setProductPriceAmount(submitedPrice);
                                productPriceDescriptions = price.getDescriptions();
                            } else {
                                prices.add(price);
                            }
                        }
                    } else {
                        availabilities.add(availability);
                    }
                }
            }


            for(ProductImage image : newProduct.getImages()) {
                if(image.isDefaultImage()) {
                    product.setProductImage(image);
                }
            }
        }

        if(newProductPrice==null) {
            newProductPrice = new ProductPrice();
            newProductPrice.setDefaultPrice(true);
            newProductPrice.setProductPriceAmount(submitedPrice);
        }

        if(product.getProductImage()!=null && product.getProductImage().getId() == null) {
            product.setProductImage(null);
        }

        if(productPriceDescriptions==null) {
            productPriceDescriptions = new HashSet<ProductPriceDescription>();
            for(ProductDescription description : product.getDescriptions()) {
                ProductPriceDescription ppd = new ProductPriceDescription();
                ppd.setProductPrice(newProductPrice);
                ppd.setLanguage(description.getLanguage());
                ppd.setName(ProductPriceDescription.DEFAULT_PRICE_DESCRIPTION);
                productPriceDescriptions.add(ppd);
            }
            newProductPrice.setDescriptions(productPriceDescriptions);
        }

        newProduct.setMerchantStore(store);

        if(newProductAvailability==null) {
            newProductAvailability = new ProductAvailability();
        }


        newProductAvailability.setProductQuantity(product.getAvailability().getProductQuantity());
        newProductAvailability.setProductQuantityOrderMin(product.getAvailability().getProductQuantityOrderMin());
        newProductAvailability.setProductQuantityOrderMax(product.getAvailability().getProductQuantityOrderMax());
        newProductAvailability.setProduct(newProduct);
        newProductAvailability.setPrices(prices);
        availabilities.add(newProductAvailability);

        newProductPrice.setProductAvailability(newProductAvailability);
        prices.add(newProductPrice);

        newProduct.setAvailabilities(availabilities);

        Set<ProductDescription> descriptions = new HashSet<ProductDescription>();
        if(product.getDescriptions()!=null && product.getDescriptions().size()>0) {

            for(ProductDescription description : product.getDescriptions()) {
                description.setProduct(newProduct);
                descriptions.add(description);

            }
        }

        newProduct.setDescriptions(descriptions);
        product.setDateAvailable(DateUtil.formatDate(date));



        if(product.getImage()!=null && !product.getImage().isEmpty()) {



            String imageName = product.getImage().getOriginalFilename();



            ProductImage productImage = new ProductImage();
            productImage.setDefaultImage(true);
            productImage.setImage(product.getImage().getInputStream());
            productImage.setProductImage(imageName);


            List<ProductImageDescription> imagesDescriptions = new ArrayList<ProductImageDescription>();

            for(Language l : languages) {

                ProductImageDescription imageDescription = new ProductImageDescription();
                imageDescription.setName(imageName);
                imageDescription.setLanguage(l);
                imageDescription.setProductImage(productImage);
                imagesDescriptions.add(imageDescription);

            }

            productImage.setDescriptions(imagesDescriptions);
            productImage.setProduct(newProduct);

            newProduct.getImages().add(productImage);

            //productService.saveOrUpdate(newProduct);

            //product displayed
            product.setProductImage(productImage);


        } //else {

        //productService.saveOrUpdate(newProduct);

        //}
        try {
            productService.create(newProduct);
        }catch (Exception e){
            e.printStackTrace();
        }

        ReadableProductPopulator populator = new ReadableProductPopulator();
        ReadableProduct readableProduct = new ReadableProduct();
        populator.populate(newProduct, readableProduct, store, store.getDefaultLanguage());

        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableProduct);
        setResponse(servletResponse, responseMap);
        return servletResponse;

    }

    @RequestMapping(value = "/{store}/product/{productId}", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getProduct(@PathVariable  Long productId, HttpServletRequest request, HttpServletResponse servletResponse) throws Exception{

        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();

        List<Manufacturer> manufacturers = manufacturerService.listByStore(store, language);

        List<ProductType> productTypes = productTypeService.list();

        List<TaxClass> taxClasses = taxClassService.listByStore(store);

        List<Language> languages = store.getLanguages();



        com.salesmanager.shop.admin.model.catalog.Product product = new com.salesmanager.shop.admin.model.catalog.Product();
        //Product product  = new Product();
        List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

        if(productId!=null && productId!=0) {//edit mode


            Product dbProduct = productService.getById(productId);

            if(dbProduct==null || dbProduct.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
                return null;
            }

            product.setProduct(dbProduct);
            Set<ProductDescription> productDescriptions = dbProduct.getDescriptions();

            for(Language l : languages) {

                ProductDescription productDesc = null;
                for(ProductDescription desc : productDescriptions) {

                    Language lang = desc.getLanguage();
                    if(lang.getCode().equals(l.getCode())) {
                        productDesc = desc;
                    }

                }

                if(productDesc==null) {
                    productDesc = new ProductDescription();
                    productDesc.setLanguage(l);
                }

                descriptions.add(productDesc);

            }

            for(ProductImage image : dbProduct.getImages()) {
                if(image.isDefaultImage()) {
                    product.setProductImage(image);
                    break;
                }

            }


            ProductAvailability productAvailability = null;
            ProductPrice productPrice = null;

            Set<ProductAvailability> availabilities = dbProduct.getAvailabilities();
            if(availabilities!=null && availabilities.size()>0) {

                for(ProductAvailability availability : availabilities) {
                    if(availability.getRegion().equals(com.salesmanager.core.business.constants.Constants.ALL_REGIONS)) {
                        productAvailability = availability;
                        Set<ProductPrice> prices = availability.getPrices();
                        for(ProductPrice price : prices) {
                            if(price.isDefaultPrice()) {
                                productPrice = price;
                                product.setProductPrice(priceUtil.getAdminFormatedAmount(store, productPrice.getProductPriceAmount()));
                            }
                        }
                    }
                }
            }

            if(productAvailability==null) {
                productAvailability = new ProductAvailability();
            }

            if(productPrice==null) {
                productPrice = new ProductPrice();
            }

            product.setAvailability(productAvailability);
            product.setPrice(productPrice);
            product.setDescriptions(descriptions);


            product.setDateAvailable(DateUtil.formatDate(dbProduct.getDateAvailable()));


        } else {


            for(Language l : languages) {

                ProductDescription desc = new ProductDescription();
                desc.setLanguage(l);
                descriptions.add(desc);

            }

            Product prod = new Product();

            prod.setAvailable(true);

            ProductAvailability productAvailability = new ProductAvailability();
            ProductPrice price = new ProductPrice();
            product.setPrice(price);
            product.setAvailability(productAvailability);
            product.setProduct(prod);
            product.setDescriptions(descriptions);
            product.setDateAvailable(DateUtil.formatDate(new Date()));


        }
        ReadableProductPopulator populator = new ReadableProductPopulator();
        ReadableProduct readableProduct = new ReadableProduct();
        populator.populate(product.getProduct(), readableProduct, store, store.getDefaultLanguage());

        ReadableProductPricePopulator pricePopulator = new ReadableProductPricePopulator();
        ReadableProductPrice readableProductPrice = new ReadableProductPrice();
        pricePopulator.populate(product.getPrice(),readableProductPrice,store,store.getDefaultLanguage());

        readableProduct.setOriginalPrice(readableProductPrice.getOriginalPrice());
        readableProduct.setFinalPrice(readableProductPrice.getFinalPrice());
        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableProduct);
        setResponse(servletResponse, responseMap);
        return servletResponse;
    }

    private void setMenu(Model model, HttpServletRequest request) throws Exception {

        //display menu
        Map<String,String> activeMenus = new HashMap<String,String>();
        activeMenus.put("catalogue", "catalogue");
        activeMenus.put("catalogue-products", "catalogue-products");

        @SuppressWarnings("unchecked")
        Map<String, Menu> menus = (Map<String, Menu>)request.getAttribute("MENUMAP");

        Menu currentMenu = (Menu)menus.get("catalogue");
        model.addAttribute("currentMenu",currentMenu);
        model.addAttribute("activeMenus",activeMenus);
        //
    }
    @RequestMapping(value = "/get-products", method = RequestMethod.GET)
    public @ResponseBody
    HttpServletResponse getProducts(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String categoryId = request.getParameter("categoryId");
        String sku = request.getParameter("sku");
        String available = request.getParameter("available");
        //String searchTerm = request.getParameter("searchTerm");
        String name = request.getParameter("name");

        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();
        List productListResponse = new ArrayList() ;

        try {

            ProductCriteria criteria = new ProductCriteria();
            if(!StringUtils.isBlank(categoryId) && !categoryId.equals("-1")) {

                Long lcategoryId = 0L;
                try {
                    lcategoryId = Long.parseLong(categoryId);
                } catch (Exception e) {
                    responseMap.put("meta", getMeta(0, 500, "Incorrect category id"));
                    responseMap.put("data", productListResponse);
                    setResponse(response, responseMap);
                    return response;
                }

                if(lcategoryId>0) {

                    Category category = categoryService.getById(lcategoryId);

                    if(category==null || category.getMerchantStore().getId()!=store.getId()) {
                        return response;
                    }

                    //get all sub categories
                    StringBuilder lineage = new StringBuilder();
                    lineage.append(category.getLineage()).append(category.getId()).append("/");

                    List<Category> categories = categoryService.listByLineage(store, lineage.toString());

                    List<Long> categoryIds = new ArrayList<Long>();

                    for(Category cat : categories) {
                        categoryIds.add(cat.getId());
                    }
                    categoryIds.add(category.getId());
                    criteria.setCategoryIds(categoryIds);

                }
            }

            if(!StringUtils.isBlank(sku)) {
                criteria.setCode(sku);
            }

            if(!StringUtils.isBlank(name)) {
                criteria.setProductName(name);
            }

            if(!StringUtils.isBlank(available)) {
                if(available.equals("true")) {
                    criteria.setAvailable(new Boolean(true));
                } else {
                    criteria.setAvailable(new Boolean(false));
                }
            }

            ProductList productList = productService.listByStore(store, language, criteria);
            List<Product> plist = productList.getProducts();
            ReadableProductPopulator populator = new ReadableProductPopulator();
            ReadableProduct readableProduct = new ReadableProduct();

            if(plist!=null) {

                for(Product product : plist) {
                    populator.populate(product, readableProduct, store, store.getDefaultLanguage());
                    Map entry = new HashMap();
                    entry.put("productId", readableProduct.getId());
                    entry.put("name", readableProduct.getDescription().getName());
                    entry.put("sku", readableProduct.getSku());
                    entry.put("available", readableProduct.isAvailable());
                    productListResponse.add(entry);




                }

            }

        } catch (Exception e) {
            responseMap.put("meta", getMeta(0, 500, "Error while fetching product list"));
            responseMap.put("data", productListResponse);
            setResponse(response, responseMap);
            return response;

        }
        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", productListResponse);
        setResponse(response, responseMap);
        return response;

    }

    @RequestMapping(value = "/delete-product", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse deleteProduct(HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception{
        String sid = request.getParameter("productId");
        MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();
        HashMap<String, Object> responseMap = new HashMap<>();
        try {

            Long id = Long.parseLong(sid);

            Product product = productService.getById(id);

            if(product==null || product.getMerchantStore().getId()!=store.getId()) {

                responseMap.put("meta", getMeta(0, 500, "Product with id: " + id + "is not present"));
                setResponse(response, responseMap);
                return response;

            } else {

                productService.delete(product);
                responseMap.put("meta", getMeta(0, 200, "Product is deleted"));
                setResponse(response, responseMap);
                return response;

            }


        } catch (Exception e) {
            LOGGER.error("Error while deleting product", e);
            responseMap.put("meta", getMeta(0, 200, e.toString()));
            setResponse(response, responseMap);
            return response;
        }

    }
}
