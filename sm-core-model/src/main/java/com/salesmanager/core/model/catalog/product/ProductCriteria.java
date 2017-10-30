package com.salesmanager.core.model.catalog.product;

import com.salesmanager.core.model.catalog.product.attribute.AttributeCriteria;
import com.salesmanager.core.model.common.Criteria;

import java.util.List;

public class ProductCriteria extends Criteria {
	
	
	private String productName;
	private List<AttributeCriteria> attributeCriteria;

	
	private Boolean available = null;
	
	private List<Long> categoryIds;
	private List<String> availabilities;
	private List<Long> productIds;
	
	private Long manufacturerId = null;
	private String refSku = null;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}


	public List<Long> getCategoryIds() {
		return categoryIds;
	}

	public void setCategoryIds(List<Long> categoryIds) {
		this.categoryIds = categoryIds;
	}

	public List<String> getAvailabilities() {
		return availabilities;
	}

	public void setAvailabilities(List<String> availabilities) {
		this.availabilities = availabilities;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}

	public void setAttributeCriteria(List<AttributeCriteria> attributeCriteria) {
		this.attributeCriteria = attributeCriteria;
	}

	public List<AttributeCriteria> getAttributeCriteria() {
		return attributeCriteria;
	}

	public void setProductIds(List<Long> productIds) {
		this.productIds = productIds;
	}

	public List<Long> getProductIds() {
		return productIds;
	}

	public void setManufacturerId(Long manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	public Long getManufacturerId() {
		return manufacturerId;
	}


	public String getRefSku() {
		return refSku;
	}

	public void setRefSku(String refSku) {
		this.refSku = refSku;
	}
}
