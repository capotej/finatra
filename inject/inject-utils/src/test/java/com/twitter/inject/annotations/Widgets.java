package com.twitter.inject.annotations;

public final class Widgets {
  private Widgets() {
  }

  /** Creates a {@link Widget} annotation with {@code name} as the value. */
  public static Widget named(String name) {
    return new WidgetImpl(name);
  }
}
