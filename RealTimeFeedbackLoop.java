import com.mongodb.client.*;
import org.bson.Document;
import java.util.*;

public class RealTimeFeedbackLoop {

    // Node class for Feedback
    static class FeedbackNode {
        String feedbackId;
        String devId;
        String feedbackText;
        Date timestamp;
        FeedbackNode prev, next;

        FeedbackNode(String feedbackId, String devId, String feedbackText) {
            this.feedbackId = feedbackId;
            this.devId = devId;
            this.feedbackText = feedbackText;
            this.timestamp = new Date();
        }
    }

    // Doubly Linked List for Feedback
    static class FeedbackDLL {
        FeedbackNode head, tail;
        MongoCollection<Document> feedbackCollection;

        FeedbackDLL(MongoDatabase db) {
            feedbackCollection = db.getCollection("feedback");
            loadFromDatabase();
        }

        void loadFromDatabase() {
            FindIterable<Document> docs = feedbackCollection.find();
            for (Document d : docs) {
                FeedbackNode node = new FeedbackNode(
                        d.getString("feedbackId"),
                        d.getString("devId"),
                        d.getString("feedbackText")
                );
                node.timestamp = d.getDate("timestamp");
                addNode(node, false);
            }
        }

        void addFeedback(String feedbackId, String devId, String feedbackText) {
            FeedbackNode node = new FeedbackNode(feedbackId, devId, feedbackText);
            addNode(node, true);
            System.out.println("Feedback added successfully!");
        }

        private void addNode(FeedbackNode node, boolean saveToDB) {
            if (head == null) head = tail = node;
            else {
                tail.next = node;
                node.prev = tail;
                tail = node;
            }

            if (saveToDB) {
                Document doc = new Document("feedbackId", node.feedbackId)
                        .append("devId", node.devId)
                        .append("feedbackText", node.feedbackText)
                        .append("timestamp", node.timestamp);
                feedbackCollection.insertOne(doc);
            }
        }

        void displayFeedback() {
            if (head == null) {
                System.out.println("No feedback records found.");
                return;
            }
            FeedbackNode current = head;
            while (current != null) {
                System.out.println("[" + current.timestamp + "] Developer ID: " + current.devId +
                        " | Feedback: " + current.feedbackText);
                current = current.next;
            }
        }
    }

    // Node class for Developer
    static class DeveloperNode {
        String devId;
        String name;
        String project;
        DeveloperNode prev, next;

        DeveloperNode(String devId, String name, String project) {
            this.devId = devId;
            this.name = name;
            this.project = project;
        }
    }

    // Doubly Linked List for Developers
    static class DeveloperDLL {
        DeveloperNode head, tail;
        MongoCollection<Document> developerCollection;

        DeveloperDLL(MongoDatabase db) {
            developerCollection = db.getCollection("developers");
            loadDevelopers();
        }

        void loadDevelopers() {
            FindIterable<Document> docs = developerCollection.find();
            for (Document d : docs) {
                DeveloperNode dev = new DeveloperNode(
                        d.getString("devId"),
                        d.getString("name"),
                        d.getString("project")
                );
                addNode(dev, false);
            }
        }

        void addDeveloper(String devId, String name, String project) {
            DeveloperNode dev = new DeveloperNode(devId, name, project);
            addNode(dev, true);
            System.out.println("Developer added successfully!");
        }

        private void addNode(DeveloperNode node, boolean saveToDB) {
            if (head == null) head = tail = node;
            else {
                tail.next = node;
                node.prev = tail;
                tail = node;
            }
            if (saveToDB) {
                Document doc = new Document("devId", node.devId)
                        .append("name", node.name)
                        .append("project", node.project);
                developerCollection.insertOne(doc);
            }
        }

        void displayDevelopers() {
            if (head == null) {
                System.out.println("No developers found.");
                return;
            }
            DeveloperNode current = head;
            while (current != null) {
                System.out.println("Developer ID: " + current.devId +
                        " | Name: " + current.name +
                        " | Project: " + current.project);
                current = current.next;
            }
        }

        DeveloperNode searchDeveloper(String id) {
            DeveloperNode cur = head;
            while (cur != null) {
                if (cur.devId.equals(id)) return cur;
                cur = cur.next;
            }
            return null;
        }
    }

    // MAIN MENU
    public static void main(String[] args) {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("feedbackLoopDB");

        DeveloperDLL developers = new DeveloperDLL(database);
        FeedbackDLL feedbacks = new FeedbackDLL(database);

        Scanner sc = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n=== Real-Time Feedback Loop for Developers ===");
            System.out.println("1. Add Developer");
            System.out.println("2. Display Developers");
            System.out.println("3. Add Feedback");
            System.out.println("4. Display All Feedback");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            while (!sc.hasNextInt()) {
                System.out.print("Invalid input. Enter a number: ");
                sc.next();
            }

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter Developer ID: ");
                    String id = sc.nextLine();
                    System.out.print("Enter Name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter Project: ");
                    String project = sc.nextLine();
                    developers.addDeveloper(id, name, project);
                }
                case 2 -> developers.displayDevelopers();
                case 3 -> {
                    System.out.print("Enter Developer ID: ");
                    String id = sc.nextLine();
                    DeveloperNode dev = developers.searchDeveloper(id);
                    if (dev != null) {
                        System.out.print("Enter Feedback: ");
                        String fb = sc.nextLine();
                        String fbId = UUID.randomUUID().toString();
                        feedbacks.addFeedback(fbId, id, fb);
                    } else {
                        System.out.println("Developer not found!");
                    }
                }
                case 4 -> feedbacks.displayFeedback();
                case 5 -> System.out.println("Exiting Real-Time Feedback Loop...");
                default -> System.out.println("Invalid choice!");
            }

        } while (choice != 5);

        mongoClient.close();
        sc.close();
    }
}
