package org.honton.chas.helmrepo.maven.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServiceSelectorTest {

  @Test
  void of() {
    Assertions.assertEquals(new ServiceSelector(null, "s", null), ServiceSelector.of("s", null));
    Assertions.assertEquals(new ServiceSelector("n", "s", null), ServiceSelector.of("n/s", null));
    Assertions.assertEquals(new ServiceSelector(null, "s", "p"), ServiceSelector.of("s:p", null));
    Assertions.assertEquals(new ServiceSelector("n", "s", "p"), ServiceSelector.of("n/s:p", null));
    Assertions.assertEquals(new ServiceSelector("n", "s", null), ServiceSelector.of("s", "n"));
  }
}
