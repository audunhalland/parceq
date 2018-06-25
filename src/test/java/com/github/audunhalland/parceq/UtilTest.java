package com.github.audunhalland.parceq;

import static org.junit.Assert.*;

import io.vavr.collection.List;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

public class UtilTest {
  @Test
  public void shingler_shingles() {
    assertThat(
        Util.shingler(3).apply(List.of(1, 2, 3, 4)),
        IsEqual.equalTo(List.of(
            List.of(1),
            List.of(1, 2),
            List.of(1, 2, 3),
            List.of(2),
            List.of(2, 3),
            List.of(2, 3, 4),
            List.of(3),
            List.of(3, 4),
            List.of(4))));
  }

  @Test
  public void sliding_zipper_works_with_zero_input() {
    assertThat(
        Util.slidingZipper(2).apply(List.empty()),
        IsEqual.equalTo(List.empty()));
  }

  @Test
  public void sliding_zipper_works_for_order_1() {
    assertThat(
        Util.slidingZipper(1).apply(List.of(List.of(1), List.of(2))),
        IsEqual.equalTo(List.of(List.of(List.of(1)), List.of(List.of(2)))));
  }

  @Test
  public void sliding_zipper_works_for_order_2() {
    assertThat(
        Util.slidingZipper(2).apply(List.of(List.of(1), List.of(2))),
        IsEqual.equalTo(List.of(List.of(List.of(1), List.of(1, 2)), List.of(List.of(2)))));
  }

  @Test
  public void sliding_zipper_works_for_order_2_with_3_elements() {
    assertThat(
        Util.slidingZipper(2).apply(List.of(List.of(1), List.of(2), List.of(3))),
        IsEqual.equalTo(List.of(
            List.of(List.of(1), List.of(1, 2)),
            List.of(List.of(2), List.of(2, 3)),
            List.of(List.of(3)))));
  }

  @Test
  public void sliding_zipper_works_for_order_3() {
    assertThat(
        Util.slidingZipper(3).apply(List.of(List.of(1), List.of(2), List.of(3))),
        IsEqual.equalTo(List.of(
            List.of(List.of(1), List.of(1, 2), List.of(1, 2, 3)),
            List.of(List.of(2), List.of(2, 3)),
            List.of(List.of(3)))));
  }
}
