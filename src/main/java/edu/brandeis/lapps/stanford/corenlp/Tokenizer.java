package edu.brandeis.lapps.stanford.corenlp;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.LifException;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.util.List;

import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 *
 * @author Chunqi SHI (shicq@cs.brandeis.edu)
 * @author Keigh Rim (krim@brandeis.edu)
 * @since 2014-03-25
 *
 */
@org.lappsgrid.annotations.ServiceMetadata(
        description = "This service is a wrapper around Stanford CoreNLP 3.3.1 providing a tokenizer service" +
                "\nInternally it uses CoreNLP default \"tokenize\", \"ssplit\" annotators.",
        requires_format = { "text", "lif" },
        produces_format = { "lif" },
        produces = { "token" }
)
public class Tokenizer extends AbstractStanfordCoreNLPWebService {

    public Tokenizer() {
        this.init(PROP_TOKENIZE, PROP_SENTENCE_SPLIT);
    }

    @Override
    public String execute(Container container) {

        String text = container.getText();
        View view = null;
        try {
            view = container.newView();
        } catch (LifException ignored) {
            // this never raises as newView() will check for duplicate view-id internally
        }
        view.addContains(Uri.TOKEN,
                String.format("%s:%s", this.getClass().getName(),getVersion()),
                "tokenizer:stanford");

        // run stanford module
        edu.stanford.nlp.pipeline.Annotation annotation
                = new edu.stanford.nlp.pipeline.Annotation(text);
        snlp.annotate(annotation);
        int sid = 0;
        List<CoreMap> sents = annotation.get(SentencesAnnotation.class);
        for (CoreMap sent : sents) {
            int tid = 0;
            for (CoreLabel token : sent.get(TokensAnnotation.class)) {
                Annotation ann = view.newAnnotation(
                        String.format("%s%d_%d", TOKEN_ID, sid, tid), Uri.TOKEN,
                        token.beginPosition(), token.endPosition());
                tid++;
                // TODO: 3/1/2018 this should go away when we complete ditch the "top-level" label field in LIF scheme
                ann.setLabel("token");
                ann.addFeature("word", token.value());
            }
            sid++;
        }
        // set discriminator to LIF
        Data<Container> data = new Data<>(Uri.LIF, container);
        return Serializer.toJson(data);
    }

}
