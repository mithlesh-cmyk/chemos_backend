package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.mapper.SalesMapper;
import chemos.chem_os.model.Sales;
import chemos.chem_os.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final SalesMapper salesMapper;

    public Sales createSale(CreateSaleRequest request){
        Sales sales = salesMapper.toEntity(request);
        return salesRepository.save(sales);
    }

    public Page<Sales> getAllSales(Pageable pageable) {
        return salesRepository.findAll(pageable);
    }

    public Sales getSaleById(String id) {
        return salesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + id
                ));
    }
}
