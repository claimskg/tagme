package org.gesis;

import it.acubelab.tagme.*;
import it.acubelab.tagme.config.TagmeConfig;
import it.acubelab.tagme.preprocessing.TopicSearcher;
import me.tongfei.progressbar.ProgressBar;

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


        return AnnotateDataset.annotationsToEscapedJson(filteredAnnotations, ann_text, lang);

    }

    @SuppressWarnings({"FeatureEnvy", "LawOfDemeter"})
    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Invalid number of arguments...");
            System.exit(1);
        }
        final File fileToLoad = new File(args[0]);
        final Dataset dataset = Dataset.csv(fileToLoad);
        final double threshold = Double.valueOf(args[1]);


        TagmeConfig.init();
        for (final Row row : ProgressBar.wrap(dataset, "Annotating dataset...")) {

            final String claimReviewTitle = row.get("claimReview_claimReviewed");
            final String reviewBody = row.get("extra_body");

            String review_json_string = AnnotateDataset.annotateTextJSON(claimReviewTitle, threshold);
            row.set("extra_entities_claimReview_claimReviewed", review_json_string);

            String body_json_string = AnnotateDataset.annotateTextJSON(reviewBody, threshold);
            row.set("extra_entities_body", body_json_string);

        }

        dataset.write(fileToLoad);
    }

    @SuppressWarnings({"LocalVariableOfConcreteClass", "MethodParameterOfConcreteClass", "FeatureEnvy"})
    private static String annotationsToEscapedJson(final Iterable<? extends Annotation> annotations, AnnotatedText ann_text, String lang) throws IOException {
        TopicSearcher searcher = new TopicSearcher(lang);
        StringBuilder jsonBuilder = new StringBuilder();
        boolean first = true;
        for (Annotation annotation : annotations) {
            if (first) {
                first = false;
            } else {
                jsonBuilder.append(",");
            }

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

            jsonBuilder.append(String.format("{\"id\" : %d\",\"\"begin\": %d,\"end\": %d,\"entity\": \"%s\"," +
                            "\"text\": \"%s\",\"score\": %.2f,\"categories\" : [%s]}",
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


