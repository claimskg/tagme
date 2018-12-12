package org.gesis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CSVDataset implements Dataset {

    private final List<String[]> content;
    private final Map<String, Integer> headerMap;
    private final String[] header;
    private final List<Row> wrapperContainer;

    CSVDataset(List<String[]> content) {
        header = content.get(0);
        this.content = content.subList(1, content.size());
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

    @Override
    public String get(int index, String name) {
        Integer column = headerMap.getOrDefault(name, -1);
        return (column >= 0) ? content.get(index)[column] : "";
    }

    @Override
    public void set(int index, String name, String value) {
        Integer column = headerMap.getOrDefault(name, -1);
        if (column >= 0) {
            System.err.println("Setting " + name + " with " + value);
            content.get(index)[column] = value;
        }
    }

    @Override
    public void write(File file) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);
        writer.println(Arrays.stream(header).map(h -> "\"" + h + "\"").collect(Collectors.joining(",")));
        for (String[] row : content) {
            writer.println(Arrays.stream(row).map(h -> "\"" + h + "\"").collect(Collectors.joining(",")));
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
