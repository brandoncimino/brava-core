package brava.core.collections;

import brava.core.Either;
import brava.core.Unchecked;
import com.sun.net.httpserver.Authenticator;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ImmutablyTests {
    private static <T> void assert_similarResults(
          Either<? extends T, ? extends Throwable> actual,
          Either<? extends T, ? extends Throwable> expected
    ){
        expected.handle(
              success -> Assertions.assertThat(actual)
                    .as("Should have successfully returned %s", expected)
                    .extracting(Either::getA)
                    .isEqualTo(success),
              exception -> Assertions.assertThat(actual)
                    .as("Should have thrown a %s", exception.getClass())
                    .extracting(Either::getB)
                    .hasSameClassAs(exception)
        );
    }
    
    record AddScenario<T>(List<T> originalList, T addedElement){ }
    
    static Stream<AddScenario<String>> addScenarios(){
        return Stream.of(
              new AddScenario<>(new ArrayList<>(), "added"),
              new AddScenario<>(Lists.newArrayList("1"), "added"),
              new AddScenario<>(Lists.newArrayList("1", "2", "3"), "added")
        );
    }
    
    @ParameterizedTest
    @MethodSource("addScenarios")
    void immutablyAdd(AddScenario<String> scenario){
        var originalListState = List.copyOf(scenario.originalList);
        
        var mutableCopy = new ArrayList<>(scenario.originalList);
        var expected = Either.resultOf(() -> {
            mutableCopy.add(scenario.addedElement);
            return mutableCopy;
        });
        
        var actual = Either.resultOf(() -> Immutably.add(scenario.originalList, scenario.addedElement));

        assert_similarResults(actual, expected);
        Assertions.assertThat(scenario.originalList)
              .as("The original list should not be modified")
              .isEqualTo(originalListState);
    }
    
    public record InsertScenario<T>(
          List<T> originalList,
          T insertedElement,
          int insertAt
    ){
        
    }
    
    static <T> Stream<InsertScenario<T>> createInsertScenarios(AddScenario<T> addScenario){
        return IntStream.range(-1, addScenario.originalList.size() + 1)
              .mapToObj(insertAt -> new InsertScenario<>(
                    addScenario.originalList,
                    addScenario.addedElement,
                    insertAt
              ));
    }
    
    static Stream<InsertScenario<String>> insertScenarios(){
        return addScenarios()
              .flatMap(ImmutablyTests::createInsertScenarios);
    }
    
    @ParameterizedTest
    @MethodSource("insertScenarios")
    void immutablyInsert(InsertScenario<String> scenario){
        var originalListState = List.copyOf(scenario.originalList);
        
        var mutableCopy = new ArrayList<>(scenario.originalList);
        var expected = Either.resultOf(() -> {
            mutableCopy.add(scenario.insertAt, scenario.insertedElement);
            return mutableCopy;
        });
        
        var actual = Either.resultOf(() -> Immutably.insert(scenario.originalList, scenario.insertedElement, scenario.insertAt));
        
        assert_similarResults(actual, expected);
        
        Assertions.assertThat(scenario.originalList)
              .as("The original list should not be modified")
              .isEqualTo(originalListState);
    }
    
    record PutScenario<K,V>(Map<K, V> originalMap, K keyToPut, V valueToPut){
        
    }
    
    static Stream<PutScenario<String, String>> putScenarios(){
        return Stream.of(
              new PutScenario<>(new HashMap<>(), "key_1", "new value"),
              new PutScenario<>(Map.of("key_1", "val_1"), "key_1", "replaced value"),
              new PutScenario<>(Map.of("key_1", "val_1"), "key_2", "new value")
        );
    }
    
    @ParameterizedTest
    @MethodSource("putScenarios")
    void immutablyPut(PutScenario<String, String> scenario){
        var originalState = Map.copyOf(scenario.originalMap);
        
        var expected = Either.resultOf(() -> {
            var mutableCopy = new HashMap<>(scenario.originalMap);
            mutableCopy.put(scenario.keyToPut, scenario.valueToPut);
            return mutableCopy;
        });
        
        var actual = Either.resultOf(() -> Immutably.put(scenario.originalMap, scenario.keyToPut, scenario.valueToPut));
        
        assert_similarResults(actual, expected);
        Assertions.assertThat(scenario.originalMap)
              .as("The original map should be modified")
              .isEqualTo(originalState);
    }
}
