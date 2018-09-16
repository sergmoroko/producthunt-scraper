
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class Scraper {
    private static final String INPUT_FILE_NAME = "./input.txt";
    private static final String OUTPUT_FILE_NAME = "./";
    private static final String FIELD_DELIMITER = "\\^";
    private static final String HEADER = "Post Name^ID^Name^Username^Twitter Username^Headline^Is Active";

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {

        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        ArrayList<String> links = readFromFile();

        HashSet<Post> posts = getPosts(links);
        ArrayList<String> lines = new ArrayList<>();

        int counter = 0;

        for (Post post : posts) {
            ArrayList<User> users = new ArrayList<>();
            users.addAll(getUpvotedUsers(post));
            users.addAll(getCommentedUsers(post));

            for (User user : users) {

                lines.add(post.getName() + "^" + user.toString());
            }

            System.out.println(++counter + " post(s) scraped successfully");

        }

        writeToExcel(lines, "test");


    }

    private static String getTimeStamp() {
        LocalDateTime time = LocalDateTime.now();
        return time.format(DateTimeFormatter.ofPattern("MM_dd_HH_mm"));
    }

    private static ArrayList<User> getCommentedUsers(Post post) throws InterruptedException, JSONException, IOException {
        ArrayList<User> users = new ArrayList<>();
        ArrayList<String> profiles = getCommentersProfiles(post);

        for (String s : profiles) {
            users.add(getUser(s));
        }

        return users;

    }

    private static ArrayList<User> getUpvotedUsers(Post post) throws IOException {
        ArrayList<String> jsons = getUpvoteJsons(post.getId());
        ArrayList<User> jsonToPersonList = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();

        for (String s : jsons) {

            TypeReference<List<User>> mapType = new TypeReference<List<User>>() {
            };

            jsonToPersonList.addAll(objectMapper.readValue(s, mapType));
        }
        return jsonToPersonList;
    }

    private static void addPostData(Post post) throws IOException, JSONException {

        String id = post.getId();

        HttpClient httpclient = HttpClients.createDefault();

        HttpGet request = new HttpGet("https://www.producthunt.com/frontend/posts/" + id + "?id=" + id);


        HttpResponse response = httpclient.execute(request);
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        String output;
        String json = "";

        while ((output = br.readLine()) != null) {
            json = output;
        }

        httpclient.getConnectionManager().shutdown();

        JSONObject obj = new JSONObject(json);
        post.setComments(obj.getJSONObject("post").getInt("comments_count"));
        post.setUrl(obj.getJSONObject("post").getString("url"));
        post.setName(obj.getJSONObject("post").getString("name"));
        post.setUrl("https://www.producthunt.com/posts/" + obj.getJSONObject("post").getString("slug"));

    }

    private static HashSet<Post> getPosts(ArrayList<String> links) throws JSONException, InterruptedException, IOException {

        HashSet<Post> posts = new HashSet<>();
        ArrayList<String> postLinks = new ArrayList<>();

        for (String link : links) {
            if (link.contains("topics/")) {
                posts.addAll(PostsScraper.getAllPosts(link));

            } else {
                postLinks.add(link);
            }

        }

        for (String link : postLinks) {

            Post post = new Post();
            post.setId(getPostID(link));
            addPostData(post);
            posts.add(post);

        }

        return posts;
    }


    private static ArrayList<String> getUpvoteJsons(String id) throws IOException {

        ArrayList<String> jsons = new ArrayList<>();

        int offset = 0;
        while (true) {
            HttpClient httpclient = HttpClients.createDefault();
            HttpResponse response = httpclient.execute(new HttpGet("https://www.producthunt.com/frontend/votes/?subject_type=Post&subject_id=" + id + "&offset=" + offset + "&limit=50"));
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity, "UTF-8");

            if (!json.equals("[]")) {
                jsons.add(json);

                offset += 50;
            } else {
                break;
            }
        }

        return jsons;
    }


    private static String getPostID(String url) throws IOException {

        HttpClient httpclient = HttpClients.createDefault();
        HttpResponse response = httpclient.execute(new HttpGet(url));
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        httpclient.getConnectionManager().shutdown();

        String scr = "<script>window.mobileAppUrl = 'producthunt://post/";

        responseString = responseString.substring(responseString.indexOf(scr) + scr.length());

        return responseString.substring(0, responseString.indexOf('\''));

    }


    private static ArrayList<String> readFromFile() throws IOException {
        ArrayList<String> products = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(INPUT_FILE_NAME), StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            products.add(line);
        }
        br.close();
        return products;
    }

    private static void writeToExcel(ArrayList<String> arrayList, String fileName) {
        ArrayList<Object[]> cellList = new ArrayList<>();

        for (String s : arrayList) {
            if (!s.equals("")) {
                cellList.add(stringToArray(s));
            }
        }

        //Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();

        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet("Sheet 1");

        //This data needs to be written (Object[])
        Map<String, Object[]> data = new TreeMap<>();
        data.put("0", stringToArray(HEADER));

        for (int i = 1; i <= cellList.size(); i++) {
            data.put(String.valueOf(i), cellList.get(i - 1));
        }


        //Iterate over data and write to sheet
        Set<String> keyset = data.keySet();

        int rowNum = 0;
        for (String key : keyset) {
            //create a row of excelsheet
            Row row = sheet.createRow(rowNum++);

            //get object array of prerticuler key
            Object[] objArr = data.get(key);

            int cellNum = 0;

            for (Object obj : objArr) {
                Cell cell = row.createCell(cellNum++);
                cell.setCellValue((String) obj);

            }
        }
        try {
            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(new File(OUTPUT_FILE_NAME + "producthunt_" + getTimeStamp() + ".xlsx"));
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object[] stringToArray(String s) {
        return s.split(FIELD_DELIMITER);
    }


    private static String getComments(String referer, String cursor, String id) throws IOException {

        byte[] byteID = Base64.encodeBase64(("Post-" + id).getBytes());
        String postID = new String(byteID);


        HttpClient httpclient = HttpClients.createDefault();

        HttpPost request = new HttpPost("https://www.producthunt.com/frontend/graphql");

        request.addHeader("Accept", "*/*");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("content-type", "application/json");
        request.addHeader("Host", "www.producthunt.com");
        request.addHeader("Origin", "https://www.producthunt.com");
        request.addHeader("Referer", referer);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36 OPR/46.0.2597.57");
        request.addHeader("x-requested-with", "XMLHttpRequest");

        String payload = "{\"query\":\"query CommentListPage($id: ID!, $threadCursor: String, $threadLimit: Int!, $threadOrder: ThreadOrder!) {\\n  node(id: $id) {\\n    id\\n    ...CommentList\\n    __typename\\n  }\\n}\\n\\nfragment CommentList on Commentable {\\n  _id\\n  id\\n  threads(first: $threadLimit, after: $threadCursor, order: $threadOrder) {\\n    edges {\\n      node {\\n        _id\\n        id\\n        ...CommentListThread\\n        __typename\\n      }\\n      __typename\\n    }\\n    pageInfo {\\n      endCursor\\n      hasNextPage\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CommentListThread on Comment {\\n  _id\\n  id\\n  is_sticky\\n  replies(first: 20) {\\n    edges {\\n      node {\\n        _id\\n        id\\n        ...CommentListThreadComment\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  ...CommentListThreadComment\\n  __typename\\n}\\n\\nfragment CommentListThreadComment on Comment {\\n  _id\\n  id\\n  body\\n  body_html\\n  can_edit\\n  can_reply\\n  created_at\\n  path\\n  subject {\\n    ... on Commentable {\\n      _id\\n      id\\n      __typename\\n    }\\n    __typename\\n  }\\n  user {\\n    _id\\n    id\\n    headline\\n    name\\n    username\\n    ...UserSpotlight\\n    __typename\\n  }\\n  ...CommentListThreadCommentDestroyButton\\n  ...CommentListThreadCommentVoteButton\\n  ...CommentListThreadPostNoticeComment\\n  ...FacebookShare\\n  __typename\\n}\\n\\nfragment CommentListThreadCommentDestroyButton on Comment {\\n  _id\\n  id\\n  can_destroy\\n  __typename\\n}\\n\\nfragment CommentListThreadCommentVoteButton on Comment {\\n  _id\\n  id\\n  ... on Votable {\\n    _id\\n    id\\n    has_voted\\n    votes_count\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CommentListThreadPostNoticeComment on Comment {\\n  _id\\n  id\\n  user {\\n    _id\\n    id\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment FacebookShare on Shareable {\\n  _id\\n  id\\n  url\\n  __typename\\n}\\n\\nfragment UserSpotlight on User {\\n  id\\n  headline\\n  name\\n  username\\n  ...UserImageLink\\n  __typename\\n}\\n\\nfragment UserImageLink on User {\\n  username\\n  ...UserImage\\n  __typename\\n}\\n\\nfragment UserImage on User {\\n  id\\n  post_upvote_streak\\n  __typename\\n}\\n\",\"variables\":{\"id\":\"" + postID + "\",\"threadLimit\":5,\"threadOrder\":\"VOTES\",\"threadCursor\":\"" + cursor + "\"},\"operationName\":\"CommentListPage\"}";

        request.setEntity(new StringEntity(payload));

        HttpResponse response = httpclient.execute(request);
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        String output;
        String json = "";

        while ((output = br.readLine()) != null) {
            json = output;
        }

        httpclient.getConnectionManager().shutdown();

        return json;

    }

    private static ArrayList<String> getCommentersProfiles(Post post) throws IOException, InterruptedException {

        ArrayList<String> comments = new ArrayList<>();
        ArrayList<String> commenters = new ArrayList<>();
        for (int i = 0; i <= post.getComments() - 1; i += 5) {
            byte[] c = org.apache.commons.codec.binary.Base64.encodeBase64(String.valueOf(i).getBytes());
            String cursor = new String(c);
            comments.add(getComments(post.getUrl(), cursor, post.getId()));
            Thread.sleep(1000);
        }

        for (String s : comments) {
            commenters.addAll(getProfiles(s));
        }

        return commenters;
    }

    private static User getUser(String username) throws IOException {
        User user = new User();
        user.setUsername(username);

        try {

            HtmlUnitDriver driver = new HtmlUnitDriver();
            driver.get("https://www.producthunt.com/@" + username);

            if (!driver.findElements(By.xpath("//h1")).isEmpty()) {
                user.setName(driver.findElement(By.xpath("//h1")).getText());
            }

            if (!driver.findElements(By.xpath("//p")).isEmpty()) {
                user.setHeadline(driver.findElement(By.xpath("//p")).getText());
            }

            if (!driver.findElements(By.xpath("//header/div/div/span")).isEmpty()) {
                String id = driver.findElement(By.xpath("//header/div/div/span")).getText();
                user.setId(id.substring(1));
            }

            if (!driver.findElements(By.cssSelector("a.twitter_afdc4")).isEmpty()) {
                String twitterLink = driver.findElement(By.cssSelector("a.twitter_afdc4")).getAttribute("href");
                String s = ".com/";
                user.setTwitter_username(twitterLink.substring(twitterLink.indexOf(s) + s.length()));
            }

            driver.quit();

        } catch (Exception e) {
//
        }

        return user;

    }

    private static ArrayList<String> getProfiles(String json) {

        ArrayList<String> users = new ArrayList<>();
        try {

            JSONObject obj = new JSONObject(json);
            JSONArray edges = obj.getJSONObject("data").getJSONObject("node").getJSONObject("threads").getJSONArray("edges");

            for (int i = 0; i < edges.length(); i++) {

                users.add(edges.getJSONObject(i).getJSONObject("node").getJSONObject("user").getString("username"));

                JSONArray replies = edges.getJSONObject(i).getJSONObject("node").getJSONObject("replies").getJSONArray("edges");

                for (int j = 0; j < replies.length(); j++) {
                    users.add(replies.getJSONObject(j).getJSONObject("node").getJSONObject("user").getString("username"));
                }
            }

        } catch (Exception e) {
//
        }

        return users;
    }
}

