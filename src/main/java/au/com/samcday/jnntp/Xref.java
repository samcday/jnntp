package au.com.samcday.jnntp;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Xref {
    private static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults();
    private static final Splitter COLON_SPLITTER = Splitter.on(':').trimResults();

    private String serverName;
    private List<Location> locations;

    public static Xref parse(String str) {
        Iterator<String> mainParts = SPACE_SPLITTER.split(str).iterator();

        String serverName = mainParts.next();

        List<Location> locations = new ArrayList<>();
        while(mainParts.hasNext()) {
            Iterator<String> locationParts = COLON_SPLITTER.split(mainParts.next()).iterator();
            locations.add(new Location(locationParts.next(), Long.parseLong(locationParts.next())));
        }

        return new Xref(serverName, locations);
    }

    public Xref(String serverName, List<Location> locations) {
        this.serverName = serverName;
        this.locations = locations;
    }

    public String getServerName() {
        return serverName;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public static class Location {
        private String group;
        private Long article;

        public Location(String group, Long article) {
            this.group = group;
            this.article = article;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public Long getArticle() {
            return article;
        }

        public void setArticle(Long article) {
            this.article = article;
        }
    }
}
