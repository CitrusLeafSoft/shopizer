package com.salesmanager.shop.store.services.product;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.catalog.product.relationship.ProductRelationshipService;
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService;
import com.salesmanager.core.business.services.tax.TaxClassService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.ProductCriteria;
import com.salesmanager.core.model.catalog.product.ProductList;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.image.ProductImageDescription;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription;
import com.salesmanager.core.model.catalog.product.relationship.ProductRelationship;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.admin.controller.products.ProductController;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.ReadableImage;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductPrice;
import com.salesmanager.shop.populator.catalog.ReadableCategoryPopulator;
import com.salesmanager.shop.populator.catalog.ReadableProductPopulator;
import com.salesmanager.shop.populator.catalog.ReadableProductPricePopulator;
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.ImageFilePath;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping(value = "/api")
public class ProductApiController extends BaseApiController {

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

    @Inject
    @Qualifier("img")
    private ImageFilePath imageUtils;
    @Inject
    private PricingService pricingService;
    @Inject
    ProductRelationshipService productRelationshipService;


    @RequestMapping(value = "/{store}/products", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse saveProduct(@Valid @ModelAttribute("product") com.salesmanager.shop.admin.model.catalog.Product productWrapper,
                                           BindingResult result, HttpServletRequest request,
                                           HttpServletResponse response, Locale locale) throws Exception {

        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);
        Language language = (Language) request.getAttribute(Constants.LANGUAGE);
        ReadableCategory readableCategory=null;
        List<Language> languages = store.getLanguages();

        Product existingProduct = productService.getByCode(productWrapper.getProduct().getSku(), language);

        if (productWrapper.getProduct().getId() == null && existingProduct != null) {
            setResponse(response, getErrorResponse(getMeta(400, 400, "SKU already exists")));
            return response;
        }

        //validate price
        BigDecimal submittedPrice = null;
        try {
            submittedPrice = priceUtil.getAmount(productWrapper.getProductPrice());
        } catch (Exception e) {
            ObjectError error = new ObjectError("productPrice", messages.getMessage("NotEmpty.product.productPrice", locale));
            result.addError(error);
        }
        Date date = new Date();
        if (!StringUtils.isBlank(productWrapper.getDateAvailable())) {
            try {
                date = DateUtil.getDate(productWrapper.getDateAvailable());
                productWrapper.getAvailability().setProductDateAvailable(date);
                productWrapper.setDateAvailable(DateUtil.formatDate(date));
            } catch (Exception e) {
                ObjectError error = new ObjectError("dateAvailable", messages.getMessage("message.invalid.date", locale));
                result.addError(error);
            }
        }


        if (result.hasErrors()) {
            HashMap errorResponse = new HashMap();
            StringBuilder message = new StringBuilder();
            List<ObjectError> allErrors = result.getAllErrors();
            for (ObjectError error : allErrors) {
                message.append(" ").append(error.getDefaultMessage());
            }
            errorResponse.put("meta", getMeta(400, 400, message.toString()));
            errorResponse.put("data", new HashMap());
            setResponse(response, errorResponse);
            return response;
        }

        Product newProduct = productWrapper.getProduct();
        ProductAvailability newProductAvailability = null;
        ProductPrice newProductPrice = null;

        Set<ProductPriceDescription> productPriceDescriptions = null;

        Set<ProductPrice> prices = new HashSet<ProductPrice>();
        Set<ProductAvailability> availabilities = new HashSet<ProductAvailability>();

        //Edit mode
        if (productWrapper.getProduct().getId() != null && productWrapper.getProduct().getId().longValue() > 0) {
            //get actual product
            newProduct = productService.getById(productWrapper.getProduct().getId());
            if (newProduct != null && newProduct.getMerchantStore().getId().intValue() != store.getId().intValue()) {
                setResponse(response, getErrorResponse(getMeta(400, 400, "Invalid Product ID")));
                return response;
            }

            //copy properties
            newProduct.setSku(productWrapper.getProduct().getSku());
            newProduct.setRefSku(productWrapper.getProduct().getRefSku());
            newProduct.setAvailable(productWrapper.getProduct().isAvailable());
            newProduct.setDateAvailable(date);
            newProduct.setManufacturer(productWrapper.getProduct().getManufacturer());
            newProduct.setType(productWrapper.getProduct().getType());
            newProduct.setProductHeight(productWrapper.getProduct().getProductHeight());
            newProduct.setProductLength(productWrapper.getProduct().getProductLength());
            newProduct.setProductWeight(productWrapper.getProduct().getProductWeight());
            newProduct.setProductWidth(productWrapper.getProduct().getProductWidth());
            newProduct.setProductVirtual(productWrapper.getProduct().isProductVirtual());
            newProduct.setProductShipeable(productWrapper.getProduct().isProductShipeable());
            newProduct.setTaxClass(productWrapper.getProduct().getTaxClass());
            newProduct.setSortOrder(productWrapper.getProduct().getSortOrder());

            Set<ProductAvailability> avails = newProduct.getAvailabilities();
            if (avails != null && avails.size() > 0) {

                for (ProductAvailability availability : avails) {
                    if (availability.getRegion().equals(com.salesmanager.core.business.constants.Constants.ALL_REGIONS)) {


                        newProductAvailability = availability;
                        Set<ProductPrice> productPrices = availability.getPrices();

                        for (ProductPrice price : productPrices) {
                            if (price.isDefaultPrice()) {
                                newProductPrice = price;
                                newProductPrice.setProductPriceAmount(submittedPrice);
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


            for (ProductImage image : newProduct.getImages()) {
                if (image.isDefaultImage()) {
                    productWrapper.setProductImage(image);
                }
            }
        }

        if (newProductPrice == null) {
            newProductPrice = new ProductPrice();
            newProductPrice.setDefaultPrice(true);
            newProductPrice.setProductPriceAmount(submittedPrice);
        }

        if (productWrapper.getProductImage() != null && productWrapper.getProductImage().getId() == null) {
            productWrapper.setProductImage(null);
        }

        if (productPriceDescriptions == null) {
            productPriceDescriptions = new HashSet<>();
            for (ProductDescription description : productWrapper.getDescriptions()) {
                ProductPriceDescription ppd = new ProductPriceDescription();
                ppd.setProductPrice(newProductPrice);
                ppd.setLanguage(description.getLanguage());
                ppd.setName(ProductPriceDescription.DEFAULT_PRICE_DESCRIPTION);
                productPriceDescriptions.add(ppd);
            }
            newProductPrice.setDescriptions(productPriceDescriptions);
        }

        newProduct.setMerchantStore(store);

        if (newProductAvailability == null) {
            newProductAvailability = new ProductAvailability();
        }


        newProductAvailability.setProductQuantity(productWrapper.getAvailability().getProductQuantity());
        newProductAvailability.setProductQuantityOrderMin(productWrapper.getAvailability().getProductQuantityOrderMin());
        newProductAvailability.setProductQuantityOrderMax(productWrapper.getAvailability().getProductQuantityOrderMax());
        newProductAvailability.setProduct(newProduct);
        newProductAvailability.setPrices(prices);
        availabilities.add(newProductAvailability);

        newProductPrice.setProductAvailability(newProductAvailability);
        prices.add(newProductPrice);

        newProduct.setAvailabilities(availabilities);

        Set<ProductDescription> descriptions = new HashSet<ProductDescription>();
        if (productWrapper.getDescriptions() != null && productWrapper.getDescriptions().size() > 0) {

            for (ProductDescription description : productWrapper.getDescriptions()) {
                description.setProduct(newProduct);
                descriptions.add(description);

            }
        }

        newProduct.setDescriptions(descriptions);
        productWrapper.setDateAvailable(DateUtil.formatDate(date));
        ProductImage productImage = null;

        if (productWrapper.getImage() != null && !productWrapper.getImage().isEmpty()) {


            String imageName = productWrapper.getImage().getOriginalFilename();

            productImage = new ProductImage();

            productImage.setDefaultImage(true);
            productImage.setImage(productWrapper.getImage().getInputStream());
            productImage.setProductImage(imageName);

            List<ProductImageDescription> imagesDescriptions = new ArrayList<ProductImageDescription>();

            for (Language l : languages) {

                ProductImageDescription imageDescription = new ProductImageDescription();
                imageDescription.setName(imageName);
                imageDescription.setLanguage(l);
                imageDescription.setProductImage(productImage);
                imagesDescriptions.add(imageDescription);

            }

            productImage.setDescriptions(imagesDescriptions);
            productImage.setProduct(newProduct);

            newProduct.getImages().add(productImage);

            //product displayed
            productWrapper.setProductImage(productImage);

        }
        try {
            if(productImage != null) {
                boolean uploadedToS3 = uploadImage(productWrapper.getImage());
                if(uploadedToS3) {
                    final String url = ShopApplication.amazonS3Client.getUrl("shopizer-cl-2",
                            productWrapper.getProductImage().getProductImage()).toString();
                    newProduct.getProductImage().setProductImageUrl(url);
                }
            }
            productService.create(newProduct);
            long categoryId = Long.parseLong(request.getParameter("category_id"));
            Category category = categoryService.getById(categoryId);
            newProduct.getCategories().add(category);
            readableCategory = new ReadableCategory();
            ReadableCategoryPopulator readableCategoryPopulator = new ReadableCategoryPopulator();
            readableCategoryPopulator.populate(category,readableCategory,store,store.getDefaultLanguage());
            //readableCategory.setId(categoryId);
            productService.update(newProduct);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String groupCode = newProduct.getSku() + "-related";
        ProductRelationship relatedProducts = null;

        List<ProductRelationship> groups = productRelationshipService.getGroups(store);
        for(ProductRelationship grp : groups) {
            if(grp.getCode().equalsIgnoreCase(groupCode)) {
                relatedProducts = grp;
            }
        }

        if(relatedProducts == null) {
            //create a product relationship for related products
            relatedProducts = new ProductRelationship();
            relatedProducts.setCode(groupCode);
            relatedProducts.setActive(true);
            relatedProducts.setStore(store);

            productRelationshipService.addGroup(store, relatedProducts.getCode());
        }

        ReadableProductPopulator populator = new ReadableProductPopulator();
        populator.setimageUtils(imageUtils);
        populator.setPricingService(pricingService);
        ReadableProduct readableProduct = new ReadableProduct();
        populator.populate(newProduct, readableProduct, store, store.getDefaultLanguage());

        ReadableImage readableImage = new ReadableImage();
        String url = "";
        ProductImage defaultImage = newProduct.getProductImage();
        if(defaultImage != null) {
            url = defaultImage.getProductImageUrl();
        }
        else {
            url  = ShopApplication.amazonS3Client.getUrl("shopizer-cl-2",
                    newProduct.getProductImage().getProductImage()).toString();
        }

        readableImage.setImageUrl(url);
        readableProduct.setImage(readableImage);
        if(readableCategory!=null)
            readableProduct.setReadableCategory(readableCategory);

        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableProduct);
        setResponse(response, responseMap);
        return response;

    }

    @RequestMapping(value = "/{store}/products/{productId}", method = RequestMethod.GET)
    @ResponseBody
    public HttpServletResponse getProduct(@PathVariable Long productId,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {

        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();

        List<Language> languages = store.getLanguages();


        com.salesmanager.shop.admin.model.catalog.Product product = new com.salesmanager.shop.admin.model.catalog.Product();
        //Product product  = new Product();
        List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

        if (productId != null && productId != 0) {//edit mode


            Product dbProduct = productService.getById(productId);

            if (dbProduct == null || dbProduct.getMerchantStore().getId().intValue() != store.getId().intValue()) {
                responseMap = getErrorResponse(getMeta(404, 404, "Product Not Found"));
                setResponse(response, responseMap);
                return response;
            }

            product.setProduct(dbProduct);
            Set<ProductDescription> productDescriptions = dbProduct.getDescriptions();

            for (Language l : languages) {

                ProductDescription productDesc = null;
                for (ProductDescription desc : productDescriptions) {

                    Language lang = desc.getLanguage();
                    if (lang.getCode().equals(l.getCode())) {
                        productDesc = desc;
                    }

                }

                if (productDesc == null) {
                    productDesc = new ProductDescription();
                    productDesc.setLanguage(l);
                }

                descriptions.add(productDesc);

            }

            for (ProductImage image : dbProduct.getImages()) {
                if (image.isDefaultImage()) {
                    product.setProductImage(image);
                    break;
                }

            }


            ProductAvailability productAvailability = null;
            ProductPrice productPrice = null;

            Set<ProductAvailability> availabilities = dbProduct.getAvailabilities();
            if (availabilities != null && availabilities.size() > 0) {

                for (ProductAvailability availability : availabilities) {
                    if (availability.getRegion().equals(com.salesmanager.core.business.constants.Constants.ALL_REGIONS)) {
                        productAvailability = availability;
                        Set<ProductPrice> prices = availability.getPrices();
                        for (ProductPrice price : prices) {
                            if (price.isDefaultPrice()) {
                                productPrice = price;
                                product.setProductPrice(priceUtil.getAdminFormatedAmount(store, productPrice.getProductPriceAmount()));
                            }
                        }
                    }
                }
            }

            if (productAvailability == null) {
                productAvailability = new ProductAvailability();
            }

            if (productPrice == null) {
                productPrice = new ProductPrice();
            }

            product.setAvailability(productAvailability);
            product.setPrice(productPrice);
            product.setDescriptions(descriptions);


            product.setDateAvailable(DateUtil.formatDate(dbProduct.getDateAvailable()));


        } else {


            for (Language l : languages) {

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
        populator.setPricingService(pricingService);
        populator.setimageUtils(imageUtils);
        ReadableProduct readableProduct = new ReadableProduct();
        populator.populate(product.getProduct(), readableProduct, store, store.getDefaultLanguage());

        ReadableProductPricePopulator pricePopulator = new ReadableProductPricePopulator();
        pricePopulator.setPricingService(pricingService);
        ReadableProductPrice readableProductPrice = new ReadableProductPrice();
        pricePopulator.populate(product.getPrice(), readableProductPrice, store, store.getDefaultLanguage());

        //TODO:
        //1. Get quantity from PRODUCT_STORE_CL
        //2. Update price based on discount given in PRODUCT_STORE_CL

        final String quantity = productService.getQuantity(product.getProduct(), store);
        final String discount = productService.getDiscount(product.getProduct(), store);

        if(quantity != null) {
            readableProduct.setQuantity(Integer.parseInt(quantity));
        }
        else {
            readableProduct.setQuantity(0);
        }

        ReadableImage readableImage = new ReadableImage();

        ProductImage defaultImage = product.getProductImage();
        String url = "";
        if(defaultImage != null) {
            url = defaultImage.getProductImageUrl();
        }
        else {
            url  = ShopApplication.amazonS3Client.getUrl("shopizer-cl-2",
                    product.getProductImage().getProductImage()).toString();
        }

        if(url.contains("static")) url = "";

        /*if(product.getProductImage() != null && product.getProductImage().getProductImageUrl() != null) {
            imageUrl = product.getProductImage().getProductImageUrl();
        }
        else {
            imageUrl = "";
        }*/
        readableImage.setImageUrl(url);
        readableProduct.setImage(readableImage);

        if(discount != null) {
            final float originalPrice = Float.parseFloat(readableProductPrice.getOriginalPrice());
            final float discountFloat = Float.parseFloat(discount);
            final float finalPrice = (1- (discountFloat/100)) * originalPrice;
            readableProduct.setFinalPrice(finalPrice + "");
        }


        /*readableProduct.setOriginalPrice(readableProductPrice.getOriginalPrice());
        readableProduct.setFinalPrice(readableProductPrice.getFinalPrice());*/
        responseMap.put("meta", getMeta(0, 200, ""));
        responseMap.put("data", readableProduct);
        setResponse(response, responseMap);
        return response;
    }


    @RequestMapping(value = "/{store}/products", method = RequestMethod.GET)
    public @ResponseBody
    HttpServletResponse getProducts(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String categoryId = request.getParameter("category_id");
        String sku = request.getParameter("sku");
        String refSku = request.getParameter("reference_sku");
        String available = request.getParameter("available");
        //String searchTerm = request.getParameter("searchTerm");
        String name = request.getParameter("name");

        HashMap<String, Object> responseMap = new HashMap<>();
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();
        List productListResponse = new ArrayList();

        try {

            ProductCriteria criteria = new ProductCriteria();
            if (!StringUtils.isBlank(categoryId)) {

                Long lcategoryId = 0L;
                try {
                    lcategoryId = Long.parseLong(categoryId);
                } catch (Exception e) {
                    responseMap.put("meta", getMeta(0, 500, "Incorrect category id"));
                    responseMap.put("data", productListResponse);
                    setResponse(response, responseMap);
                    return response;
                }

                if (lcategoryId >= -1) {

                    Category category = categoryService.getById(lcategoryId);

                    if (category == null || category.getMerchantStore().getId() != store.getId()) {
                        return response;
                    }

                    //get all sub categories
                    StringBuilder lineage = new StringBuilder();
                    lineage.append(category.getLineage()).append(category.getId()).append("/");

                    List<Category> categories = categoryService.listByLineage(store, lineage.toString());

                    List<Long> categoryIds = new ArrayList<Long>();

                    for (Category cat : categories) {
                        categoryIds.add(cat.getId());
                    }
                    categoryIds.add(category.getId());
                    criteria.setCategoryIds(categoryIds);

                }
            }

            if (!StringUtils.isBlank(sku)) {
                criteria.setCode(sku);
            }

            if (!StringUtils.isBlank(refSku)) {
                criteria.setRefSku(refSku);
            }

            if (!StringUtils.isBlank(name)) {
                criteria.setProductName(name);
            }

            if (!StringUtils.isBlank(available)) {
                if (available.equals("true")) {
                    criteria.setAvailable(new Boolean(true));
                } else {
                    criteria.setAvailable(new Boolean(false));
                }
            }

            ProductList productList = productService.listByStore(store, language, criteria);
            List<Product> plist = productList.getProducts();
            ReadableProductPopulator populator = new ReadableProductPopulator();
            populator.setPricingService(pricingService);
            populator.setimageUtils(imageUtils);
            ReadableProduct readableProduct = new ReadableProduct();

            if (plist != null) {
                String url = "";
                for (Product product : plist) {
                    populator.populate(product, readableProduct, store, store.getDefaultLanguage());
                    Map entry = new HashMap();
                    entry.put("productId", readableProduct.getId());
                    entry.put("name", readableProduct.getDescription().getName());
                    entry.put("sku", readableProduct.getSku());
                    entry.put("available", readableProduct.isAvailable());
                    entry.put("quantity", readableProduct.getQuantity());
                    entry.put("price", readableProduct.getPrice());

                    if(product.getProductImage() != null && product.getProductImage().getProductImageUrl() != null) {
                        url = product.getProductImage().getProductImageUrl();
                    }
                    else {
                        url = "";
                    }
                    entry.put("image", url);
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

    @RequestMapping(value = "/{store}/products/delete", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse deleteProduct(HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
        String sid = request.getParameter("productId");
        MerchantStore store = (MerchantStore) request.getAttribute(Constants.ADMIN_STORE);
        Language language = store.getDefaultLanguage();
        HashMap<String, Object> responseMap = new HashMap<>();
        try {

            Long id = Long.parseLong(sid);

            Product product = productService.getById(id);

            if (product == null || product.getMerchantStore().getId() != store.getId()) {

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

    private boolean uploadImage(MultipartFile file){
        try {

            if (!file.isEmpty()) {
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentType(file.getContentType());
                final PutObjectRequest putObjectRequest = new PutObjectRequest("shopizer-cl-2",
                        file.getOriginalFilename(),
                        file.getInputStream(),
                        objectMetadata).
                        withCannedAcl(CannedAccessControlList.PublicRead);
                ShopApplication.amazonS3Client.putObject(putObjectRequest);
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
