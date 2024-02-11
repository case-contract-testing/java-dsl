package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_boundary.BoundaryMockDefinition;
import io.contract_testing.contractcase.case_example_mock_types.mocks.base.AnyMockDescriptor;

class BoundaryDefinitionMapper {

  static <T extends AnyMockDescriptor> BoundaryMockDefinition  map(ExampleDefinition<T> definition) {
    return new BoundaryMockDefinition.Builder().definition(definition.getDefinition())
        .states(definition.getStates()).build();
  }
}
