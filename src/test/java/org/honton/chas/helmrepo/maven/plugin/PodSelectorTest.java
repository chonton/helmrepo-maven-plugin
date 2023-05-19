package org.honton.chas.helmrepo.maven.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PodSelectorTest {

  @Test
  void of() {
    Assertions.assertEquals(new PodSelector(null, "p"), PodSelector.of("p", null));
    Assertions.assertEquals(new PodSelector("n", "p"), PodSelector.of("n/p", null));
    Assertions.assertEquals(new PodSelector("n", "p"), PodSelector.of("p", "n"));
  }
}
