package org.honton.chas.helmrepo.maven.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;

@Value
public class ReleaseState {
  @NonNull ReleaseInfo release;

  /** The releases that this release requires before deployment */
  @NonNull Set<String> requires;

  /** The releases that depend upon this release */
  Map<String, ReleaseState> depends = new HashMap<>();

  public boolean isSolved() {
    return requires.isEmpty();
  }

  boolean removeRequiresFromDependents() {
    long count =
        depends.values().stream()
            .filter(dependent -> dependent.removeRequirement(release.name))
            .count();
    return count > 0;
  }

  private boolean removeRequirement(String name) {
    requires.remove(name);
    return requires.isEmpty();
  }

  public void addDependent(String name, ReleaseState value) {
    depends.put(name, value);
  }
}
