package org.gesis;

import it.acubelab.tagme.*;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import me.tongfei.progressbar.ProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum EvaluateOnERD50 {
    ;


    @SuppressWarnings("LocalVariableOfConcreteClass")
    private static List<Annotation> annotateText(final String text, double threshold) throws IOException {
        String lang = "en";

        AnnotatedText ann_text = new AnnotatedText(text);

        RelatednessMeasure relatednessMeasure = RelatednessMeasure.create(lang);

        final Predicate<Annotation> filter = (a -> a.isDisambiguated() && a.getRho() >= threshold);

        TagmeParser parser = new TagmeParser(lang, true);

        Disambiguator disambiguator = new Disambiguator(lang);
        Segmentation segmentation = new Segmentation();
        RhoMeasure rho = new RhoMeasure();

        parser.parse(ann_text);
        segmentation.segment(ann_text);
        disambiguator.disambiguate(ann_text, relatednessMeasure);
        rho.calc(ann_text, relatednessMeasure);


        List<Annotation> tagMeAnnotations = ann_text.getAnnotations();
        if (tagMeAnnotations == null) {
            tagMeAnnotations = Collections.emptyList();
        }

        return tagMeAnnotations.stream()
                .filter(filter).collect(Collectors.toList());

    }

    @SuppressWarnings({"FeatureEnvy"})
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Invalid number of arguments...");
            System.exit(1);
        }
        Double threshold = Double.valueOf(args[2]);
        final List<Document> corpus = new ArrayList<>();

        Files.list(Paths.get(args[0])).forEach(file -> {
            try {
                final BufferedReader reader = Files.newBufferedReader(file);
                Optional<String> text = reader.lines().reduce(String::concat);
                text.ifPresent(s -> corpus.add(new Document(file.getFileName().toString().split("\\.")[0], s)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        List<String> gold = new ArrayList<>();
        final BufferedReader goldreader = Files.newBufferedReader(Paths.get(args[1]));
        goldreader.lines().forEachOrdered(line -> {
            String[] bits = line.split("\t");
            gold.add(bits[0] + ";" + bits[1] + ";" + bits[2] + ";" + bits[5]);

        });


        List<String> answers = new ArrayList<>();
        PrintWriter outputWriter = new PrintWriter(Files.newBufferedWriter(Paths.get("kore50.tsv")));
        TagmeConfig.init();
        for (final Document doc : ProgressBar.wrap(corpus, "Annotating dataset...")) {

            final List<Annotation> annotations = EvaluateOnERD50.annotateText(doc.getText(), threshold);
            final TopicSearcher searcher = new TopicSearcher("en");

            for (final Annotation annotation : annotations) {
                int wikipediaId = annotation.getTopic();
                String entity = searcher.getTitle(wikipediaId);
                answers.add(doc.getId() + ";" + annotation.getStart() + ";" + annotation.getEnd() + ";" + entity);
                outputWriter.println(doc.getId() + "\t" + annotation.getStart() + "\t" + annotation.getEnd() + "\t\t\t" + entity + "\t\t");
            }
            outputWriter.println();

        }
        outputWriter.flush();

        printScores(answers, gold);
    }

    private static void printScores(final List<String> answers, final List<String> gold) {

        int goldIndex = 0;
        int annotationIndex = 0;

        int tp = 0;
        int fp = 0;
        int fn = 0;

        while (goldIndex < gold.size()) {
            String[] goldBits = gold.get(goldIndex).split(";");
            String goldDoc = goldBits[0];
            int goldStart = Integer.valueOf(goldBits[1]);
            int goldEnd = Integer.valueOf(goldBits[2]);
            String goldEntity = goldBits[3];
            while (annotationIndex < answers.size()) {
                String[] aBits = answers.get(annotationIndex).split(";");
                String aDoc = aBits[0];
                int aStart = Integer.valueOf(aBits[1]);
                int aEnd = Integer.valueOf(aBits[2]);
                String aEntity = aBits[3];
                if (aStart >= goldStart) {
                    if (goldDoc.equals(aDoc)) {
                        if (goldStart == aStart && goldEnd == aEnd && goldEntity.equals(aEntity)) {
                            tp++;
                            annotationIndex++;
                        } else if (goldStart == aStart) {
                            fn++;
                            fp++;
                            annotationIndex++;
                        } else if (aStart > goldStart) {
                            fn++;
                            goldIndex++;
                            break;
                        } else if (aStart < goldStart) {
                            fp++;
                            annotationIndex++;
                        }
                    } else {
                        fp++;
                    }
                }
            }
        }
        double P = (double) tp / (double) (tp + fp);
        double R = (double) tp / (double) (tp + fn);
        double F1 = 2 * P * R / (P + R);
        System.out.println("P=" + P);
        System.out.println("R=" + R);
        System.out.println("F1=" + F1);
    }


    private static class Document {
        private final String id;
        private String text;

        Document(final String id, final String text) {
            this.id = id;
            this.text = text;
        }

        Document(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        void addToken(final String token) {
            text += " " + token;
        }

        public String getText() {
            return text;
        }
    }
}


