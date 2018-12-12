package org.gesis;

import it.acubelab.tagme.*;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.TopicSearcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum AnnotateDataset {
    ;


    @SuppressWarnings("LocalVariableOfConcreteClass")
    private static String annotateTextJSON(final String text, double threshold) throws IOException {
        String lang = "en";

        AnnotatedText ann_text = new AnnotatedText(text);

        RelatednessMeasure relatednessMeasure = RelatednessMeasure.create(lang);

        final AnnotationFilter filter = (a -> a.isDisambiguated() && a.getRho() >= threshold);

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
        List<Annotation> filteredAnnotations = tagMeAnnotations.stream()
                .filter(filter).collect(Collectors.toList());


        return AnnotateDataset.annotationsToJson(filteredAnnotations, ann_text, lang);

    }

    @SuppressWarnings({"FeatureEnvy", "LawOfDemeter"})
    public static void main(String[] args) throws IOException {

        TagmeConfig.init();

        final File fileToLoad = new File(args[0]);
        final Dataset dataset = Dataset.csv(new File(args[0]));

        final double threshold = Double.valueOf(args[1]);
        for (final Row row : dataset) {

            final String claimReviewTitle = row.get("claimReview_claimReviewed");
            final String reviewBody = row.get("extra_body");

            String review_json_string = AnnotateDataset.annotateTextJSON(claimReviewTitle, threshold);
            System.err.println(review_json_string);
            row.set("extra_entities_claimReview_claimReviewed", review_json_string);

            String body_json_string = AnnotateDataset.annotateTextJSON(reviewBody, threshold);
            row.set("extra_entities_body", body_json_string);

        }

        dataset.write(fileToLoad);
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

            if (entity == null) {
                entity = "";
            }
            if (categories == null) {
                categories = new String[0];
            }

            jsonBuilder.append(String.format("{\n" +
                            "\t\"id\" : %d,\n " +
                            "\t\"begin\": %d,\n" +
                            "\t\"end\": %d,\n" +
                            "\t\"entity\": \"%s\",\n" +
                            "\t\"text\": \"%s\" ,\n" +
                            "\t\"score\": %.2f, \n" +
                            "\ts\"categories\" : [%s]\n}\n",
                    wikipediaId,
                    begin,
                    end,
                    entity,
                    text,
                    score,
                    Arrays.stream(categories).map(c -> "\"" + c + "\"").collect(Collectors.joining(","))));

        }
//        System.err.println(jsonBuilder.toString());
        return "[\n" + jsonBuilder + "\n]";
    }

    @FunctionalInterface
    private interface AnnotationFilter extends Predicate<Annotation> {

    }
}


