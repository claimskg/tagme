package org.gesis;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface Dataset extends Iterable<Row> {
    static Dataset csv(final File file) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setKeepEscapeSequences(true);
        settings.getFormat().setDelimiter(',');
        settings.getFormat().setQuote('"');
        settings.getFormat().setQuoteEscape('"');
        settings.getFormat().setCharToEscapeQuoteEscaping('"');
        settings.setMaxCharsPerColumn(4096 * 100);

        return csv(file, settings);
    }

    static Dataset csv(File file, CsvParserSettings settings) throws IOException {
        CsvParser parser = new CsvParser(settings);
        return new CSVDataset(parser.parseAll(file));

    }

    String get(int index, final String name);

    void set(int index, final String name, final String value);

    void write(File file) throws FileNotFoundException;
}
