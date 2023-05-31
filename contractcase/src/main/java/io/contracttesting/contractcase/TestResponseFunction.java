package io.contracttesting.contractcase;

public interface TestResponseFunction<T> {

  void call(T returnedObject);
}
