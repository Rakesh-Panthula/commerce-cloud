/**
 * 
 */
package com.sncustomwebservices.facades.impl;

import de.hybris.platform.category.CategoryService;
import de.hybris.platform.category.model.CategoryModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sncustomwebservices.facades.CustomBrandFacade;
import com.sncustomwebservices.facades.data.BrandData;
import com.sncustomwebservices.service.CustomBrandService;

/**
 * @author Anand.Mund
 *
 */
public class CustomBrandFacadeImpl implements CustomBrandFacade {
	
	private CustomBrandService brandService;
	private CategoryService categoryService;
	
	@Override
	public BrandData getBrand(){
		
		final BrandData brandList = new BrandData();
		final Set<String> brandSet = new HashSet<String>();
		
		final Collection<CategoryModel> category = getCategoryService().getCategoriesForCode("brands");

		for (final CategoryModel categoryModel : category)		{
			final Collection<CategoryModel> subCategory = categoryModel.getAllSubcategories();
			for (final CategoryModel categoryModel2 : subCategory){
				brandSet.add(categoryModel2.getName());
			}
		}
		
		brandList.setBrand(brandSet);
		return brandList;
	}
	/**
	 * @return the brandService
	 */
	public CustomBrandService getBrandService(){
		return brandService;
	}
	/**
	 * @param brandService the brandService to set
	 */
	public void setBrandService(CustomBrandService brandService){
		this.brandService = brandService;
	}
	/**
	 * @return the categoryService
	 */
	public CategoryService getCategoryService(){
		return categoryService;
	}
	/**
	 * @param categoryService the categoryService to set
	 */
	public void setCategoryService(CategoryService categoryService){
		this.categoryService = categoryService;
	}
	
}
