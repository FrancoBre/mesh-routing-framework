package org.ungs.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tuple")
class TupleTest {

  @Nested
  @DisplayName("Value Storage")
  class ValueStorage {

    @Test
    @DisplayName("should store first value correctly")
    void storesFirstValue() {
      Tuple<String, Integer> tuple = new Tuple<>("hello", 42);
      assertEquals("hello", tuple.getFirst());
    }

    @Test
    @DisplayName("should store second value correctly")
    void storesSecondValue() {
      Tuple<String, Integer> tuple = new Tuple<>("hello", 42);
      assertEquals(42, tuple.getSecond());
    }

    @Test
    @DisplayName("should allow null values")
    void allowsNullValues() {
      Tuple<String, Integer> tuple = new Tuple<>(null, null);
      assertNull(tuple.getFirst());
      assertNull(tuple.getSecond());
    }

    @Test
    @DisplayName("should allow mixed null and non-null")
    void allowsMixedNullAndNonNull() {
      Tuple<String, Integer> tuple1 = new Tuple<>(null, 42);
      assertNull(tuple1.getFirst());
      assertEquals(42, tuple1.getSecond());

      Tuple<String, Integer> tuple2 = new Tuple<>("hello", null);
      assertEquals("hello", tuple2.getFirst());
      assertNull(tuple2.getSecond());
    }
  }

  @Nested
  @DisplayName("Field Access")
  class FieldAccess {

    @Test
    @DisplayName("should expose first field directly")
    void exposesFirstField() {
      Tuple<String, Integer> tuple = new Tuple<>("test", 100);
      assertEquals("test", tuple.first);
    }

    @Test
    @DisplayName("should expose second field directly")
    void exposesSecondField() {
      Tuple<String, Integer> tuple = new Tuple<>("test", 100);
      assertEquals(100, tuple.second);
    }
  }

  @Nested
  @DisplayName("Type Safety")
  class TypeSafety {

    @Test
    @DisplayName("should work with different types")
    void worksWithDifferentTypes() {
      Tuple<Integer, String> intStr = new Tuple<>(1, "one");
      assertEquals(1, intStr.getFirst());
      assertEquals("one", intStr.getSecond());

      Tuple<Double, Boolean> doubleBool = new Tuple<>(3.14, true);
      assertEquals(3.14, doubleBool.getFirst(), 0.001);
      assertTrue(doubleBool.getSecond());

      Tuple<Long, Long> longLong = new Tuple<>(100L, 200L);
      assertEquals(100L, longLong.getFirst());
      assertEquals(200L, longLong.getSecond());
    }

    @Test
    @DisplayName("should work with same types")
    void worksWithSameTypes() {
      Tuple<String, String> strStr = new Tuple<>("first", "second");
      assertEquals("first", strStr.getFirst());
      assertEquals("second", strStr.getSecond());
    }

    @Test
    @DisplayName("should work with complex types")
    void worksWithComplexTypes() {
      Tuple<Tuple<Integer, Integer>, String> nested = new Tuple<>(new Tuple<>(1, 2), "nested");
      assertEquals(1, nested.getFirst().getFirst());
      assertEquals(2, nested.getFirst().getSecond());
      assertEquals("nested", nested.getSecond());
    }
  }

  @Nested
  @DisplayName("Equality")
  class Equality {

    @Test
    @DisplayName("should not be equal to null")
    void notEqualToNull() {
      Tuple<String, Integer> tuple = new Tuple<>("test", 42);
      assertNotEquals(null, tuple);
    }

    @Test
    @DisplayName("should not be equal to different type")
    void notEqualToDifferentType() {
      Tuple<String, Integer> tuple = new Tuple<>("test", 42);
      assertNotEquals("test", tuple);
    }
  }

  @Nested
  @DisplayName("Use Cases")
  class UseCases {

    @Test
    @DisplayName("should work as return value for multiple returns")
    void worksAsMultipleReturn() {
      Tuple<Integer, String> result = divideWithRemainder(10, 3);
      assertEquals(3, result.getFirst()); // quotient
      assertEquals("1", result.getSecond()); // remainder as string
    }

    @Test
    @DisplayName("should work with collections")
    void worksWithCollections() {
      java.util.List<Tuple<String, Integer>> pairs =
          java.util.List.of(new Tuple<>("a", 1), new Tuple<>("b", 2), new Tuple<>("c", 3));

      assertEquals(3, pairs.size());
      assertEquals("a", pairs.get(0).getFirst());
      assertEquals(2, pairs.get(1).getSecond());
    }

    private Tuple<Integer, String> divideWithRemainder(int a, int b) {
      return new Tuple<>(a / b, String.valueOf(a % b));
    }
  }
}
