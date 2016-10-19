package aia.stream;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.io.File;
import java.util.concurrent.CompletionStage;

/**
 * Created by Murtuza on 10/19/2016.
 */
public class ReadParseFilterSerializeMain {

    public static void main(String[] args) {

        State filteredState = new State("ok");
        ActorSystem system = ActorSystem.create("system");
        ActorMaterializer mat = ActorMaterializer.create(system);

        /*
        A file formatted as the following is the inFile (source)
            my-host-1 | web-app | ok | 2015-08-12T12:12:00.127Z | 5 tickets sold.||
            my-host-2 | web-app | ok | 2015-08-12T12:12:01.127Z | 3 tickets sold.||
            my-host-1 | web-app | ok | 2015-08-12T12:12:02.127Z | 1 tickets sold.||
            my-host-2 | web-app | error | 2015-08-12T12:12:03.127Z | exception!!||
         */
        File inFile = new File("C:\\temp\\blah.txt");
        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(inFile);
        /*
        The json output will be stored in the outfile (sink)
         */
        File outFile = new File("c:\\temp\\blah.copied.txt");
        Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toFile(outFile);

        /*
         * Read from file and delimit data based on lineseparator. The input is
         * a byteString and the output is a string
         */
        Flow<ByteString, String, NotUsed> frame = Framing
                .delimiter(ByteString.fromString(System.lineSeparator()), 200, FramingTruncation.ALLOW)
                .map(x -> {

                    System.out.println(x.decodeString("UTF8"));
                    return x.decodeString("UTF8");
                });
        /*
         * Each frame is parsed into a Event object. The input is a string and
         * output is a Event. I don't know how to use collect to ignore empty or
         * bad lines as shown in the scala sample.
         * So I assume the file is fine and the lines are fine. Since I don't
         * know usage of collect I am simply doing a get() to get the event from
         * the Option.
         */
        Flow<String, Event, NotUsed> parse = Flow.of(String.class)
                .map(x -> {
                    //System.out.println(LogStreamProcessor.parseLineEx(x).get());
                    return LogStreamProcessor.parseLineEx(x).get();

                });

        /*
         * Which events from the previous state need to be kept, determined by
         * the filteredstate. Input is event and output is filtered Events.
         */
        Flow<Event, Event, NotUsed> filter = Flow.of(Event.class)
                .filter(evt -> {
                    //System.out.println(evt.getState().equals(filteredState));
                    return evt.getState().equals(filteredState);


                });

        /*
         * Input is filtered events and output is a serialized form, instead of
         * using spray json, I am simply using the toString of the Event class
         * to generate the json string, that is converted and outputted as a
         * bytestring.
         */
        Flow<Event, ByteString, NotUsed> serialize = Flow.of(Event.class)
                .map(evt ->
                {
                    //System.out.println("Hello");
                    return ByteString.fromString(evt.toString());
                });

        /*
         * All the previous independent flows are connected using via,
         * description of each flow with input and output is commented above.
         */
        Flow<ByteString, ByteString, NotUsed> composedFlow = frame.via(parse).via(filter).via(serialize);
       // Flow<ByteString, ByteString, NotUsed> composedFlow = frame.via(parse).via(serialize);
        /*
         * Now attache the source, which is our log file to the composed flow
         * and use the output file sink.
         */
        RunnableGraph<CompletionStage<IOResult>> runnableGraph = source.via(composedFlow).toMat(sink, Keep.right());


        /*
        Code written up until now is a blueprint, nothing happens till you actually call the run method on the graph
         */
        runnableGraph.run(mat).handle((ioresult, throwable) -> {
            /*
            If there was an error throwable is not null, if it ran successfully ioresult will be not null and throwable will be null
             */
            if (throwable != null) {
                System.out.println("Error: " + throwable);
            }
            if (ioresult != null) {
                String s = String.format("Wrote %s bytes to %s", ioresult.count(), outFile.getAbsolutePath());
                System.out.println(s);
            }
            return null;
        });

        try {
            Thread.sleep(10000); // If I don't wait, the app terminates before the threads complete the writing
        }
        catch (Exception e){}

    }
}
