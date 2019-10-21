package org.gesis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CSVDatasetTest {

    private File csvFile;

    @BeforeEach
    void setUp() {
        csvFile = new File("/home/tchechem/workspace/fake_news_extractor/claim_extraction_18_10_2019.csv");
    }

    @Test
    void testInvalidCSVLoad() {
        try {
            final Dataset dataset = Dataset.csv(csvFile);
            assert true;
        } catch (IOException e) {
            assert false;
        }
    }
}