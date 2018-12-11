package org.gesis;

import java.util.Optional;

public class CSVRow implements Row {

    private final Dataset dataset;
    private final int index;

    CSVRow(final Dataset dataset, int index) {
        this.dataset = dataset;
        this.index = index;
    }

    @Override
    public String get(String name) {
        String value = dataset.get(index, name);
        return Optional.ofNullable(value).orElse("");
    }

    @Override
    public void set(String name, String value) {
        dataset.set(index, name, value);
    }

    @Override
    public int rowIndex() {
        return index;
    }
}
