package io.contracttesting.contractcase;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import io.contract_testing.contractcase.case_boundary.BoundaryFailure;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class LogPrinter implements ILogPrinter {

  private static final AnsiFormat brightRed = new AnsiFormat(Attribute.BRIGHT_RED_TEXT());
  private static final AnsiFormat brightYellow = new AnsiFormat(Attribute.BRIGHT_YELLOW_TEXT());

  private static final AnsiFormat cyan = new AnsiFormat(Attribute.CYAN_TEXT());

  private static final AnsiFormat magentaBackground = new AnsiFormat(Attribute.MAGENTA_BACK(),
      Attribute.BLACK_TEXT());

  private static final AnsiFormat magenta = new AnsiFormat(Attribute.MAGENTA_TEXT());

  private static final AnsiFormat white = new AnsiFormat(Attribute.WHITE_TEXT());

  private static final AnsiFormat whiteBright = new AnsiFormat(Attribute.BRIGHT_WHITE_TEXT());

  private static final AnsiFormat blue = new AnsiFormat(Attribute.BRIGHT_BLUE_TEXT());

  private static final AnsiFormat blueBack = new AnsiFormat(Attribute.BRIGHT_BLUE_BACK(),
      Attribute.BLACK_TEXT());

  @Override

  public @NotNull BoundaryResult log(@NotNull String level, @NotNull String timestamp,
      @NotNull String version, @NotNull String typeString, @NotNull String location,
      @NotNull String message, @NotNull String additional) {
    try {
      AnsiFormat typeColour;
      AnsiFormat messageColour;

      switch (level) {
        case "error" -> {
          typeColour = brightRed;
          messageColour = typeColour;
        }
        case "warn" -> {
          typeColour = brightYellow;
          messageColour = typeColour;
        }
        case "debug" -> {
          typeColour = cyan;
          messageColour = typeColour;
        }
        case "maintainerDebug" -> {
          typeColour = magentaBackground;
          messageColour = magenta;
        }
        case "deepMaintainerDebug" -> {
          typeColour = blueBack;
          messageColour = blue;
        }
        default -> {
          typeColour = brightRed;
          messageColour = white;
        }
      }

      System.out.println(
          timestamp + " " + whiteBright.format(version) + typeColour.format(typeString) + " "
              + blue.format(location) + ": " + messageColour.format(message) + (additional.isBlank()
              ? "" : "\n" + messageColour.format(additional)));
      return new BoundarySuccess();
    } catch (Exception e) {
      return new BoundaryFailure(e.getClass().getName(), e.getMessage(),
          Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(
              Collectors.joining()));
    }
  }
}
