package io.contract_testing.contractcase;

import java.util.List;

class BoundaryVersionGenerator {


  List<String> getVersions() {
    var version = getClass().getPackage().getImplementationVersion();
    return List.of("Java-DSL@" + (version != null ? version : "UNKNOWN"));
  }

}
