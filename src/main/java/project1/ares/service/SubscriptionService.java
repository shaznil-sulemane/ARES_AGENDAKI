package project1.ares.service;

import org.springframework.stereotype.Service;
import project1.ares.model.Company;
import project1.ares.repository.CompanyRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Service
public class SubscriptionService {

    private final CompanyRepository companyRepository;

    public SubscriptionService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Flux<Void> deactivateExpiredPlans() {
        return companyRepository.findByPlanEndDateBeforeAndActiveTrue(LocalDate.now())
                .flatMap(company -> {
                    company.setActive(false);
                    company.setPlan(new Company.Plan());
                    return companyRepository.save(company).then();
                });
    }
}
