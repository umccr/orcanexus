package dev.ccgcm.platform.orcanexus.config;

import dev.ccgcm.platform.orcanexus.domain.port.LimsQueryPort;
import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsService;
import dev.ccgcm.platform.orcanexus.domain.service.FindLimsRecordsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

  @Bean
  FindLimsRecordsUseCase findLimsRecordsUseCase(LimsQueryPort limsQueryPort) {
    return new FindLimsRecordsService(limsQueryPort);
  }
}
