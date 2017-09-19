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
import com.salesmanager.core.model.catalog.product.Product;
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
import com.salesmanager.shop.store.services.BaseApiController;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
    public String saveProduct(@Valid @ModelAttribute("product") com.salesmanager.shop.admin.model.catalog.Product  product, BindingResult result, Model model, HttpServletRequest request, Locale locale) throws Exception {


       // Language language = new Language("en");//(Language)request.getAttribute("LANGUAGE");

        //display menu
        //setMenu(model,request);

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
            return "admin-products-edit";
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
                return "redirect:/admin/products/products.html";
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

        productService.create(newProduct);
        /*model.addAttribute("success","success");*/

        return new Gson().toJson(newProduct);
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

}
