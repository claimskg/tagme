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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum EvaluateOnConll2003YAGO {
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
        if (args.length < 2) {
            System.err.println("Invalid number of arguments...");
            System.exit(1);
        }
        Double threshold = Double.valueOf(args[1]);
        final List<Document> corpus = new ArrayList<>();
        final BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]));
        List<String> lines = reader.lines().collect(Collectors.toList());
        Document document = null;
        for (String line : lines) {
            if (line.contains("-DOCSTART-")) {
                document = new Document(line);
            } else {
                assert document != null;
                document.addToken(line.trim());
            }
        }

        PrintWriter outputWriter = new PrintWriter(Files.newBufferedWriter(Paths.get("yagotaskb.tsv")));


        TagmeConfig.init();
        for (final Document doc : ProgressBar.wrap(corpus, "Annotating dataset...")) {

            final List<Annotation> annotations = EvaluateOnConll2003YAGO.annotateText(doc.getText(), threshold);
            outputWriter.println(doc.getId());
            final TopicSearcher searcher = new TopicSearcher("en");

            for (final Annotation annotation : annotations) {
                int wikipediaId = annotation.getTopic();
                String entity = searcher.getTitle(wikipediaId);
                outputWriter.println(annotation.getStart() + "\t" + entity + "\t" + "http://en.wikipedia.org/wiki/" + entity + "\t" + wikipediaId + "\t");
            }
            outputWriter.println();

        }
        outputWriter.flush();
    }


    private static class Document {
        private final String id;
        private String text;

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


