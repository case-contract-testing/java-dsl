package io.contract_testing.contractcase;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.contract_testing.contractcase.case_example_mock_types.base.AnyMockDescriptor;
import java.util.List;
import java.util.function.Function;

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

  public JsonNode toJSON() {
    var mapper = new ObjectMapper();
    ObjectNode node = mapper.createObjectNode();
    node.set("definition", mapper.valueToTree(definition.toJSON()));
    node.set("states",
        mapper.createArrayNode()
            .addAll(this.states.stream()
                .map((Function<Object, JsonNode>) mapper::valueToTree)
                .toList()));

    return node;

  }


}
