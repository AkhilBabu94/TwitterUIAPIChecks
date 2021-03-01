package MyRunner;

import Driver.TestBase;
import Utils.TestUtils;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.Markup;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import cucumber.api.CucumberOptions;
import cucumber.api.testng.TestNGCucumberRunner;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static io.restassured.RestAssured.given;

@CucumberOptions(
        features = "src/test/resources/Features",
        glue = {"stepDefinitions"},
        monochrome = true,
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber-pretty",
                "json:target/cucumber-reports/CucumberTestReport.json",
                "rerun:target/cucumber-reports/rerun.txt"
        }
)

public class TestRunner extends TestBase {
    private TestNGCucumberRunner testNGCucumberRunner;
    private Object WebElement;
    ExtentTest test;
    ExtentReports extent;

    @BeforeSuite(alwaysRun = true)
    public void setUpClass() throws Exception {
        testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
        TestBase.initialization();
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(System.getProperty("user.dir") +"/test-output/extent.html");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        test = extent.createTest("Twitter Automation", "Twitter API and UI automation");
    }

    static String extractInt(String str)
    {
        str = str.replaceAll("[^\\d]", " ");
        str = str.trim();
        str = str.replaceAll(" +", " ");
        if (str.equals(""))
            return "-1";
        return str;
    }

    @Test(dataProvider = "dataset")
    public void seleniumTwitterAutomation(String name,String url) throws IOException, InterruptedException, URISyntaxException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        driver.get(url);
        String screenshotMainPage = "mainPage_"+name.replaceAll(" ","");
        TestUtils.takeScreenshot(screenshotMainPage);

//      Function call for Oauth Authentication
        String token = oAuthTokenGeneration();

//      Function call for getting tweet related data of the given user
        HashMap<Integer, HashMap<String, Object>> tweetData = tweetDataGenaration(token,name.replaceAll(" ",""));
        int k=0;
        int foundTotal = 0;
        boolean found = false;
        List<Integer> nums = new ArrayList();
        int j=0;

        List<String> tweetText = new ArrayList<>();
        List<String> tweetIds = new ArrayList<>();
        while(j<10){
            nums.add(j);
            tweetText.add(tweetData.get(j).get("text").toString().substring(0,50));
            tweetIds.add(tweetData.get(j).get("id").toString());
            j++;
        }
        while(k<100){
            int f =0;
            for (Integer integer : new ArrayList<>(nums)) {
                try{
                    String text = tweetData.get(integer).get("text").toString().substring(0,50);
                    if(text.contains("@")){
                        text = text.split("@")[0];
                    }
                    System.out.println("Text is " + text);
                    By element = By.xpath("//span[contains(.,'" + text + "')]");
                    waitForPresenceOfElement(element, 1);
                    System.out.println("element is founded successfully");
                    TestUtils.takeScreenshot("TweetPic_"+integer+"_"+name);

                    //Data from UI
                    WebElement data =driver.findElement(By.xpath("//span[contains(.,'" + text + "')]/ancestor::div[3]/div[3]"));
                    String tweetdata = data.getAttribute("aria-label");
                    String retweetCountPresentInUI = extractInt(tweetdata).split(" ")[1];
                    String likeCountPresentInUI = extractInt(tweetdata).split(" ")[2];
                    System.out.println("The retweet count from UI is "+retweetCountPresentInUI+" and likes count from UI is "+likeCountPresentInUI);

                    //Data from API
                    String likeCountFromAPI = tweetData.get(integer).get("favorite_count").toString();
                    String retweetCountFromAPI = tweetData.get(integer).get("retweet_count").toString();
                    System.out.println("The retweet count from API is "+retweetCountFromAPI+" and likes count from API is "+likeCountFromAPI);
                    found=true;

                    //difference between retweet counts if present
                    if(retweetCountPresentInUI.equals(retweetCountFromAPI)){
                        System.out.println("There is no difference between retweet count for the tweet");
                    }else{
                        int retweetPresentInUIInt = Integer.parseInt(retweetCountPresentInUI);
                        int retweetPresentInAPIInt = Integer.parseInt(retweetCountFromAPI);
                        int diff = retweetPresentInUIInt-retweetPresentInAPIInt;
                        System.out.println("The difference between retweet count for the tweet "+text+" is "+diff);
                    }

                    //difference between likes/fav counts if present
                    if(likeCountPresentInUI.equals(likeCountFromAPI)){
                        System.out.println("There is no difference between likes count for the tweet");
                    }else{
                        int favouritePresentInUIInt = Integer.parseInt(likeCountPresentInUI);
                        int favouritePresentInAPIInt = Integer.parseInt(likeCountFromAPI);
                        int diff = favouritePresentInUIInt-favouritePresentInAPIInt;
                        System.out.println("The difference between likes count for the tweet "+text+" is "+diff);
                    }
                    foundTotal=foundTotal+1;
                    nums.remove(integer);
                }catch(TimeoutException|StaleElementReferenceException e){
                    System.out.println("element not found");
                }
                f++;
                if(found||f==nums.size()){
                    js.executeScript("window.scrollBy(0,500)");
                }
            }
            if(foundTotal==10){
                break;
            }
            System.out.println("The total founded text is "+foundTotal);
            System.out.println("The left items are "+nums);
            k++;
        }

        //Friend list and verified account
        driver.get(url);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10,TimeUnit.SECONDS);

        //Login to twitter
        driver.findElement(By.xpath("//span[text()='Following']")).click();
        driver.findElement(By.xpath("(//div[@dir='auto'][contains(.,'Log in')])[2]")).click();
        driver.findElement(By.xpath("//input[contains(@type,'text')]")).sendKeys("username");
        driver.findElement(By.xpath("//input[@type='password']")).sendKeys("password");
        driver.findElement(By.xpath("(//div[@dir='auto'][contains(.,'Log in')])[1]")).click();
        driver.findElement(By.xpath("//span[text()='Following']")).click();

        //Get Following first name link and clink on it
        WebElement FollowingFirstName =driver.findElement(By.xpath("/html/body/div[1]/div/div/div[2]/main/div/div/div/div[1]/div/div[2]/section/div/div/div[1]/div/div/div/div[2]/div[1]/div[1]/a/div/div[1]/div[1]/span/span"));

        //Get top 10 friends
        List<String> top10Friends = new ArrayList<>();
        int i=1;
        while(i<=10){
            WebElement FollowingName =driver.findElement(By.xpath("/html/body/div[1]/div/div/div[2]/main/div/div/div/div[1]/div/div[2]/section/div/div/div["+i+"]/div/div/div/div[2]/div[1]/div[1]/a/div/div[1]/div[1]/span/span"));
            top10Friends.add(FollowingName.getText());
            i++;
        }

        FollowingFirstName.click();

        //Get number of verified accounts in the list of the user
        driver.findElement(By.xpath("//span[text()='Following']")).click();
        Thread.sleep(5000);
        int numberOfVerifiedFollowers = 0;
        i=1;
        WebElement followerList = driver.findElement(By.xpath("/html/body/div[1]/div/div/div[2]/main/div/div/div/div"));
        List<WebElement> svgs = followerList.findElements(By.xpath("//*[name()='svg']"));
        System.out.println("Total svgs are "+svgs.size());
        int scrollDown =0;
        List<String> verifiedAccountNames = new ArrayList<>();
        while(i<=svgs.size()){
            try{
                WebElement svg =driver.findElement(By.xpath("/html/body/div[1]/div/div/div[2]/main/div/div/div/div/div/div[2]/section/div/div/div["+i+"]/div/div/div/div[2]/div[1]/div[1]/a/div/div[1]/div[2]/*[name()='svg']"));
                WebElement FollowingName =driver.findElement(By.xpath("/html/body/div[1]/div/div/div[2]/main/div/div/div/div/div/div[2]/section/div/div/div["+i+"]/div/div/div/div[2]/div[1]/div[1]/a/div/div[1]/div[2]/*[name()='svg']/ancestor::div[2]/div[1]/span/span"));
                js.executeScript("arguments[0].setAttribute('style', 'background: yellow; border: 2px solid red;');", FollowingName);
                if(svg.getAttribute("aria-label")!=null){
                    System.out.println("The account name for number "+i+" is "+FollowingName.getText());
                    verifiedAccountNames.add(FollowingName.getText());
                    numberOfVerifiedFollowers=numberOfVerifiedFollowers+1;
                }}catch (NoSuchElementException e){}
            scrollDown=scrollDown+1;
            if(scrollDown==4){
                scrollDown=0;
                System.out.println("Scrolling down");
                js.executeScript("window.scrollBy(0,300)");
            }
            i++;
        }
        System.out.println("The number of verified followers are "+numberOfVerifiedFollowers);


//        Extent report customization
        String currentDir = System.getProperty("user.dir");
        String pathMainPageScreenShot = currentDir.replaceAll("\\\\", "/") + "/screenshots/" + screenshotMainPage + ".png";
        String top10tweetScreenShots = currentDir.replaceAll("\\\\", "/") + "/screenshots/";

        URI uri1 = new URI("file:///"+pathMainPageScreenShot);
        URL pathMainPageScreenShoturl = uri1.toURL();
        URI uri2 = new URI("file:///"+top10tweetScreenShots);
        URL top10tweetScreenShotsurl = uri2.toURL();

        System.out.println("path is "+pathMainPageScreenShoturl);

        String[][] data = {
                { "Twitter Handle", "Twitter handle URL", "Snapshot link to the snapshot of landing page","Selected top 10 tweet Id & text","Selected top 10 tweets images/link of the image","Top 10 friends","Selected friends and verified account" },
                { name, url, "Snapshot of main landing page link is "+pathMainPageScreenShoturl, "Top 10 tweet text are "+tweetText+" and Top 10 tweet ids are "+tweetIds,"Top 10 tweet screenshots are present in path "+top10tweetScreenShotsurl, String.valueOf(top10Friends),String.valueOf(verifiedAccountNames)},
        };
        Markup m = MarkupHelper.createTable(data);
        test.pass(m);
        }

    public void waitForPresenceOfElement(By loc, int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.until(ExpectedConditions.elementToBeClickable(loc));
    }

    public String oAuthTokenGeneration(){
        String CONSUMER_KEY = "YrfBeTHrMUCuSD4kdAMBF69WX";
        String CONSUMER_SECRET = "10Xbelb6ftIsnqNVvukPI1BwbL9nn2QnoX0QheetSYo0t32hJ8";

        Base64.Encoder encoder = Base64.getEncoder();
        String originalString = CONSUMER_KEY+":"+CONSUMER_SECRET;
        String encodedString = encoder.encodeToString(originalString.getBytes());
        System.out.println("Encoded string is "+encodedString);

        String baseUrl = "https://api.twitter.com";
        RestAssured.baseURI=baseUrl;
        RequestSpecification request = given();

        Response response = request.header("Authorization","Basic "+encodedString)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .body("grant_type=client_credentials")
                .when()
                .post("/oauth2/token");
        String jsonString = response.asString();
        String token = JsonPath.from(jsonString).get("access_token");
        System.out.println("Token is "+token);
        return token;
    }

    public HashMap<Integer, HashMap<String, Object>> tweetDataGenaration(String token,String screenName){
        Response response = given().auth().oauth2(token)
                .queryParam("count","100")
                .queryParam("include_rts","false")
                .queryParam("screen_name",screenName)
                .when().get("https://api.twitter.com/1.1/statuses/user_timeline.json");
        String jsonString = response.asString();
        List<Integer> retweet_count = JsonPath.from(jsonString).get("retweet_count");
        int i=0;
        HashMap<Integer,Integer> Retweetmap = new HashMap<>();
        Integer[] Values = new Integer[retweet_count.size()];
        while (i<retweet_count.size()){
            Retweetmap.put(retweet_count.get(i),i);
            Values[i]= retweet_count.get(i);
            i++;
        }
        int temp;
        for (i = 0; i < Values.length; i++)
        {
            for (int j = i + 1; j < Values.length; j++) {
                if (Values[i] < Values[j])
                {
                    temp = Values[i];
                    Values[i] = Values[j];
                    Values[j] = temp;
                }
            }
        }

        Integer[] requiredArrayIndex = new Integer[10];
        int k =0;
        while(k<10){
            if(Retweetmap.containsKey(Values[k])){
                requiredArrayIndex[k]=Retweetmap.get(Values[k]);
            }
            k++;
        }

        List requiredArrayIndexList = Arrays.asList(requiredArrayIndex);
        i=0;
        String id = null;
        String created_at= null;
        String text= null;
        String username= null;
        String favorite_count= null;
        String retweet_counts= null;
        HashMap<Integer, HashMap<String, Object>> tweetData = new HashMap<Integer, HashMap<String,Object>>();
        int l=0;
        while (i<retweet_count.size()){
            if(requiredArrayIndexList.contains(i)){
                id = JsonPath.from(jsonString).getString("id["+i+"]");
                created_at = JsonPath.from(jsonString).getString("created_at["+i+"]");
                text = JsonPath.from(jsonString).getString("text["+i+"]");
                username = JsonPath.from(jsonString).getString("user.name["+i+"]");
                favorite_count = JsonPath.from(jsonString).getString("favorite_count["+i+"]");
                retweet_counts = JsonPath.from(jsonString).getString("retweet_count["+i+"]");
                tweetData.put(l, new HashMap<String, Object>());
                tweetData.get(l).put("id", id);
                tweetData.get(l).put("created_at", created_at);
                tweetData.get(l).put("text", text);
                tweetData.get(l).put("username", username);
                tweetData.get(l).put("favorite_count", favorite_count);
                tweetData.get(l).put("retweet_count", retweet_counts);
                l++;
            }
            i++;
        }
        System.out.println("Id is "+tweetData.get(9).get("id"));
        return tweetData;
    }


//    Get the top 10 friends having the most number of followers.
    public HashMap<Integer, HashMap<String, Object>> friendsDataGeneration(String token){
        Response response = given().auth().oauth2(token)
                .queryParam("count","100")
                .queryParam("cursor",-1)
                .queryParam("screen_name","BarackObama")
                .when().get("https://api.twitter.com/1.1/friends/list.json");
        String jsonString = response.asString();
        List<Integer> followers_count = JsonPath.from(jsonString).get("users.followers_count");
        int i=0;
        HashMap<Integer,Integer> followersCountMap = new HashMap<>();
        Integer[]Values = new Integer[followers_count.size()];
        while (i<followers_count.size()){
            followersCountMap.put(followers_count.get(i),i);
            Values[i]= followers_count.get(i);
            i++;
        }
        int temp;
        for (i = 0; i < Values.length; i++)
        {
            for (int j = i + 1; j < Values.length; j++) {
                if (Values[i] < Values[j])
                {
                    temp = Values[i];
                    Values[i] = Values[j];
                    Values[j] = temp;
                }
            }
        }

        Integer[] requiredArrayIndexForMoreFollowerCount = new Integer[10];
        int k =0;
        while(k<10){
            if(followersCountMap.containsKey(Values[k])){
                requiredArrayIndexForMoreFollowerCount[k]=followersCountMap.get(Values[k]);
            }
            k++;
        }

        List requiredArrayIndexListForMoreFollowerCount = Arrays.asList(requiredArrayIndexForMoreFollowerCount);
        i=0;
        String screen_name = null;
        String friends_count= null;
        String followers_counts= null;
        HashMap<Integer, HashMap<String, Object>> tweetDataForMoreFollowerCount = new HashMap<Integer, HashMap<String,Object>>();
        int l=0;
        while (i<followers_count.size()){
            if(requiredArrayIndexListForMoreFollowerCount.contains(i)){
                screen_name = JsonPath.from(jsonString).getString("users.screen_name["+i+"]");
                friends_count = JsonPath.from(jsonString).getString("users.friends_count["+i+"]");
                followers_counts = JsonPath.from(jsonString).getString("users.followers_count["+i+"]");
                tweetDataForMoreFollowerCount.put(l, new HashMap<String, Object>());
                tweetDataForMoreFollowerCount.get(l).put("screen_name", screen_name);
                tweetDataForMoreFollowerCount.get(l).put("friends_count", friends_count);
                tweetDataForMoreFollowerCount.get(l).put("followers_counts", followers_counts);
                l++;
            }
            i++;
        }
        int f=0;
        while(f<10){
            System.out.println("Screen name is "+tweetDataForMoreFollowerCount.get(f).get("screen_name"));
            System.out.println("Number of followers are "+tweetDataForMoreFollowerCount.get(f).get("followers_counts"));
            f++;
        }
        return tweetDataForMoreFollowerCount;
    }

    @DataProvider
    public Object[][] dataset() {
        Object[][] testObjArray = TestUtils.getTestData(System.getProperty("user.dir") +"/TestData.xlsx","Sheet1");
        return (testObjArray);
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownClass() throws Exception {
        extent.flush();
        testNGCucumberRunner.finish();
        TestBase.quitDriver();
    }
}