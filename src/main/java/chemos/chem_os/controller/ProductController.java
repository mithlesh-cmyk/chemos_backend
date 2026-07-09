package chemos.chem_os.controller;

import chemos.chem_os.dto.ApiSuccessResponse;
import chemos.chem_os.dto.ProductDropdownResponse;
import chemos.chem_os.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<Page<ProductDropdownResponse>>> searchProducts(
            @RequestParam(value = "query", required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductDropdownResponse> responsePage =
                productService.searchProducts(query, page, size);

        String message = responsePage.isEmpty()
                ? "No products found."
                : "Products fetched successfully.";

        return ResponseEntity.ok(
                ApiSuccessResponse.<Page<ProductDropdownResponse>>builder()
                        .message(message)
                        .data(responsePage)
                        .build()
        );
    }
}