package com.da.crystal.report.UFL;

import com.crystaldecisions.reports.common.value.FormulaValue;
import com.crystaldecisions.reports.common.value.NumericValue;
import com.crystaldecisions.reports.common.value.StringValue;
import com.crystaldecisions.reports.formulas.FormulaValueReference;

public class ArgUtils {

  // ---- Raw value access ----

  /**
   * Returns the raw FormulaValue at the given index, or null if missing.
   */
  public static FormulaValue getArgValue(FormulaValueReference[] args, int index) {
    if (args != null && index < args.length && args[index] != null) {
      return args[index].getFormulaValue();
    }
    return null;
  }

  // ---- String ----

  /**
   * Extracts the plain string value from a FormulaValue argument.
   *
   * <p>
   * Crystal Reports value types have a {@code toString()} that includes
   * type prefixes (e.g. {@code n{120}} for numbers, {@code s{...}} for strings).
   * This method extracts the raw value without any type marker.
   * </p>
   *
   * @param args  the argument array
   * @param index the argument index to read
   * @return the plain string representation, or "" if missing/null/empty
   */
  public static String getArgString(FormulaValueReference[] args, int index) {
    FormulaValue val = getArgValue(args, index);
    return val != null ? toPlainString(val) : "";
  }

  /**
   * Converts a FormulaValue to its plain string representation (no type prefix).
   */
  public static String toPlainString(FormulaValue val) {
    if (val == null) {
      return "";
    }
    if (val instanceof StringValue) {
      String s = ((StringValue) val).getString();
      return s != null ? s.trim() : "";
    }
    if (val instanceof NumericValue) {
      NumericValue num = (NumericValue) val;
      if (num.isIntegerValue()) {
        return Long.toString(num.getLong());
      } else {
        return Double.toString(num.getDouble());
      }
    }
    FormulaValue coerced = val.coerceToString();
    if (coerced instanceof StringValue) {
      String s = ((StringValue) coerced).getString();
      return s != null ? s.trim() : "";
    }
    String s = val.toString();
    if (s != null) {
      s = s.trim();
      if (s.length() >= 3 && s.charAt(1) == '{' && s.endsWith("}")) {
        s = s.substring(2, s.length() - 1);
      }
    }
    return s != null ? s.trim() : "";
  }

  // ---- Integer ----

  /**
   * Extracts an integer from a FormulaValue argument.
   *
   * @param args         the argument array
   * @param index        the argument index to read
   * @param defaultValue value returned when the argument is missing/null/not
   *                     numeric
   * @return the integer value
   */
  public static int getArgInteger(FormulaValueReference[] args, int index, int defaultValue) {
    FormulaValue val = getArgValue(args, index);
    if (val instanceof NumericValue) {
      return ((NumericValue) val).getInt();
    }
    if (val != null) {
      try {
        return Integer.parseInt(toPlainString(val));
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  /**
   * Extracts an integer, defaulting to 0.
   */
  public static int getArgInteger(FormulaValueReference[] args, int index) {
    return getArgInteger(args, index, 0);
  }

  // ---- Long ----

  /**
   * Extracts a long from a FormulaValue argument.
   *
   * @param args         the argument array
   * @param index        the argument index to read
   * @param defaultValue value returned when the argument is missing/null/not
   *                     numeric
   * @return the long value
   */
  public static long getArgLong(FormulaValueReference[] args, int index, long defaultValue) {
    FormulaValue val = getArgValue(args, index);
    if (val instanceof NumericValue) {
      return ((NumericValue) val).getLong();
    }
    if (val != null) {
      try {
        return Long.parseLong(toPlainString(val));
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  /**
   * Extracts a long, defaulting to 0L.
   */
  public static long getArgLong(FormulaValueReference[] args, int index) {
    return getArgLong(args, index, 0L);
  }

  // ---- Double ----

  /**
   * Extracts a double from a FormulaValue argument.
   *
   * @param args         the argument array
   * @param index        the argument index to read
   * @param defaultValue value returned when the argument is missing/null/not
   *                     numeric
   * @return the double value
   */
  public static double getArgDouble(FormulaValueReference[] args, int index, double defaultValue) {
    FormulaValue val = getArgValue(args, index);
    if (val instanceof NumericValue) {
      return ((NumericValue) val).getDouble();
    }
    if (val != null) {
      try {
        return Double.parseDouble(toPlainString(val));
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  /**
   * Extracts a double, defaulting to 0.0.
   */
  public static double getArgDouble(FormulaValueReference[] args, int index) {
    return getArgDouble(args, index, 0.0);
  }

  // ---- Boolean ----

  /**
   * Extracts a boolean from a FormulaValue argument.
   * Accepts boolean values directly, or string "true"/"1"/"yes"
   * (case-insensitive).
   *
   * @param args         the argument array
   * @param index        the argument index to read
   * @param defaultValue value returned when the argument is missing/null
   * @return the boolean value
   */
  public static boolean getArgBoolean(FormulaValueReference[] args, int index, boolean defaultValue) {
    FormulaValue val = getArgValue(args, index);
    if (val == null) {
      return defaultValue;
    }
    if (val instanceof com.crystaldecisions.reports.common.value.BooleanValue) {
      return ((com.crystaldecisions.reports.common.value.BooleanValue) val).getBoolean();
    }
    if (val instanceof NumericValue) {
      return ((NumericValue) val).getLong() != 0;
    }
    String s = toPlainString(val).trim();
    return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
  }

  /**
   * Extracts a boolean, defaulting to false.
   */
  public static boolean getArgBoolean(FormulaValueReference[] args, int index) {
    return getArgBoolean(args, index, false);
  }

  // ---- Utility ----

  public static String argsToString(FormulaValueReference[] args) {
    if (args == null)
      return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      if (i > 0)
        sb.append(", ");
      sb.append(getArgString(args, i));
    }
    return sb.toString();
  }
}
