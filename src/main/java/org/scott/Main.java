package org.scott;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.println("Hello World!");

        Queue<String> addressLinks = new ConcurrentLinkedQueue<>();
        int NUM_SCRAPERS = 6;
        List<Scraper> scrapers = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_SCRAPERS);
        /*Path rn
        Locations Page -> Directory-listLink
            State Locations -> Directory-listLink
                Address Page -> Core-cta--online
                    Burrito -> data-button-value="Burrito"
                        <div data-v-5bebcb26="" data-analytics-section="protein-or-veggie" class="item-selector"><div data-v-5bebcb26="" class="title-container">
                            <div data-v-7dd48d56="" tabindex="0" aria-atomic="true" role="definition" aria-label="You can select Chicken $8.75 180cal" class="card type-single">
         */
        //create file writer service
        BlockingQueue<AddressMenu> writeQueue = new LinkedBlockingQueue<>();
        JsonWriterService writer = new JsonWriterService("output.json", writeQueue);
        new Thread(writer).start();

        /* THIS IS FOR SCRAPING LOCATION LINKS IF THEY'RE DIFFERENT NOW
        I SHOULD PROBABLY JUST TURN THIS INTO AN ARGUMENT BUT IM LAZY

        //create text file for addresses
        BufferedWriter bwriter = new BufferedWriter(new FileWriter("addresslinks.txt"));

        //create web driver
        WebDriver driver = new ChromeDriver();
        driver.get("https://locations.chipotle.com/");
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(1000));

        //THESE LINKS DO NOT CONTAIN "https://locations.chipotle.com/"
        //SINGLE THREAD MAKES A BIG QUEUE FOR EVERY ADDRESS
        //there has to be a pretty way to condense this
        List<String> stateLinks = findLinks(driver, "Directory-listLink");
        for (String stateLink:stateLinks) {
            driver.get("https://locations.chipotle.com/" + stateLink);
            //states page
            List<String> cityLinks = findLinks(driver, "Directory-listLink");

            //for each city found
            for (String cityLink : cityLinks) {
                driver.get("https://locations.chipotle.com/" + cityLink);
                List<String> tlinks = findLinks(driver, "Teaser-titleLink");
                if (tlinks.isEmpty()) {
                    //regular page
                    addressLinks.add(cityLink);
                    System.out.println(cityLink);
                    bwriter.write(cityLink);
                    bwriter.newLine();
                    bwriter.flush();
                } else {
                    //parse another list
                    for (String link:tlinks) {
                        link=link.replace("../","");
                        System.out.println(link);
                        addressLinks.add(link);
                        bwriter.write(link);
                        bwriter.newLine();
                        bwriter.flush();
                    }
                }
            }
        }
         */

        //THIS IS WHAT THE OTHER SIDE OF THE ARGUMENT WOULD BE -> READ FROM TXT
        try (BufferedReader br = new BufferedReader(new FileReader("addresslinks.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                addressLinks.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }


        //Spawning Scrapers now that list is created
        for (int i = 0; i < NUM_SCRAPERS; i++) {
            Thread.sleep(4000);
            Scraper scraper = new Scraper(i,addressLinks, writeQueue);
            scrapers.add(scraper);
            executor.submit(scraper);
        }

        //I actually can't remember why I have this besides a kill switch
        while (!addressLinks.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //kill all
        for (Scraper scraper : scrapers) {
            scraper.stop();
        }

        writer.stop();
        // Shutdown the executor
        executor.shutdown();
    }

    private static List<String> findLinks(WebDriver driver, String className) {
        //parsing everything after seeing each item. Boohoo
//        List<String> stateLinks = new ArrayList<String>();
//        for (WebElement item: stateList) {
//            stateLinks.add(item.getDomAttribute("href"));
//        }
        //intellij does some wizard stuff
        return driver.findElements(By.className(className))
                .stream()
                .map(item -> item.getDomAttribute("href"))
                .collect(Collectors.toList());
    }
}

