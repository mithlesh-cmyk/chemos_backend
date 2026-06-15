package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSaleRequest;
import chemos.chem_os.mapper.SalesMapper;
import chemos.chem_os.model.Sales;
import chemos.chem_os.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final SalesMapper salesMapper;

    public Sales createSale(CreateSaleRequest request){
        Sales sales = salesMapper.toEntity(request);
        return salesRepository.save(sales);
    }

    public List<Sales> getAllSales() {
        return salesRepository.findAll();
    }

    public Sales getSaleById(String id) {
        return salesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + id
                ));
    }
}
