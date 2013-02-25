package au.com.samcday.jnntp;

import com.google.common.collect.AbstractIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class OverviewList implements Iterable<Overview> {
    private Iterator<Overview> iter;
    private List<Long> missingArticles;
    private Long lastArticleNum;
    private LinkedBlockingQueue<Overview> items;
    private boolean done;

    public OverviewList(LinkedBlockingQueue<Overview> items) {
        this.missingArticles = new ArrayList<>();
        this.items = items;
        this.iter = new OverviewIterator();
    }

    @Override
    public Iterator<Overview> iterator() {
        if(this.iter == null) {
            throw new RuntimeException("OverviewList can only be iterated once.");
        }
        Iterator<Overview> ret = this.iter;
        this.iter = null;
        return ret;
    }

    public List<Long> getMissingArticles() {
        if(!this.done) {
            throw new RuntimeException("Cannot retrieve missing article numbers until this OverviewList has been completely iterated.");
        }
        return this.missingArticles;
    }

    private class OverviewIterator extends AbstractIterator<Overview> {
        @Override
        protected Overview computeNext() {
            while(true) {
                try {
                    Overview item = items.take();
                    if(item == Overview.END) {
                        done = true;
                        return this.endOfData();
                    }

                    // TODO: is it safe to assume articles will arrive in order?
                    if(lastArticleNum != null) {
                        for(long i = lastArticleNum + 1; i < item.getArticle() - 1; i++) {
                            missingArticles.add(i);
                        }
                    }
                    lastArticleNum = item.getArticle();

                    return item;
                }
                catch(InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
