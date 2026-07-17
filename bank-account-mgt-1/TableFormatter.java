/** Static helpers for rendering left-justified console tables with dividers. */
final class TableFormatter {
    private TableFormatter() {
    }

    /** @return each column's width: the larger of its minimum and its widest cell */
    static int[] columnWidths(String[] headers, String[][] rows, int[] minWidths) {
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = Math.max(minWidths[c], headers[c].length());
            for (String[] row : rows) {
                if (row[c].length() > widths[c]) {
                    widths[c] = row[c].length();
                }
            }
        }
        return widths;
    }

    /** @return a left-justified printf format string, e.g. "%-8s | %-17s | ...%n" */
    static String buildRowFormat(int[] widths) {
        StringBuilder format = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                format.append(" | ");
            }
            format.append("%-").append(widths[c]).append("s");
        }
        format.append("%n");
        return format.toString();
    }

    /** @return a horizontal rule as wide as the whole table, columns plus " | " separators */
    static String buildDivider(int[] widths) {
        int total = 0;
        for (int width : widths) {
            total += width;
        }
        total += 3 * (widths.length - 1);
        return "─".repeat(total);
    }
}
