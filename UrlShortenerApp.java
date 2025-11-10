package URL;

import java.util.Scanner;
import java.util.UUID;

public class UrlShortenerApp {
    private static UrlShortenerService service;
    private static UUID currentUserId;
    private static Scanner scanner;
    
    public static void main(String[] args) {
        initialize();
        showWelcomeMessage();
        runMainLoop();
    }
    
    private static void initialize() {
        service = new UrlShortenerService();
        scanner = new Scanner(System.in);
        currentUserId = service.generateNewUserId();
    }
    
    private static void showWelcomeMessage() {
        System.out.println("Сервис сокращения ссылок");
        System.out.println("ID пользователя: " + currentUserId);
        System.out.println("Все ссылки автоматически удаляются через 24 часа");
    }
    
    private static void runMainLoop() {
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    createShortLink();
                    break;
                case "2":
                    accessShortLink();
                    break;
                case "3":
                    service.getUserStats(currentUserId);
                    break;
                case "4":
                    deleteLink();
                    break;
                case "5":
                    changeUser();
                    break;
                case "0":
                    System.out.println("Выход");
                    return;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
            
            System.out.println();
        }
    }
    
    private static void showMenu() {
        System.out.println("Меню:");
        System.out.println("1. Создать короткую ссылку");
        System.out.println("2. Перейти по короткой ссылке");
        System.out.println("3. Показать мою статистику");
        System.out.println("4. Удалить ссылку");
        System.out.println("5. Сменить пользователя");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }
    
    private static void createShortLink() {
        System.out.print("Введите длинную ссылку: ");
        String url = scanner.nextLine().trim();
        
        if (url.isEmpty() || !url.startsWith("http")) {
            System.out.println("Неверный формат ссылки. Должна начинаться с http/https");
            return;
        }
        
        System.out.print("Введите лимит переходов (по умолчанию 10): ");
        String limitInput = scanner.nextLine().trim();
        int limit = limitInput.isEmpty() ? 10 : Integer.parseInt(limitInput);
        
        String shortUrl = service.createShortLink(url, currentUserId, limit);
        System.out.println("Короткая ссылка создана: " + shortUrl);
    }
    
    private static void accessShortLink() {
        System.out.print("Введите короткую ссылку: ");
        String shortUrl = scanner.nextLine().trim();
        
        if (shortUrl.isEmpty()) {
            System.out.println("Введите корректную ссылку");
            return;
        }
        
        service.accessShortLink(shortUrl);
    }
    
    private static void deleteLink() {
        System.out.print("Введите короткую ссылку для удаления: ");
        String shortUrl = scanner.nextLine().trim();
        
        if (service.deleteLink(currentUserId, shortUrl)) {
            System.out.println("Ссылка успешно удалена");
        }
    }
    
    private static void changeUser() {
        System.out.println("Текущий пользователь: " + currentUserId);
        System.out.println("1. Сгенерировать нового пользователя");
        System.out.println("2. Ввести существующий UUID");
        System.out.print("Выберите действие: ");
        
        String choice = scanner.nextLine().trim();
        if (choice.equals("1")) {
            currentUserId = service.generateNewUserId();
            System.out.println("Новый пользователь создан: " + currentUserId);
        } else if (choice.equals("2")) {
            System.out.print("Введите UUID: ");
            String uuidInput = scanner.nextLine().trim();
            try {
                currentUserId = UUID.fromString(uuidInput);
                System.out.println("Пользователь изменен: " + currentUserId);
            } catch (IllegalArgumentException e) {
                System.out.println("Неверный формат UUID");
            }
        }
    }
}