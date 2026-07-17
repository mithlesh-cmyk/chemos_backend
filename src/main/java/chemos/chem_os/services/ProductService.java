package chemos.chem_os.services;

import chemos.chem_os.dto.ProductDropdownResponse;
import chemos.chem_os.mapper.ProductMapper;
import chemos.chem_os.model.Products;
import chemos.chem_os.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public Page<ProductDropdownResponse> searchProducts(String query, int page, int size) {

        String cleanQuery = (query == null)
                ? ""
                : query.trim().replaceAll("\\s+", " ").toLowerCase();

        Page<Products> productsPage =
                productRepository.searchProducts(
                        cleanQuery,
                        PageRequest.of(page, size)
                );

        return productsPage.map(productMapper::toDropDownResponse);
    }
}
