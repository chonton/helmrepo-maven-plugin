package org.honton.chas.helmrepo.maven.plugin;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReleaseHelperTest {

  @Test
  void asSet() {
    Assertions.assertEquals(Set.of("a", "b", "c"), ReleaseHelper.asSet(" a, c,b, "));
  }

  @Test
  void isMavenArtifact() {
    Assertions.assertTrue(ReleaseHelper.isMavenArtifact("g:a:v"));
    Assertions.assertFalse(ReleaseHelper.isMavenArtifact("https://some.where/else"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> ReleaseHelper.isMavenArtifact("g:a:v:c"));
  }

  @Test
  void unversionedName() {
    Assertions.assertEquals(
        "superfantastic", ReleaseHelper.unversionedName("superfantastic-44.12.3.tgz"));
    Assertions.assertEquals(
        "test-reports", ReleaseHelper.unversionedName("org.honton.chas:test-reports:1.3.4"));
    Assertions.assertEquals(
        "test-reports",
        ReleaseHelper.unversionedName(
            "https://repo.maven.apache.org/maven2/org/honton/chas/test-reports/1.3.4/test-reports-1.3.4.tgz"));
  }
}
