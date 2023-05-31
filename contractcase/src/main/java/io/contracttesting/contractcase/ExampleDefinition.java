package io.contracttesting.contractcase;

import java.util.List;

public class ExampleDefinition {

  private final List<? extends Object> states;
  private final Object definition;

  public ExampleDefinition(List<? extends Object> states, Object definition) {
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
