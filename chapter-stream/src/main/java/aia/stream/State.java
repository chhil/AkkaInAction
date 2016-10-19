package aia.stream;

/**
 * Created by Murtuza on 10/18/2016.
 */
public class State {

    protected String   stateStr;

    public State(String state) {


        this.stateStr = state.toLowerCase();

    }


    @Override
    public boolean equals(Object obj) {
        return this.stateStr.equalsIgnoreCase(((State)obj).stateStr);
    }
}
