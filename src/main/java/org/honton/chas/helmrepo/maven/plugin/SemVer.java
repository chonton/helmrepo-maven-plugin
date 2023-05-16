package org.honton.chas.helmrepo.maven.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;
import lombok.NonNull;
import lombok.Value;

@Value
public class SemVer {

  int major;
  int minor;
  int patch;
  List<String> preRelease;
  List<String> build;

  public static SemVer valueOf(String s) {
    return new VersionParser(s).parse();
  }

  static final class VersionParser {

    private final String source;
    private int offset = 0;

    public VersionParser(@NonNull String s) {
      source = s;
    }

    SemVer parse() {
      int major = numeric("major");
      consumeDot();
      int minor = numeric("minor");
      consumeDot();
      int patch = numeric("patch");

      List<String> preRelease = readSegments('-', true);
      List<String> build = readSegments('+', false);

      if (offset < source.length()) {
        throw new IllegalArgumentException(
            String.format("Unexpected characters in '%s' at %d", source, offset - 1));
      }

      return new SemVer(major, minor, patch, preRelease, build);
    }

    private int numeric(String version) {
      String numeric = identifier(version, Character::isDigit);
      if (numeric.charAt(0) == '0' && numeric.length() > 1) {
        throw new IllegalArgumentException(
            String.format(
                "No leading zero allowed in '%s' at position %d",
                numeric, offset - numeric.length()));
      }
      return Integer.parseInt(numeric);
    }

    private List<String> readSegments(int leadChar, boolean preRelease) {
      if (peek() != leadChar) {
        return List.of();
      }
      List<String> segments = new ArrayList<>();
      do {
        segments.add(identifier(preRelease));
      } while (peek() == '.');
      return Collections.unmodifiableList(segments);
    }

    private boolean noMoreToProcess() {
      return offset >= source.length();
    }

    private int peek() {
      if (noMoreToProcess()) {
        return -1;
      }
      return source.charAt(offset);
    }

    private int get() {
      return source.charAt(offset++);
    }

    private void consumeDot() {
      if (noMoreToProcess()) {
        throw new IllegalArgumentException(
            String.format("Expected '.' in '%s' at position %d", source, offset - 1));
      }
      int c = get();
      if ('.' != c) {
        throw new IllegalArgumentException(
            String.format("Expected '.', got '%c' in '%s' at position %d", c, source, offset - 1));
      }
    }

    private boolean isIdentifierCharacter(int c) {
      return Character.isDigit(c) || ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || c == '-';
    }

    private String identifier(boolean preRelease) {
      int toss = get();
      if (toss != '+' && toss != '-' && toss != '.') {
        throw new IllegalStateException();
      }
      if (preRelease) {
        if (Character.isDigit(peek())) {
          return identifier("numeric", Character::isDigit);
        }
      }
      return identifier("alphanumeric", this::isIdentifierCharacter);
    }

    private String identifier(String type, IntPredicate predicate) {
      int start = offset;
      while (predicate.test(peek())) {
        offset++;
      }
      if (start == offset) {
        throw new IllegalArgumentException(
            String.format("Empty %s identifier '%s' at %d", type, source, start));
      }
      return source.substring(start, offset);
    }
  }
}
