package org.example;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.FileWriter;

import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        // Подключение к MongoDB
        MongoClient mongoClient = null;
        MongoCredential credential = MongoCredential.createCredential("admin1", "admin", "Cool12211221!$$".toCharArray());
        mongoClient = new MongoClient(new ServerAddress("89.248.206.92", 27017), Arrays.asList(credential));

        // Выбор базы данных и коллекции
        MongoDatabase database = mongoClient.getDatabase("avito");
        MongoCollection<Document> collection = database.getCollection("price_history");

        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);

            while (true) {
                for (Document doc : collection.find()) {
                    String url = doc.getString("link"); // Предположим, что URL хранится в поле "url"

                    if (url != null && !url.isEmpty()) {
                        try {
                            // Загрузите страницу по URL
                            HtmlPage page = webClient.getPage(url);

                            // Ищите элемент на странице
                            HtmlElement element = page.getFirstByXPath("//span[@class='closed-warning-content-_f4_B']");

                            if (element != null) {
                                // Если элемент найден, удалите документ из коллекции price_history
                                collection.deleteOne(doc);
                                System.out.println("Документ удален с URL: " + url);
                                // Открываем файл для записи URL удаленных документов
                                try (FileWriter fileWriter = new FileWriter("deleted_document_urls.txt", true)) {
                                    fileWriter.write(url + "\n");
                                }
                            } else {
                                System.out.println("Элемент не найден на странице с URL: " + url);
                            }
                        } catch (Exception e) {
                            System.out.println("Ошибка при загрузке страницы с URL: " + url);
                        }

                        // Приостанавливаем выполнение цикла на 20 секунд (20000 миллисекунд)
                        Thread.sleep(5000);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Закрытие соединения с MongoDB
        mongoClient.close();
    }
}
