package org.honton.chas.helmrepo.maven.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.Value;

@Value
public class ReleaseRequirements {
  @NonNull Release release;

  /** The releases that this release requires before deployment */
  @NonNull Set<String> requires;

  /** The releases that depend upon this release */
  Map<String, ReleaseRequirements> depends = new HashMap<>();

  public boolean isSolved() {
    return requires.isEmpty();
  }

  boolean removeRequiresFromDependents() {
    long count =
        depends.values().stream()
            .filter(dependent -> dependent.removeRequirement(release.getName()))
            .count();
    return count > 0;
  }

  private boolean removeRequirement(String name) {
    requires.remove(name);
    return requires.isEmpty();
  }

  public void addDependent(String name, ReleaseRequirements value) {
    depends.put(name, value);
  }
}
