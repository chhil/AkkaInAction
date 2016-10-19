package aia.stream;

import akka.japi.Option;

import java.time.ZonedDateTime;

/**
 * This does not absolutely mimic https://github.com/RayRoestenburg/akka-in-action/blob/master/chapter-stream/src/main/scala/aia/stream/Event.scala
 * But the idea of datastructure is similar.
 * Created by Murtuza on 10/18/2016.
 */
public class Event {


    private String host;
    private String service;
    private State state;
    private ZonedDateTime time;
    private String description;
    private Option<String> tag = Option.none();
    private Option<Double> metric = Option.none();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Option<String> getTag() {
        return tag;
    }

    public void setTag(Option<String> tag) {
        this.tag = tag;
    }

    public Option<Double> getMetric() {
        return metric;
    }

    public void setMetric(Option<Double> metric) {
        this.metric = metric;
    }

    @Override
    public String toString() {
        return String.format("%n{%nhost:%s%n" +
                "service:%s%n" +
                "state:%s%n" +
                "zoneddate:%s%n" +
                "description:%s%n" +
                "tag:%s%n" +
                "metric%s%n}", getHost(), getService(), getState(), getTime(), getDescription(), getTag(), getMetric());
    }
}
