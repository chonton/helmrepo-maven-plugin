package org.honton.chas.helmrepo.maven.plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
class ReleaseHelper {

  private final int N_GAV_SEGMENTS = 3;

  String unversionedName(String chart) {
    String[] parts = chart.split(":");
    if (parts.length == N_GAV_SEGMENTS) {
      // maven artifact
      return parts[1];
    }

    int firstIdx = chart.lastIndexOf('/') + 1;
    if (chart.endsWith(".tgz")) {
      int endIdx = chart.lastIndexOf('-');
      if (endIdx < 0) {
        throw new IllegalArgumentException(chart + " is not a proper versioned chart");
      }
      return chart.substring(firstIdx, endIdx);
    }
    return chart.substring(firstIdx);
  }

  Set<String> asSet(String commaSeparated) {
    return commaSeparated != null
        ? new HashSet<>(Arrays.asList(commaSeparated.split("\\w*,\\w*")))
        : Set.of();
  }

  boolean isMavenArtifact(String chart) {
    String[] parts = chart.split(":");
    if (parts.length > N_GAV_SEGMENTS) {
      throw new IllegalArgumentException(chart + " is not a valid chart specification");
    }
    return parts.length == N_GAV_SEGMENTS;
  }
}
