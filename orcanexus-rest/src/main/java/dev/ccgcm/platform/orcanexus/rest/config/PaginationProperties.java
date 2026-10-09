package dev.ccgcm.platform.orcanexus.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "orcanexus.pagination")
public class PaginationProperties {

  private int defaultPage = 0;
  private int defaultSize = 10;
  private int maxSize = 1000;

  public int getDefaultPage() {
    return defaultPage;
  }

  public void setDefaultPage(int defaultPage) {
    this.defaultPage = defaultPage;
  }

  public int getDefaultSize() {
    return defaultSize;
  }

  public void setDefaultSize(int defaultSize) {
    this.defaultSize = defaultSize;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }
}
