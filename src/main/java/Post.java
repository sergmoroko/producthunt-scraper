
class Post {
    private String name = "";
    private String id = "";
    private String url = "";
    private int comments =0;

    Post(){

    }


    Post(String name, String id, int comments, String url){
        this.name = name;
        this.id = id;
        this.comments = comments;
        this.url = url;
    }

    int getComments() {
        return comments;
    }

    void setComments(int comments) {
        this.comments = comments;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }
}
