package com.github.audunhalland.parceq;

import io.vavr.collection.List;
import java.util.function.Function;

public class Util {
  public static <T> Function<List<T>, List<List<T>>> shingler(int order) {
    return input ->
        Util.<T>slidingZipper(order)
            .apply(input.map(List::of))
            .flatMap(Function.identity())
            .toList();
  }

  public static <T> Function<List<List<T>>, List<List<List<T>>>> slidingZipper(int n) {
    return input ->
        n <= 1
            ? input.map(item -> List.of(item))
            : Util.<T>slidingZipper(n - 1).apply(input)
                .zip(input
                    .map(List::head)
                    .sliding(n)
                    .toList()
                    .padTo(input.length(), List.empty()))
                .map(tup -> tup._1.append(tup._2)
                    .filter(List::nonEmpty));
  }
}
