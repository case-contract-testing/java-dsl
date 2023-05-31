package io.contracttesting.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryMockDefinition;

class BoundaryDefinitionMapper {

  static BoundaryMockDefinition map(ExampleDefinition definition) {
    return new BoundaryMockDefinition.Builder().definition(definition.getDefinition())
        .states(definition.getStates()).build();
  }
}
