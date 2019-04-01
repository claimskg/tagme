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
    private static List<Annotation> annotateText(final AnnotatedText ann_text, double threshold) throws IOException {
        String lang = "en";

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


        List<String> answers = new ArrayList<>();
        PrintWriter outputWriter = new PrintWriter(Files.newBufferedWriter(Paths.get("kore50.tsv")));
        TagmeConfig.init();
        for (final Document doc : ProgressBar.wrap(corpus, "Annotating dataset...")) {
            final AnnotatedText annotatedText = new AnnotatedText(doc.getText());
            final List<Annotation> annotations = EvaluateOnERD50.annotateText(annotatedText, threshold);
            final TopicSearcher searcher = new TopicSearcher("en");

            for (final Annotation annotation : annotations) {
                int wikipediaId = annotation.getTopic();
                String entity = searcher.getTitle(wikipediaId);
                int start = annotatedText.getOriginalTextStart(annotation);
                int end = annotatedText.getOriginalTextEnd(annotation);
                String text = annotatedText.getText(annotation);
                answers.add(doc.getId() + ";" + annotation.getStart() + ";" + annotation.getEnd() + ";" + entity);
                outputWriter.println(doc.getId() + "\t" + start + "\t" + end + "\t\t\t" + text + "\t\t");
            }
            outputWriter.println();

        }
        outputWriter.flush();

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


