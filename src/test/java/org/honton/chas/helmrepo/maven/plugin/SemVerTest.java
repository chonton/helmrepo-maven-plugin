package org.honton.chas.helmrepo.maven.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SemVerTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1.26.6",
        "1.26.6-DEBUG",
        "0.9.1+Yellow",
        "0.9.1-alpha.1.one",
        "0.9.1-alpha.1.1+Blue",
        "1.0.0-alpha",
        "1.0.0-alpha.1",
        "1.0.0-0.3.7",
        "1.0.0-x.7.z.92",
        "1.0.0-x-y-z.--",
        "1.0.0-alpha+001",
        "1.0.0+20130313144700",
        "1.0.0-beta+exp.sha5114f85"
      })
  void parses(String good) {
    Assertions.assertDoesNotThrow(() -> SemVer.valueOf(good));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0.0-beta+exp.sha.5114f85", "1.0.0+21AF26D3----117B344092BD"})
  void doesNotParse(String bad) {
    Assertions.assertThrows(IllegalArgumentException.class, () -> SemVer.valueOf(bad));
  }
}
