package org.gesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum ComputeERD50Scores {
    ;


    @SuppressWarnings({"FeatureEnvy"})
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Invalid number of arguments...");
            System.exit(1);
        }
        List<Entry> gold = loadFile(Paths.get(args[0]));
        List<Entry> answers = loadFile(Paths.get(args[1]));

        printScores(answers, gold);
    }

    private static void printScores(final List<Entry> answers, final List<Entry> gold) {


        answers.sort(Comparator.comparing(Entry::getId).thenComparing(Entry::getStart));
        gold.sort(Comparator.comparing(Entry::getId).thenComparing(Entry::getStart));

        int goldIndex = 0;
        int annotationIndex = 0;

        int tp = 0;
        int fp = 0;
        int fn = 0;

        while (goldIndex < gold.size()) {

            Entry goldEntry = gold.get(goldIndex);
            while (annotationIndex < answers.size()) {
                final Entry aEntry = answers.get(annotationIndex);
                if (goldEntry.getId().equals(aEntry.getId())) {
                    if (goldEntry.getStart() == aEntry.getStart() && goldEntry.getEnd() == aEntry.getEnd() && goldEntry.getEntity().toLowerCase().equals(aEntry.getEntity().toLowerCase())) {
                        tp++;
                        annotationIndex++;
                    } else if (goldEntry.getStart() == aEntry.getStart()) {
                        fn++;
                        fp++;
                        annotationIndex++;
                    } else if (goldEntry.getStart() > aEntry.getStart()) {
                        if (goldEntry.getEnd() == aEntry.getEnd()) {
                            annotationIndex++;
                            goldIndex++;
                            goldEntry = gold.get(goldIndex);
                            fp++;
                            fn++;
                        } else if (goldEntry.getEnd() > aEntry.getEnd()) {
                            annotationIndex++;
                            fp++;
                        } else {
                            goldIndex++;
                            System.out.println("FN:" + goldEntry);
                            goldEntry = gold.get(goldIndex);
                            fn++;
                        }

                    } else {
                        fn++;
                        System.out.println("FN:" + goldEntry);
                        goldIndex++;
                        goldEntry = gold.get(goldIndex);
                    }
                } else if (goldEntry.getId().compareTo(aEntry.getId()) > 0) {
                    fp++;
                    annotationIndex++;
                } else {
                    fn++;
                    System.out.println("FN:" + goldEntry);
                    goldIndex++;
                    goldEntry = gold.get(goldIndex);
                }
            }
            if (annotationIndex == answers.size()) {
                fp += gold.size() - goldIndex;
                goldIndex = gold.size();
            }
        }
        double P = (double) tp / (double) (tp + fp);
        double R = (double) tp / (double) (tp + fn);
        double F1 = 2 * P * R / (P + R);
        System.out.println("TP=" + tp);
        System.out.println("FP=" + fp);
        System.out.println("FN=" + fn);
        System.out.println("P=" + P);
        System.out.println("R=" + R);
        System.out.println("F1=" + F1);
    }

    private static List<Entry> loadFile(Path path) throws IOException {
        List<Entry> entries = new ArrayList<Entry>();
        final BufferedReader answersreader = Files.newBufferedReader(path);
        answersreader.lines().forEachOrdered(line -> {
            if (!line.isEmpty()) {
                String[] bits = line.split("\t", -1);
                entries.add(new Entry(bits[0], Integer.valueOf(bits[1]), Integer.valueOf(bits[2]), bits[5]));
            }
        });
        return entries;
    }

    private static class Entry {
        private final String id;
        private final int start;
        private final int end;
        private final String entity;

        Entry(String id, int start, int end, String entity) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.entity = entity;
        }

        public String getId() {
            return id;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        String getEntity() {
            return entity;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "id='" + id + '\'' +
                    ", start=" + start +
                    ", end=" + end +
                    ", entity='" + entity + '\'' +
                    '}';
        }
    }
}


