package io.contract_testing.contractcase;

import io.contract_testing.contractcase.case_example_mock_types.AnyMockDescriptor;
import java.util.List;

public class ExampleDefinition<M extends AnyMockDescriptor> {

  private final List<? extends Object> states;
  private final M definition;

  public ExampleDefinition(List<? extends Object> states, M definition) {
    this.states = states;
    this.definition = definition;
  }

  Object getDefinition() {
    return definition;
  }

  public List<? extends Object> getStates() {
    return states;
  }
}
