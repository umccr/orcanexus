package dev.ccgcm.platform.orcanexus.aurora.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "orcanexus.datasource")
public class DatasourceProperties {

  /**
   * Schema name mappings applied to the jOOQ runtime.
   *
   * <p>The generated jOOQ model is bound to whatever schema name was present when it was generated.
   * When the physical database schema is named differently from the generated one, each entry
   * rewrites the generated ({@code input}) schema name to the physical ({@code output}) schema name
   * at query-render time, so the model can be reused without regeneration.
   */
  private List<SchemaMapping> schemaMappings = new ArrayList<>();

  public List<SchemaMapping> getSchemaMappings() {
    return schemaMappings;
  }

  public void setSchemaMappings(List<SchemaMapping> schemaMappings) {
    this.schemaMappings = schemaMappings;
  }

  /** A single schema name mapping from the generated model name to the physical database name. */
  public static class SchemaMapping {

    /** Schema name as it appears in the generated jOOQ model. */
    private String input;

    /** Schema name the query should physically be executed against. */
    private String output;

    public String getInput() {
      return input;
    }

    public void setInput(String input) {
      this.input = input;
    }

    public String getOutput() {
      return output;
    }

    public void setOutput(String output) {
      this.output = output;
    }
  }
}
