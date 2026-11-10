package dev.ccgcm.platform.orcanexus.aurora.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DatasourcePropertiesBindingTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(DatasourceProperties.class)
  static class PropertiesConfig {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

  @Test
  void bindsConfiguredSchemaMappings() {
    runner
        .withPropertyValues(
            "orcanexus.datasource.schema-mappings[0].input=generated_a",
            "orcanexus.datasource.schema-mappings[0].output=physical_a",
            "orcanexus.datasource.schema-mappings[1].input=generated_b",
            "orcanexus.datasource.schema-mappings[1].output=physical_b")
        .run(
            context -> {
              var mappings = context.getBean(DatasourceProperties.class).getSchemaMappings();

              assertThat(mappings).hasSize(2);
              assertThat(mappings.get(0).getInput()).isEqualTo("generated_a");
              assertThat(mappings.get(0).getOutput()).isEqualTo("physical_a");
              assertThat(mappings.get(1).getInput()).isEqualTo("generated_b");
              assertThat(mappings.get(1).getOutput()).isEqualTo("physical_b");
            });
  }

  @Test
  void defaultsToNoMappings() {
    runner.run(
        context ->
            assertThat(context.getBean(DatasourceProperties.class).getSchemaMappings()).isEmpty());
  }

  @Test
  void exposedListCannotBeUsedToModifyInternalState() {
    var properties = new DatasourceProperties();
    properties.setSchemaMappings(List.of(new DatasourceProperties.SchemaMapping()));

    assertThatThrownBy(() -> properties.getSchemaMappings().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void laterChangesToTheSuppliedListDoNotAffectTheProperties() {
    var supplied = new ArrayList<DatasourceProperties.SchemaMapping>();
    supplied.add(new DatasourceProperties.SchemaMapping());

    var properties = new DatasourceProperties();
    properties.setSchemaMappings(supplied);
    supplied.clear();

    assertThat(properties.getSchemaMappings()).hasSize(1);
  }
}
