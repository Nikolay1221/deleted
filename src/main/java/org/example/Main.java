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

        mongoClient = new MongoClient(new ServerAddress("localhost", 27017));

        // Выбор базы данных и коллекции
        MongoDatabase database = mongoClient.getDatabase("avito");
        MongoCollection<Document> price_history = database.getCollection("price_history");
        MongoCollection<Document> avito_collection = database.getCollection("avito_collection");

        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);

            while (true) {
                for (Document historyDoc : price_history.find()) {
                    String link = historyDoc.getString("link"); // Поле "link" в коллекции price_history

                    if (link != null && !link.isEmpty()) {
                        try {
                            // Загрузите страницу по URL
                            HtmlPage page = webClient.getPage(link);

                            // Ищите элемент на странице
                            HtmlElement element = page.getFirstByXPath("//span[@class='closed-warning-content-_f4_B']");

                            if (element != null) {
                                // Ищите элемент в avito_collection с использованием поля "url"
                                Document avitoDoc = avito_collection.find(new Document("url", link)).first();

                                if (avitoDoc != null) {
                                    // Удалите документ из avito_collection
                                    avito_collection.deleteOne(avitoDoc);
                                    System.out.println("Документ удален из avito_collection с URL: " + link);
                                }

                                // Затем удалите документ из коллекции price_history
                                price_history.deleteOne(historyDoc);
                                System.out.println("Документ удален из price_history с URL: " + link);

                                // Открываем файл для записи URL удаленных документов
                                try (FileWriter fileWriter = new FileWriter("deleted_document_urls.txt", true)) {
                                    fileWriter.write(link + "\n");
                                }
                            } else {
                                System.out.println("Элемент не найден на странице с URL: " + link);
                            }
                        } catch (Exception e) {
                            System.out.println("Ошибка при обработке URL: " + link);
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
