package io.contracttesting.contractcase;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import io.contract_testing.contractcase.case_boundary.BoundaryResult;
import io.contract_testing.contractcase.case_boundary.BoundarySuccess;
import io.contract_testing.contractcase.case_boundary.ILogPrinter;
import io.contract_testing.contractcase.case_boundary.IResultPrinter;
import io.contract_testing.contractcase.case_boundary.PrintableMatchError;
import io.contract_testing.contractcase.case_boundary.PrintableMessageError;
import io.contract_testing.contractcase.case_boundary.PrintableTestTitle;
import io.contracttesting.contractcase.connectors.ExceptionMapper;
import org.jetbrains.annotations.NotNull;

public class LogPrinter implements ILogPrinter, IResultPrinter {

  private static final AnsiFormat brightRed = new AnsiFormat(Attribute.BRIGHT_RED_TEXT());
  private static final AnsiFormat brightYellow = new AnsiFormat(Attribute.BRIGHT_YELLOW_TEXT());

  private static final AnsiFormat redBack = new AnsiFormat(Attribute.RED_BACK(),
      Attribute.WHITE_TEXT());

  private static final AnsiFormat red = new AnsiFormat(Attribute.RED_TEXT());

  private static final AnsiFormat cyan = new AnsiFormat(Attribute.CYAN_TEXT());

  private static final AnsiFormat magentaBackground = new AnsiFormat(Attribute.MAGENTA_BACK(),
      Attribute.BLACK_TEXT());

  private static final AnsiFormat magenta = new AnsiFormat(Attribute.MAGENTA_TEXT());

  private static final AnsiFormat white = new AnsiFormat(Attribute.WHITE_TEXT());

  private static final AnsiFormat whiteBright = new AnsiFormat(Attribute.BRIGHT_WHITE_TEXT());

  private static final AnsiFormat blue = new AnsiFormat(Attribute.BRIGHT_BLUE_TEXT());

  private static final AnsiFormat blueBack = new AnsiFormat(Attribute.BRIGHT_BLUE_BACK(),
      Attribute.BLACK_TEXT());

  private static final AnsiFormat green = new AnsiFormat(Attribute.GREEN_TEXT());

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
      return ExceptionMapper.map(e);
    }
  }

  private String spaces(int num, String toPad) {
    return toPad.replace("\n", "\n" + " ".repeat(Math.max(0, num)));
  }

  @Override
  public @NotNull BoundaryResult printMatchError(
      @NotNull PrintableMatchError description) {
    try {
      // TODO: Clean up this mess.
      // For now it's just a
      // copy/paste of the javascript, which
      // makes for very unpleasant Java
      System.out.println(spaces(6,
          redBack.format(" " + description.getKind() + " ") + " "
              + whiteBright.format(description.getLocation()) + " "
              + whiteBright.format(
              description.getMessage())) + "\n" + spaces(9,
          "Expected something like:\n" + spaces(3,
              green.format(description.getExpected()))) + "\n" + spaces(9,
          "Actual:\n" + spaces(3, red.format(description.getActual()))) + "\n\n" + spaces(
          12, white.format(
              " - " + description.getLocation() + " ["
                  + description.getErrorTypeTag() + "]")));

      return new BoundarySuccess();
    } catch (Exception e) {
      return ExceptionMapper.map(e);
    }
  }

  @Override
  public @NotNull BoundaryResult printMessageError(
      @NotNull PrintableMessageError description) {
    try {
      // TODO: Clean up this mess.
      // For now it's just a
      // copy/paste of the javascript, which
      // makes for very unpleasant Java
      System.out.println(spaces(6, redBack.format(" " + description.getKind() + " ")
          + " " + whiteBright.format(description.getLocation()) + " "
          + whiteBright.format(
          description.getMessage())) + "\n\n" + spaces(
          12, white.format(
              " - " + description.getLocation() + " ["
                  + description.getErrorTypeTag() + "]")));

      return new BoundarySuccess();
    } catch (Exception e) {
      return ExceptionMapper.map(e);
    }
  }

  @Override
  public @NotNull BoundaryResult printTestTitle(@NotNull PrintableTestTitle titleDetails) {
    try {
      // TODO: Clean up this mess.
      // For now it's just a
      // copy/paste of the javascript, which
      // makes for very unpleasant Java
      System.out.println(spaces(3,
          titleDetails.getKind().equals("SUCCESS") ? green.format(titleDetails.getIcon())
              : red.format(titleDetails.getIcon()) + " " + whiteBright.format(
                  titleDetails.getTitle()) + "\n" + titleDetails.getAdditionalText()));

      return new BoundarySuccess();
    } catch (Exception e) {
      return ExceptionMapper.map(e);
    }
  }
}
