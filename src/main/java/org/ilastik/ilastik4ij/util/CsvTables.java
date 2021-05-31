package org.ilastik.ilastik4ij.util;

import com.opencsv.CSVReader;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;

import java.io.IOException;
import java.io.Reader;

final public class CsvTables {
    private CsvTables() {
    }

    /**
     * Read CSV table with a header.
     *
     * @throws RuntimeException some row has invalid number of values
     */
    public static GenericTable read(Reader source) throws IOException {
        GenericTable table = new DefaultGenericTable();
        CSVReader reader = new CSVReader(source);

        String[] headers = reader.readNext();
        if (headers == null) {
            return table;
        }

        table.appendColumns(headers);
        int n = table.getColumnCount();

        for (int row = 0; ; row++) {
            String[] values = reader.readNext();
            if (values == null) {
                break;
            }

            if (values.length != n) {
                throw new RuntimeException(String.format(
                        "Row #%d: expected %d values, got %d", row + 1, n, values.length));
            }

            table.appendRow();
            for (int col = 0; col < n; col++) {
                table.set(col, row, values[col]);
            }
        }

        return table;
    }
}
