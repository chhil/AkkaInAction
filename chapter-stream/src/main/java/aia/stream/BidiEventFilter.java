package aia.stream;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.SupervisorStrategy;
import akka.japi.function.Function;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.io.File;
import java.util.concurrent.CompletionStage;

/**
 * Created by Murtuza on 10/21/2016.
 */
public class BidiEventFilter {


    public static void main(String[] args) {
        State filteredState = new State("ok");

        ActorSystem system = ActorSystem.create("system");
        ActorMaterializer mat = ActorMaterializer.create(system);
        Function<Throwable, SupervisorStrategy.Directive> decider = thr -> SupervisorStrategy.resume();


        /*
        A file formatted as the following is the inFile (source)
            my-host-1 | web-app | ok | 2015-08-12T12:12:00.127Z | 5 tickets sold.||
            my-host-2 | web-app | ok | 2015-08-12T12:12:01.127Z | 3 tickets sold.||
            my-host-1 | web-app | ok | 2015-08-12T12:12:02.127Z | 1 tickets sold.||
            my-host-2 | web-app | error | 2015-08-12T12:12:03.127Z | exception!!||
         */
        File inFile = new File("C:\\temp\\blah.txt");
        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(inFile.toPath());
        /*
        The json output will be stored in the outfile (sink)
         */
        File outFile = new File("c:\\temp\\blah.copied.txt");
        Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toPath(outFile.toPath());

        /*
        If you compare this with EventFilter flows, its frame and parse flows have been combined into one fluent flow.
        Note the Flow definition is In1,Out1,Mat1
         */
        Flow<ByteString, Event, NotUsed> inFlow = Framing.delimiter(ByteString.fromString(System.lineSeparator()), 100, FramingTruncation.ALLOW)
                .map(bs -> bs.decodeString("UTF8"))
                .map(x -> {
                    //System.out.println(LogStreamProcessor.parseLineEx(x).get());
                    return LogStreamProcessor.parseLineEx(x).get();

                });

        //Note the Flow definition is In2,Out2,Mat2
        Flow<Event, ByteString, NotUsed> outFlow = Flow.of(Event.class).map(event ->
                ByteString.fromString(LogStreamProcessor.logLine(event)));

        // Bidiflow takes in1 out1,In2,out2,mat1,mat2

        /**
         * Wraps two Flows to create a ''BidiFlow''. The materialized value of the resulting BidiFlow is NotUsed.
         *
         * {{{
         *     +----------------------------+
         *     | Resulting BidiFlow         |
         *     |                            |
         *     |  +----------------------+  |
         * I1 ~~> |        Flow1         | ~~> O1
         *     |  +----------------------+  |
         *     |                            |
         *     |  +----------------------+  |
         * O2 <~~ |        Flow2         | <~~ I2
         *     |  +----------------------+  |
         *     +----------------------------+
         * }}}
         *
         */
        BidiFlow<ByteString, Event, Event, ByteString, NotUsed> bidiFlow = BidiFlow.fromFlows(inFlow, outFlow);

        /*
        Filter taken straight out out of the previous EventFilter class
         */
        Flow<Event, Event, NotUsed> filter = Flow.of(Event.class)
                .filter(evt -> {
                    //System.out.println(evt.getState().equals(redState));
                    return evt.getState().equals(filteredState);


                });

        /**
         * Add the given Flow as the final step in a bidirectional transformation
         * pipeline. By convention protocol stacks are growing to the left: the right most is the bottom
         * layer, the closest to the metal.
         * {{{
         *     +---------------------------+
         *     | Resulting Flow            |
         *     |                           |
         *     |  +------+        +------+ |
         * I1 ~~> |      |  ~O1~> |      | |
         *     |  | bidi |        |filter| |
         * O2 <~~ |      | <~I2~  |      | |
         *     |  +------+        +------+ |
         *     +---------------------------+
         * }}}
         */

        Flow<ByteString, ByteString, NotUsed> flow = bidiFlow.join(filter);


        RunnableGraph<CompletionStage<IOResult>> runnableGraph = source.via(flow).toMat(sink, Keep.right());


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


    }

}


