package URL;

import java.util.UUID;

public class UrlShortenerTest {
    public static void main(String[] args) {
        UrlShortenerService service = new UrlShortenerService();
        
        System.out.println("Тест 1: Уникальные ссылки для разных пользователей");
        UUID user1 = service.generateNewUserId();
        UUID user2 = service.generateNewUserId();
        
        String url = "https://vkvideo.ru";
        String link1 = service.createShortLink(url, user1);
        String link2 = service.createShortLink(url, user2);
        
        System.out.println("Пользователь 1: " + link1);
        System.out.println("Пользователь 2: " + link2);
        System.out.println("Ссылки разные: " + !link1.equals(link2));
        System.out.println();
        
        System.out.println("Тест 2: Лимит переходов");
        UUID user3 = service.generateNewUserId();
        String testLink = service.createShortLink("https://www.google.com", user3, 2);
        System.out.println("Тестовая ссылка: " + testLink);
        
        service.accessShortLink(testLink);
        service.accessShortLink(testLink);
        service.accessShortLink(testLink);
        System.out.println();
        
        System.out.println("Тест 3: Статистика пользователя");
        service.getUserStats(user1);
        
        System.out.println("Тест 4: Срок жизни задается программой");
        UUID user4 = service.generateNewUserId();
        String timedLink = service.createShortLink("https://www.github.com", user4, 5);
        System.out.println("Ссылка создана с автоматическим сроком жизни 24 часа");
        service.getUserStats(user4);
    }
}