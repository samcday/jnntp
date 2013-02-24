package au.com.samcday.jnntp;

import java.util.Iterator;

public class OverviewList implements Iterable<Overview> {
    Iterator<Overview> iter;

    public OverviewList(Iterator<Overview> iter) {
        this.iter = iter;
    }

    @Override
    public Iterator<Overview> iterator() {
        return this.iter;
    }
}
