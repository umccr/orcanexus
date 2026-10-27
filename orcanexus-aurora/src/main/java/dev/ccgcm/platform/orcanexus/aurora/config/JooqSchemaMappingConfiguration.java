package dev.ccgcm.platform.orcanexus.aurora.config;

import dev.ccgcm.platform.orcanexus.aurora.config.DatasourceProperties.SchemaMapping;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jOOQ runtime configuration.
 *
 * <p>Applies the configured schema name mappings (see {@link DatasourceProperties}) to the
 * auto-configured jOOQ {@code Configuration} via a {@link RenderMapping}. This lets the generated
 * jOOQ model, which is bound to the schema name captured at generation time, be executed against a
 * differently named physical schema without regenerating the model. The concrete names are supplied
 * entirely by configuration.
 */
@Configuration
public class JooqSchemaMappingConfiguration {

  @Bean
  DefaultConfigurationCustomizer jooqSchemaMappingCustomizer(DatasourceProperties properties) {
    return c -> {
      var mappings = properties.getSchemaMappings();
      if (mappings.isEmpty()) {
        return;
      }
      var renderMapping = new RenderMapping();
      for (SchemaMapping mapping : mappings) {
        renderMapping
            .getSchemata()
            .add(new MappedSchema().withInput(mapping.getInput()).withOutput(mapping.getOutput()));
      }
      c.settings().withRenderMapping(renderMapping);
    };
  }
}
