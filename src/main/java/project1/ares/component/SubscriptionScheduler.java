package project1.ares.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import project1.ares.service.SubscriptionService;

import java.util.concurrent.TimeUnit;

@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // roda todo dia à meia-noite
//    @Scheduled(cron = "0 0 0 * * ?")
    @Scheduled(timeUnit = TimeUnit.HOURS, fixedRate = 12)
    public void checkExpiredPlans() {
        subscriptionService.deactivateExpiredPlans()
                .collectList()
                .subscribe(users -> {
                    System.out.println("✅ Planos expirados processados: " + users.size());
                });
    }
}

