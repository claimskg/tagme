package org.gesis;

public interface Row {

    String get(final String name);

    void set(final String name, final String value);

    int rowIndex();
}
