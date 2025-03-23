package org.scott;

import com.google.gson.stream.JsonWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class JsonWriterService implements Runnable{
    //private BlockingQueue<AddressMenu> writeQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<AddressMenu> queue;
    private final JsonWriter writer;

    public JsonWriterService(String filePath, BlockingQueue<AddressMenu> q) throws IOException {
        this.writer = new JsonWriter(new FileWriter(filePath));
        this.queue = q;
        writer.setIndent("  ");
        writer.beginArray();
    }

    @Override
    public void run() {
        try {
            while (true) {
                //the object is MenuItem
                writeItem(queue.take());
            }
        } catch (InterruptedException | IOException e) {
            System.err.println(e);
        }
    }

    public void stop() throws IOException {
        writer.endArray();
        writer.close();
        Thread.currentThread().interrupt();
    }

    void writeItem(AddressMenu x) throws IOException {
        writer.beginObject();
        writer.name("address").value(x.getAddress());
        writer.name("city").value(x.getCity());
        writer.name("state").value(x.getState());
        writer.beginObject();
        for (Map.Entry<String, Integer> item: x.getMenu().entrySet()) {
            writer.name(item.getKey()).value(item.getValue());
        }
        writer.endObject();
        writer.endObject();
    }
}
