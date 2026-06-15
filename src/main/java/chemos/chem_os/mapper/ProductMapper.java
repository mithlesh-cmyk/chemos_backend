package chemos.chem_os.mapper;

import chemos.chem_os.dto.ProductDropdownResponse;
import chemos.chem_os.model.Products;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductDropdownResponse toDropDownResponse(Products products){

        return new ProductDropdownResponse(
                products.getId(),
                products.getName()
        );
    }
}
