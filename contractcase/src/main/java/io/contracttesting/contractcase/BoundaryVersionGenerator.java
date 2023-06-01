package io.contracttesting.contractcase;

import java.util.List;

class BoundaryVersionGenerator {

  // TODO: Figure out how to get versions correctly

  List<String> getVersions() {
    var version = getClass().getPackage().getImplementationVersion();
    return List.of("Java-DSL@" + (version != null ? version : "UNKNOWN"));
  }

}
