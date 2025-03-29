package org.scott;

import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper implements Runnable {
    static Pattern onetwothree = Pattern.compile("^.{3}");
    static Pattern addressPattern = Pattern.compile("https://locations.chipotle.com/([a-z]{2})/([a-z0-9-]+)/([a-z0-9-]+)");
    static int staticid;
    private final int id;
    private final Queue<String> readQueue;
    private final Queue<AddressMenu> writeQueue;


    public Scraper(int id, Queue<String> readQueue, Queue<AddressMenu> writeQueue) {
        this.id = id;
        staticid = id;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
    }

    public void run() {
        //create new driver
        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(5000));

        String link;
        //get from top of queues
        //TODO: start on this tmrw
        while (!readQueue.isEmpty()) {
            synchronized (readQueue) {
                link = readQueue.poll();
            }

            //I don't think this will actually throw an error
            try {
                safeGet("https://locations.chipotle.com/"+link, driver);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                writeStorePrices(driver, writeQueue);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void writeStorePrices(WebDriver driver, Queue<AddressMenu> writeQueue) throws InterruptedException, IOException {
        //intellij is saying below might throw an error but put I my faith in chipotle
        Matcher adcomponents = addressPattern.matcher(driver.getCurrentUrl());
        System.out.println(driver.getCurrentUrl());

        Map<String, Integer> chipotleMenu = new HashMap<>();

        if (adcomponents.matches()) {
            //address, city, state
            String[] addressComponents = {adcomponents.group(3), adcomponents.group(2), adcomponents.group(1)};
        } else {
            System.out.println("no matches");
            return;
        }

        //should only execute on the location page
        try {
            WebElement ele = driver.findElement(By.className("Core-cta--online"));
            Thread.sleep(4000);
            safeClick(ele, driver);
        } catch (Exception e) {
            System.out.println(driver.getCurrentUrl() + "has no order online");
            return;
        }

        int loops = 0;
        while(loops < 10) {
            try  {
                WebElement burritoOption = driver.findElement(By.cssSelector("div[data-button-value='Burrito']"));
                Thread.sleep(2000);
                safeClick(burritoOption,driver);
                break;
            } catch(Exception e) {
                System.out.println("Thread "+staticid+" looping : "+ driver.getCurrentUrl());
                Thread.sleep(5000);
                loops += 1;
            }
        }



        //Regex pattern = "You can select ([A-z\s]+) (\$[1-9]\.[0-9]{2}) [0-9]{3}cal"
        //loop over item types in list
        //List<WebElement> menuItems = driver.findElements(By.className("meal-builder-item-selector-card-container"));
        List<WebElement> menuItems = driver.findElements(By.className("item-cost"));
        //System.out.println(menuItems);
        for (WebElement menuItem: menuItems) {
            String name = menuItem.findElement(By.xpath("./../../div[@class='item-name-container']/div[@class='item-name']")).getText();
            try {
                String strippedprice = menuItem.getText().split("\\s+")[0];
                int price = Integer.parseInt(strippedprice.replaceAll("[\s$.-]",""));
                //System.out.println(name + price);
                chipotleMenu.put(name, price);
            } catch (Exception e){
                //should probably check for element not found exception
                System.out.println("Error on item "+name);
                System.err.println(e);
            }

        }
        //sending info to JsonWriter
        System.out.println(adcomponents.group(3)+" "+ adcomponents.group(1)+" "+ adcomponents.group(2)+" "+ chipotleMenu);
        writeQueue.add(new AddressMenu(adcomponents.group(3), adcomponents.group(1), adcomponents.group(2), chipotleMenu));
        Thread.sleep(2000);
    }

    public void stop() throws IOException {
        Thread.currentThread().interrupt();
    }

    private static void safeClick(WebElement ele, WebDriver driver) throws InterruptedException {
        ele.click();
        Thread.sleep(1000);
        while (Objects.equals(driver.getTitle(), "429 Too Many Requests")) {
            Thread.sleep(5000);
            driver.navigate().refresh();
        }
    }

    private static void safeGet(String url, WebDriver driver) throws InterruptedException {
        driver.get(url);
        Thread.sleep(1000);
        while (Objects.equals(driver.getTitle(), "429 Too Many Requests")) {
            Thread.sleep(5000);
            driver.navigate().refresh();
        }
    }

}
