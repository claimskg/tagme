package org.gesis;

import it.acubelab.tagme.*;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum AnnotateDataset {
    ;


    @SuppressWarnings("LocalVariableOfConcreteClass")
    private static String annotateText(final String text, double threshold) {
        String lang = "en";

        AnnotatedText ann_text = new AnnotatedText(text);

        RelatednessMeasure relatednessMeasure = RelatednessMeasure.create(lang);

        final AnnotationFilter filter = (a -> a.isDisambiguated() && a.getRho() >= threshold);

        try {
            TagmeParser parser = new TagmeParser(lang, true);

            Disambiguator disambiguator = new Disambiguator(lang);
            Segmentation segmentation = new Segmentation();
            RhoMeasure rho = new RhoMeasure();

            parser.parse(ann_text);
            segmentation.segment(ann_text);
            disambiguator.disambiguate(ann_text, relatednessMeasure);
            rho.calc(ann_text, relatednessMeasure);

            List<Annotation> tagMeAnnotations = ann_text.getAnnotations().stream()
                    .filter(filter).collect(Collectors.toList());


            return AnnotateDataset.annotationsToJson(tagMeAnnotations, ann_text, lang);
        } catch (IOException ignored) {
            return "";
        }
    }

    public static void main(String[] args) throws IOException {

        TagmeConfig.init();


        final Table dataset = Table.read().csv(args[0]);
        final double threshold = Double.valueOf(args[1]);
        final StringColumn entityColumn = (StringColumn) dataset.column("extra_entities_claimReview_claimReviewed");
        final StringColumn bodyEntityColumn = (StringColumn) dataset.column("extra_entities_body");
        for (final Row row : dataset) {

            final String claimReviewTitle = row.getString("claimReview_claimReviewed");
            final String reviewBody = row.getString("extra_body");

            entityColumn.set(row.getRowNumber(),
                    AnnotateDataset.annotateText(claimReviewTitle, threshold));

            bodyEntityColumn.set(row.getRowNumber(),
                    AnnotateDataset.annotateText(reviewBody, threshold));

        }

        dataset.write().csv(CsvWriteOptions.builder(args[0])
                .header(true)
                .escapeChar('\\')
                .quoteChar('"')
                .separator(',')
                .lineEnd(System.lineSeparator()).build());
    }

    @SuppressWarnings({"LocalVariableOfConcreteClass", "MethodParameterOfConcreteClass", "FeatureEnvy"})
    private static String annotationsToJson(final Iterable<? extends Annotation> annotations, AnnotatedText ann_text, String lang) throws IOException {
        TopicSearcher searcher = new TopicSearcher(lang);
        StringBuilder jsonBuilder = new StringBuilder();

        for (Annotation annotation : annotations) {

            int wikipediaId = annotation.getTopic();
            int begin = annotation.getStart();
            int end = annotation.getEnd();
            String entity = searcher.getTitle(wikipediaId);
            String text = ann_text.getOriginalText(annotation);
            double score = annotation.getRho();
            String[] categories = searcher.getCategories(wikipediaId);

            jsonBuilder.append(String.format("{\n" +
                            "\t\"id\" : %d,\n " +
                            "\t\"begin\": %d,\n" +
                            "\t\"end\": %d,\n" +
                            "\t\"entity\": \"%s\",\n" +
                            "\t\"text\": \"%s\" ,\n" +
                            "\t\"score\": %.2f, \n" +
                            "\ts\"categories\" : [%s]}",
                    wikipediaId,
                    begin,
                    end,
                    entity,
                    text,
                    score,
                    Arrays.stream(categories).map(c -> "\"" + c + "\"").collect(Collectors.joining(","))));

        }

        return "[\n" + jsonBuilder + "\n]";
    }

    @FunctionalInterface
    private interface AnnotationFilter extends Predicate<Annotation> {

    }
}


