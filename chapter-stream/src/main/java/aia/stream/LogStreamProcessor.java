package aia.stream;

import akka.japi.Option;
import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletionStage;

/**
 * Created by Murtuza on 10/19/2016.
 */
public class LogStreamProcessor {

    public static Option<Event> parseLineEx(String line) throws Exception {

        //my-host-1 | web-app | ok | 2015-08-12T12:12:00.127Z | 5 tickets sold.||
        // parse line delimited by the pipe | symbol.

        if (!line.isEmpty()) {
            Event evt = new Event();
            String[] arr = line.split("\\|");

            evt.setHost(arr[0].trim());
            evt.setService(arr[1].trim());
            evt.setState(new State(arr[2].trim()));
            evt.setTime(ZonedDateTime.parse(arr[3].trim()));
            evt.setDescription(arr[4].trim());
            if (arr.length > 5) {
                if (!arr[5].trim().isEmpty()) {
                    evt.setTag(new Option.Some<String>(String.valueOf(arr[5].trim())));
                } else evt.setTag(Option.none());
            } else evt.setTag(Option.none());
            if (arr.length > 6) {

                if (!arr[6].trim().isEmpty()) {
                    evt.setMetric(new Option.Some<Double>(Double.valueOf(arr[6].trim())));
                } else evt.setMetric(Option.none());
            } else evt.setMetric(Option.none());

            return new Option.Some<Event>(evt);
        } else

            return Option.none();
    }

    public static String logLine(Event event) {
         return String.format("%s|%s|%s|%s|%s|||%n",event.getHost(),event.getService(),event.getState().stateStr,event.getTime(),event.getDescription());
    }

    /**
     * Returns a Source of log lines from File.
     */
    public static Source<String, CompletionStage<IOResult>> logLines(Path path) {

        return delimitedText(FileIO.fromPath(path), 1024 * 1024);
    }


    /**
     * Returns a Source of Framed byte strings using "\r\n" or "\n" depenedent on your systems line sep as a delimiter.
     */
    public static <T> Source<String, T> delimitedText(Source<ByteString, T> source, int maxLine) {
        return convertToString(source.via(Framing.delimiter(ByteString.fromString(System.lineSeparator()), maxLine)));

    }

    /**
     * Converts (previously framed) ByteStrings to String.
     */
    public static <T> Source<String, T> convertToString(Source<ByteString, T> source) {
        return source.map(x -> x.decodeString("UTF8"));

    }

}





