package tech.goksi.bug;

import com.fasterxml.jackson.databind.ObjectMapper;
import tech.goksi.bug.util.ObjectMapperSupplier;

public class ExampleMigration {


  public ObjectMapperSupplier objectMapperSupplier() {
    ObjectMapper objectMapper = new ObjectMapper();

    return () -> objectMapper;
  }
}
