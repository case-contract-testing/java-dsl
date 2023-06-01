package io.contract_testing.contractcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MainTest {

  @Test
  void testSomeThingBoom() {
    var c = new ContractCaseTest();

    c.printAndExplode(Map.of("key", "value", "otherKey", "otherValue"));
  }

  @Test
  void testCallback() {
    var c = new ContractCaseTest();

    var b = new CallbackImpl();

    c.requestCallback(b);
  }

  @Test
  void testSecondCallback() {
    var c = new ContractCaseTest();

    c.doubleCallback();
  }

  record FooRecord(String s, int i, List<String> A) {

  }

  @Test
  void testJson() {
    var c = new ContractCaseTest();

    c.jsonSpike("Map", Map.of("key", "value", "otherKey", "otherValue"));
    c.jsonSpike("Map of Maps",
        Map.of(
            "key", "value",
            "otherKey", Map.of(
                "subkey", "subvalue",
                "subotherKey", new CoreNumberMatcher(2))));

    c.jsonSpike("array", List.of("1", "2", "3").toArray());
    c.jsonSpike("list", List.of("1", "2", "3"));
    c.jsonSpike("random object", new FooRecord("s", 1, Arrays.asList("1", "2")));
  }


  @Test
  void testAsyncCallback() {
    var c = new ContractCaseTest();

    var b = new CallbackImpl();

    c.requestAsyncCallback(b);
  }

  @Test
  void testCallbackExploding() {
    var c = new ContractCaseTest();

    var b = new CallbackExplodingImpl();

    c.requestCallback(b);
  }

  @Test
  void testSomeThing() {
    var c = new ContractCaseTest();

    c.print("Passed from java");
    try {
      c.printAndExplode(Map.of("key", "value", "otherKey", "otherValue"));
    } catch (Exception e) {
      System.out.print("\033[1;34m" + e.getMessage() + "\033[0m\n");
    }

    assertEquals("foo", "foo");
  }

  @Test
  void testValidate() {
    var c = new ContractCaseTest();
    var b = new CallbackImpl();
    c.requestTrigger(b);
  }
}
