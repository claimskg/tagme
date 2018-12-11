package org.gesis;

public class CSVRow implements Row {

    private final Dataset dataset;
    private final int index;

    CSVRow(final Dataset dataset, int index) {
        this.dataset = dataset;
        this.index = index;
    }

    @Override
    public String get(String name) {
        return dataset.get(index, name);
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
