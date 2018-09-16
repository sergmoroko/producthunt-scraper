import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class PostsScraper {

    public static void main(String[] args) throws IOException, InterruptedException, JSONException {

    }

    static ArrayList<Post> getAllPosts(String url) throws IOException, JSONException, InterruptedException {

        ArrayList<Post> posts = new ArrayList<>();

        String json = requestPosts(url, "MA==");


        int postsQty = getPostsQty(json);


        for (int i = 0; i <= postsQty - 1; i += 20) {
            byte[] offset = Base64.encodeBase64(String.valueOf(i).getBytes());
            String json1 = requestPosts(url, new String(offset));
            posts.addAll(getPostsFromJson(json1));
            Thread.sleep(1000);

        }

        return posts;

    }


    private static int getPostsQty(String json) throws JSONException {

        JSONObject obj = new JSONObject(json);
        return obj.getJSONObject("data").getJSONObject("topic").getInt("posts_count");

    }


    private static ArrayList<Post> getPostsFromJson(String json) throws JSONException {

        ArrayList<Post> posts = new ArrayList<>();

        JSONObject obj = new JSONObject(json);

        JSONArray jsonArray = obj.getJSONObject("data").getJSONObject("topic").getJSONObject("posts").getJSONArray("edges");

        ArrayList<String> list = new ArrayList<>();

        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i = 0; i < len; i++) {
                list.add(jsonArray.get(i).toString());
            }
        }

        for (String s : list) {
            JSONObject object = new JSONObject(s);
            int id = object.getJSONObject("node").getInt("id");
            String name = object.getJSONObject("node").getString("name");
            int comments = object.getJSONObject("node").getInt("comments_count");
            String slug = object.getJSONObject("node").getString("slug");

            Post post = new Post(name, String.valueOf(id), comments, "https://www.producthunt.com/posts/" + slug);
            posts.add(post);

        }

        return posts;
    }


    private static String requestPosts(String referer, String offset) throws IOException {

        String slug = referer.substring(referer.lastIndexOf("/") + 1);
        slug = slug.replace("[^a-zA-Z0-9]", "");

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

        String payload = "{\"query\":\"query TopicPage($slug: String!, $cursor: String, $query: String, $subtopic: Int, $order: String, $page: String!, $skipCards: Boolean!) {\\n  cards(first: 1, after: $cursor, kind: FEED, page: $page, page_slug: $slug) @skip(if: $skipCards) {\\n    edges {\\n      node {\\n        ...FeedCards\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  topic(slug: $slug) {\\n    id\\n    posts_count\\n    ...Header\\n    ...PostList\\n    ...MetaTags\\n    __typename\\n  }\\n}\\n\\nfragment PostList on Topic {\\n  name\\n  slug\\n  posts(first: 20, after: $cursor, query: $query, subtopic: $subtopic, order: $order) {\\n    edges {\\n      node {\\n        id\\n        ...PostItemList\\n        __typename\\n      }\\n      __typename\\n    }\\n    pageInfo {\\n      endCursor\\n      hasNextPage\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment PostItemList on Post {\\n  id\\n  ...PostItem\\n  __typename\\n}\\n\\nfragment PostItem on Post {\\n  id\\n  comments_count\\n  name\\n  shortened_url\\n  slug\\n  tagline\\n  ...CollectButton\\n  ...PostThumbnail\\n  ...PostVoteButton\\n  ...TopicFollowButtonList\\n  __typename\\n}\\n\\nfragment CollectButton on Post {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment PostThumbnail on Post {\\n  id\\n  thumbnail {\\n    id\\n    media_type\\n    ...MediaThumbnail\\n    __typename\\n  }\\n  ...PostStatusIcons\\n  __typename\\n}\\n\\nfragment MediaThumbnail on Media {\\n  id\\n  image_uuid\\n  __typename\\n}\\n\\nfragment PostStatusIcons on Post {\\n  name\\n  product_state\\n  __typename\\n}\\n\\nfragment PostVoteButton on Post {\\n  _id\\n  id\\n  ... on Votable {\\n    id\\n    votes_count\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TopicFollowButtonList on Topicable {\\n  id\\n  topics {\\n    edges {\\n      node {\\n        id\\n        ...TopicFollowButton\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TopicFollowButton on Topic {\\n  id\\n  name\\n  slug\\n  __typename\\n}\\n\\nfragment Header on Topic {\\n  id\\n  name\\n  description\\n  image_uuid\\n  ...FollowersCount\\n  __typename\\n}\\n\\nfragment FollowersCount on Followable {\\n  followers_count\\n  followers(first: 3) {\\n    edges {\\n      node {\\n        id\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment MetaTags on MetaTaggable {\\n  id\\n  meta {\\n    audio_url\\n    canonical_url\\n    creator\\n    description\\n    image\\n    mobile_app_url\\n    oembed_url\\n    robots\\n    title\\n    type\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment FeedCards on Card {\\n  ...LiveChatCard\\n  ...TopicsCard\\n  ...TopicCard\\n  ...CollectionFeedCard\\n  ...AskProductHuntCard\\n  ...NewPostsCard\\n  ...JobsCard\\n  ...FeedUpcomingPagesCard\\n  __typename\\n}\\n\\nfragment AskProductHuntCard on AskProductHuntCard {\\n  is_dismissed\\n  product_request {\\n    _id\\n    id\\n    path\\n    title\\n    recommended_products(first: 3, order: VOTES) {\\n      edges {\\n        node {\\n          _id\\n          id\\n          product {\\n            id\\n            posts(first: 1, filter: FEATURED) {\\n              edges {\\n                node {\\n                  _id\\n                  id\\n                  name\\n                  path\\n                  slug\\n                  tagline\\n                  __typename\\n                }\\n                __typename\\n              }\\n              __typename\\n            }\\n            thumbnail_media {\\n              id\\n              ...MediaThumbnail\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    user {\\n      _id\\n      id\\n      name\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CollectionFeedCard on CollectionCard {\\n  is_dismissed\\n  collection {\\n    id\\n    name\\n    title\\n    background_image_banner_url\\n    path\\n    user {\\n      id\\n      name\\n      __typename\\n    }\\n    items(first: 5) {\\n      edges {\\n        node {\\n          post {\\n            id\\n            slug\\n            name\\n            tagline\\n            thumbnail {\\n              id\\n              image_uuid\\n              media_type\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment LiveChatCard on LiveChatCard {\\n  is_dismissed\\n  upcoming_live_chats {\\n    id\\n    name\\n    tagline\\n    slug\\n    ...LiveChatImage\\n    ...AmaEventSubscribeButton\\n    __typename\\n  }\\n  upcoming_featured {\\n    id\\n    name\\n    tagline\\n    starts_at\\n    ends_at\\n    subscriber_count\\n    slug\\n    ...LiveChatImage\\n    ...AmaEventSubscribeButton\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment LiveChatImage on LiveChat {\\n  schedule_image_uuid\\n  guests {\\n    id\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment AmaEventSubscribeButton on LiveChat {\\n  starts_at\\n  ends_at\\n  twitter_username\\n  name\\n  id\\n  __typename\\n}\\n\\nfragment NewPostsCard on NewPostsCard {\\n  is_dismissed\\n  kind\\n  posts {\\n    ...PostItemList\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TopicsCard on TopicsCard {\\n  is_dismissed\\n  topics {\\n    id\\n    slug\\n    name\\n    ...TopicImage\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment TopicImage on Topic {\\n  image_uuid\\n  __typename\\n}\\n\\nfragment TopicCard on TopicCard {\\n  is_dismissed\\n  topic {\\n    id\\n    slug\\n    name\\n    description\\n    ...TopicImage\\n    posts(first: 5) {\\n      edges {\\n        node {\\n          id\\n          slug\\n          name\\n          tagline\\n          thumbnail {\\n            id\\n            image_uuid\\n            media_type\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment JobsCard on JobsCard {\\n  is_dismissed\\n  jobs {\\n    id\\n    company_name\\n    job_title\\n    image_uuid\\n    url\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment FeedUpcomingPagesCard on UpcomingPagesCard {\\n  is_dismissed\\n  upcoming_pages {\\n    ...UpcomingPageItem\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment UpcomingPageItem on UpcomingPage {\\n  id\\n  name\\n  tagline\\n  slug\\n  background_image_uuid\\n  logo_uuid\\n  public_subscriber_count\\n  user {\\n    ...UserSpotlight\\n    __typename\\n  }\\n  popular_subscribers {\\n    id\\n    twitter_username\\n    ...UserSpotlight\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment UserSpotlight on User {\\n  id\\n  headline\\n  name\\n  username\\n  ...UserImageLink\\n  __typename\\n}\\n\\nfragment UserImageLink on User {\\n  username\\n  ...UserImage\\n  __typename\\n}\\n\\nfragment UserImage on User {\\n  id\\n  post_upvote_streak\\n  __typename\\n}\\n\",\"variables\":{\"slug\":\"" + slug + "\",\"subtopic\":null,\"cursor\":\"" + offset + "\",\"page\":\"topic\",\"skipCards\":true},\"operationName\":\"TopicPage\"}";
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

}

