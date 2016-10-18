package aia.stream;

import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletionStage;


/**
 * Created by Murtuza on 8/25/2016.
 */
public class StreamingCopy {

    public static void main(String[] args) throws IOException {

        ActorSystem system = ActorSystem.create("system");
        ActorMaterializer mat = ActorMaterializer.create(system);

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromFile(new File("C:\\temp\\blah.txt"));

        Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toFile(new File("c:\\temp\\blah.copied.txt"));

        RunnableGraph<CompletionStage<IOResult>> runnableGraph0 = source.to(sink);

        CompletionStage<IOResult> cs = runnableGraph0.run(mat).handle((x, t) -> {
            if (t != null) {
                System.out.println("Error: " + t);
            }
            if (x != null) {
                String s = String.format("%s , %s bytes read", x.status(), x.count());
                System.out.println(s);
            }
            return null;
        });

        RunnableGraph<CompletionStage<IOResult>> runnableGraph1 = source.toMat(sink, Keep.left());

        cs = runnableGraph1.run(mat).handle((x, t) -> {
            if (t != null) {
                System.out.println("Error: " + t);
            }
            if (x != null) {
                String s = String.format("%s , %s bytes read", x.status(), x.count());
                System.out.println(s);
            }
            return null;
        });

        RunnableGraph<CompletionStage<IOResult>> runnableGraph2 = source.toMat(sink, Keep.right());
        cs = runnableGraph2.run(mat).handle((x, t) -> {
            if (t != null) {
                System.out.println("Error: " + t);
            }
            if (x != null) {
                String s = String.format("%s , %s bytes written", x.status(), x.count()); // Here keep right was used i.e. sink, which writes data
                System.out.println(s);
            }
            return null;
        });


        RunnableGraph<Pair<CompletionStage<IOResult>, CompletionStage<IOResult>>> pairRunnableGraph = source.toMat(sink, Keep.both());

        Pair<CompletionStage<IOResult>, CompletionStage<IOResult>> pair = pairRunnableGraph.run(mat);
        pair.first().handle((x, t) -> {
            if (t != null) {
                System.out.println("Error: " + t);
            }
            if (x != null) {
                String s = String.format("%s , %s bytes read", x.status(), x.count()); // Here keep right was used i.e. sink, which writes data
                System.out.println(s);
            }
            return null;
        });


        pair.second().handle((x, t) -> {
            if (t != null) {
                System.out.println("Error: " + t);
            }
            if (x != null) {
                String s = String.format("%s , %s bytes written", x.status(), x.count()); // Here keep right was used i.e. sink, which writes data
                System.out.println(s);
            }
            return null;
        });
    }


}
