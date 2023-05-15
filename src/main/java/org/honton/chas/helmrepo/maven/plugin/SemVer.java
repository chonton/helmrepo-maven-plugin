package org.honton.chas.helmrepo.maven.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private int currentPosition = 0;

    public VersionParser(@NonNull String s) {
      source = s;
    }

    private static boolean isAsciiLetter(int ch) {
      return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    SemVer parse() {
      int major = readNumeric();
      consumeDot();
      int minor = readNumeric();
      consumeDot();
      int patch = readNumeric();

      List<String> preRelease = peek() == '-' ? readSegments(false) : List.of();
      List<String> build = peek() == '+' ? readSegments(true) : List.of();

      if (currentPosition < source.length()) {
        throw new IllegalArgumentException(
            String.format("Unexpected characters in \"%s\" at %d", source, currentPosition - 1));
      }

      return new SemVer(major, minor, patch, preRelease, build);
    }

    private List<String> readSegments(boolean allowLeadingZero) {
      List<String> segments = new ArrayList<>();
      do {
        segments.add(identifier(allowLeadingZero));
      } while (peek() == '.');
      return Collections.unmodifiableList(segments);
    }

    private boolean noMoreToProcess() {
      return currentPosition >= source.length();
    }

    private int peek() {
      if (noMoreToProcess()) {
        return -1;
      }
      return source.charAt(currentPosition);
    }

    private void consumeDot() {
      if (noMoreToProcess()) {
        throw new IllegalArgumentException(
            String.format("Expected '.' in \"%s\" at position %d", source, currentPosition - 1));
      }
      int c = source.charAt(currentPosition++);
      if ('.' != c) {
        throw new IllegalArgumentException(
            String.format(
                "Expected '.', got '%c' in \"%s\" at position %d", c, source, currentPosition - 1));
      }
    }

    private boolean isIdentifierCharacter() {
      int c = peek();
      return Character.isDigit(c) || isAsciiLetter(c) || c == '-';
    }

    private int readNumeric() {
      return Integer.parseInt(numericIdentifier(false));
    }

    private String identifier(boolean leadingZeroAllowed) {
      char toss = source.charAt(currentPosition);
      if (Character.isDigit(toss) || isAsciiLetter(toss)) {
        throw new IllegalStateException();
      }
      ++currentPosition;
      if (Character.isDigit(peek())) {
        return numericIdentifier(leadingZeroAllowed);
      }
      return alphanumericIdentifier();
    }

    private String numericIdentifier(boolean leadingZeroAllowed) {
      int start = currentPosition;
      while (Character.isDigit(peek())) {
        currentPosition++;
      }
      if (start == currentPosition) {
        throw new IllegalArgumentException(
            String.format("Empty numeric identifier in \"%s\" at position %d", source, start));
      }
      if (!leadingZeroAllowed && source.charAt(start) == '0') {
        currentPosition = start + 1;
      }
      return source.substring(start, currentPosition);
    }

    private String alphanumericIdentifier() {
      int start = currentPosition;
      while (isIdentifierCharacter()) {
        currentPosition++;
      }
      if (start == currentPosition) {
        throw new IllegalArgumentException(
            String.format("Empty alphanumeric identifier \"%s\" at %d", source, start));
      }
      return source.substring(start, currentPosition);
    }
  }
}
