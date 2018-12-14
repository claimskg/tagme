package org.gesis;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CSVDataset implements Dataset {

    private final List<String[]> content;
    private final Map<String, Integer> headerMap;
    private final String[] header;
    private final List<Row> wrapperContainer;

    CSVDataset(final List<String[]> content) throws IOException {
        header = content.get(0);
        this.content = content.subList(1, content.size());
        check_column_consistency(header,content);
        headerMap = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            headerMap.put(header[i], i);
        }

        wrapperContainer = IntStream.range(0, this.content.size())
                .mapToObj(i -> create(this, i))
                .collect(Collectors.toList());

    }

    private static Row create(Dataset dataset, int row) {
        return new CSVRow(dataset, row);
    }

    private void check_column_consistency(final String[] header, final Iterable<String[]> content) throws IOException {
        int row_index = 0;
        for (String[] row : content) {
            if (row.length != header.length) {
                throw new IOException(
                        MessageFormat.format("Invalid column count on line {0}, {1}, instead of {2}",
                                row_index, row.length, header.length));
            }
            row_index++;
        }
    }

    @Override
    public String get(int index, String name) {
        Integer column = headerMap.getOrDefault(name, -1);
        return (column >= 0) ? content.get(index)[column] : "";
    }

    @Override
    public void set(int index, String name, String value) {
        Integer column = headerMap.getOrDefault(name, -1);
        if (column >= 0) {
            content.get(index)[column] = value;
        }
    }

    @Override
    public void write(File file) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);
        writer.println(Arrays.stream(header).map(h -> "\"" + StringEscapeUtils.escapeCsv(h) + "\"").collect(Collectors.joining(",")));
        for (String[] row : content) {
            writer.println(Arrays.stream(row).map(value -> "\"" + StringEscapeUtils.escapeCsv(value) + "\"").collect(Collectors.joining(",")));
        }
    }

    @Override
    public Iterator<Row> iterator() {
        return wrapperContainer.iterator();
    }

    @Override
    public void forEach(Consumer<? super Row> action) {
        wrapperContainer.forEach(action);
    }

    @Override
    public Spliterator<Row> spliterator() {
        return wrapperContainer.spliterator();
    }
}
