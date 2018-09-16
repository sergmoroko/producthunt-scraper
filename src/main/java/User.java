
public class User {
    private String name = "";
    private String id = "";
    private String username = "";
    private String twitter_username = "";
    private String headline = "";
    private String trashed = "";

    private String getName() {
        if(name != null) {
        return name.equals("null") ? "" : name;
    }
        return "";
    }

    void setName(String name) {
        this.name = name;
    }

    private String getUsername() {
        if(username != null) {
        return username.equals("null") ? "" : username;
        }
        return "";
    }

    void setUsername(String username) {
        this.username = username;
    }

    private String getTwitter_username() {
        if(twitter_username != null) {
            return twitter_username.equals("null") ? "" : twitter_username;
        }
        return "";
    }

    void setTwitter_username(String twitter_username) {
        this.twitter_username = twitter_username;
    }

    private String getHeadline() {
        if(headline != null) {
            return headline.equals("null") ? "" : headline;
        }
        return "";
    }

    void setHeadline(String headline) {
        this.headline = headline;
    }

    private String getTrashed() {
        if(trashed != null) {
        if (trashed.equals("false")){
            return "yes";
        }
        if(trashed.equals("true") ) {
            return "no";
        }

        }
        return "";
    }


    private String getId() {
        if(id != null) {
        return id.equals("null") ? "" : id;
        }
        return "";
    }

    void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {

        return getId() + "^" + getName() + "^" + getUsername() + "^"
                + getTwitter_username() + "^" + getHeadline() + "^" + getTrashed();
    }
}
