package org.scott;

import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper implements Runnable {
    static Pattern addressPattern = Pattern.compile("https://locations.chipotle.com/([a-z]{2})/([a-z]+)/([a-z0-9-]+)");
    private final int id;
    private final Queue<String> readQueue;
    private final Queue<AddressMenu> writeQueue;


    public Scraper(int id, Queue<String> readQueue, Queue<AddressMenu> writeQueue) {
        this.id = id;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
    }

    public void run() {
        //create new driver
        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(5000));

        String link;
        //get from top of queue
        while (true) {
            synchronized (readQueue) {
                if (!readQueue.isEmpty()) {
                    link = readQueue.poll();
                } else {
                    return;
                }
            }

            driver.get("https://locations.chipotle.com/"+link);
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
        WebElement ele = driver.findElement(By.className("Core-cta--online"));
        ele.click();


        WebElement burritoOption = driver.findElement(By.cssSelector("div[data-button-value='Burrito']"));
        try  {
            burritoOption.click();
        } catch(Exception e) {
            Thread.sleep(6000);
            burritoOption.click();
        }


        //Regex pattern = "You can select ([A-z\s]+) (\$[1-9]\.[0-9]{2}) [0-9]{3}cal"
        //loop over item types in list
        List<WebElement> menuItems = driver.findElements(By.className("meal-builder-item-selector-card-container"));

        for (WebElement menuItem: menuItems) {
            try {
                int price = Integer.parseInt(menuItem.findElement(By.className("item-cost")).getText().replaceAll("[$.]",""));
                String name = menuItem.findElement(By.className("item-name")).getText();
                chipotleMenu.put(name, price);
            } catch (Exception e){
                //should probably check for element not found exception
            }

        }
        //sending info to JsonWriter
        writeQueue.add(new AddressMenu(adcomponents.group(3), adcomponents.group(1), adcomponents.group(2), chipotleMenu));
        Thread.sleep(2000);
    }

    public void stop() throws IOException {
        Thread.currentThread().interrupt();
    }

}
