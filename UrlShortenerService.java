package URL;

import java.io.*;
import java.net.URI;
import java.awt.Desktop;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UrlShortenerService {
    private Map<String, ShortLink> shortLinks;
    private Map<UUID, List<String>> userLinks;
    private Map<String, Set<UUID>> urlUsers;
    
    private static final int DEFAULT_CLICK_LIMIT = 10;
    private static final int DEFAULT_VALIDITY_HOURS = 24;
    
    public UrlShortenerService() {
        this.shortLinks = new ConcurrentHashMap<>();
        this.userLinks = new ConcurrentHashMap<>();
        this.urlUsers = new ConcurrentHashMap<>();
        startCleanupTask();
    }
    
    public String createShortLink(String originalUrl, UUID userId) {
        return createShortLink(originalUrl, userId, DEFAULT_CLICK_LIMIT);
    }
    
    public String createShortLink(String originalUrl, UUID userId, int clickLimit) {
        int validityHours = DEFAULT_VALIDITY_HOURS;
        
        String existingLink = findExistingUserLink(originalUrl, userId);
        if (existingLink != null) {
            return existingLink;
        }
        
        ShortLink shortLink = new ShortLink(originalUrl, userId, clickLimit, validityHours);
        
        shortLinks.put(shortLink.getId(), shortLink);
        
        userLinks.computeIfAbsent(userId, k -> new ArrayList<>()).add(shortLink.getId());
        urlUsers.computeIfAbsent(originalUrl, k -> new HashSet<>()).add(userId);
        
        System.out.println("Создана короткая ссылка: " + shortLink.getShortUrl());
        System.out.println("Срок жизни: " + validityHours + " часов");
        System.out.println("Лимит переходов: " + clickLimit);
        return shortLink.getShortUrl();
    }
    
    private String findExistingUserLink(String originalUrl, UUID userId) {
        Set<UUID> users = urlUsers.get(originalUrl);
        if (users != null && users.contains(userId)) {
            List<String> userShortIds = userLinks.get(userId);
            if (userShortIds != null) {
                for (String shortId : userShortIds) {
                    ShortLink link = shortLinks.get(shortId);
                    if (link != null && link.getOriginalUrl().equals(originalUrl)) {
                        return link.getShortUrl();
                    }
                }
            }
        }
        return null;
    }
    
    public void accessShortLink(String shortUrl) {
        String shortId = extractShortId(shortUrl);
        ShortLink shortLink = shortLinks.get(shortId);
        
        if (shortLink == null) {
            System.out.println("Ссылка не найдена: " + shortUrl);
            return;
        }
        
        if (!shortLink.canBeAccessed()) {
            handleInaccessibleLink(shortLink);
            return;
        }
        
        shortLink.incrementClicks();
        
        System.out.println("Переход по ссылке: " + shortLink.getOriginalUrl());
        System.out.println("Статистика: " + shortLink.getCurrentClicks() + "/" + 
                          shortLink.getClickLimit() + " переходов");
        
        openInBrowser(shortLink.getOriginalUrl());
    }
    
    private void handleInaccessibleLink(ShortLink shortLink) {
        if (shortLink.isExpired()) {
            System.out.println("Ссылка истекла: " + shortLink.getShortUrl());
            long hoursSinceCreation = ChronoUnit.HOURS.between(shortLink.getCreatedAt(), LocalDateTime.now());
            notifyUser(shortLink.getUserId(), 
                      "Ссылка " + shortLink.getShortUrl() + " истекла. Создана " + 
                      hoursSinceCreation + " часов назад");
        } else if (shortLink.getCurrentClicks() >= shortLink.getClickLimit()) {
            System.out.println("Лимит переходов исчерпан: " + shortLink.getShortUrl());
            notifyUser(shortLink.getUserId(), 
                      "Лимит переходов по ссылке " + shortLink.getShortUrl() + " исчерпан");
        } else if (!shortLink.isActive()) {
            System.out.println("Ссылка деактивирована: " + shortLink.getShortUrl());
        }
        
        shortLink.deactivate();
    }
    
    private String extractShortId(String shortUrl) {
        if (shortUrl.startsWith("clck.ru/")) {
            return shortUrl.substring(8);
        }
        return shortUrl;
    }
    
    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Автоматическое открытие в браузере не поддерживается");
                System.out.println("Скопируйте ссылку: " + url);
            }
        } catch (Exception e) {
            System.out.println("Ошибка при открытии браузера: " + e.getMessage());
            System.out.println("Ссылка для ручного копирования: " + url);
        }
    }
    
    private void notifyUser(UUID userId, String message) {
        System.out.println("Уведомление для пользователя " + userId + ": " + message);
    }
    
    public void getUserStats(UUID userId) {
        List<String> userShortIds = userLinks.get(userId);
        if (userShortIds == null || userShortIds.isEmpty()) {
            System.out.println("У пользователя " + userId + " нет созданных ссылок");
            return;
        }
        
        System.out.println("Статистика пользователя " + userId + ":");
        for (String shortId : userShortIds) {
            ShortLink link = shortLinks.get(shortId);
            if (link != null) {
                long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), link.getExpiresAt());
                System.out.println("  " + link);
                System.out.println("  Осталось часов: " + (hoursLeft > 0 ? hoursLeft : 0));
                System.out.println();
            }
        }
    }
    
    public boolean deleteLink(UUID userId, String shortUrl) {
        String shortId = extractShortId(shortUrl);
        ShortLink link = shortLinks.get(shortId);
        
        if (link == null || !link.getUserId().equals(userId)) {
            System.out.println("Ссылка не найдена или доступ запрещен");
            return false;
        }
        
        shortLinks.remove(shortId);
        userLinks.get(userId).remove(shortId);
        urlUsers.get(link.getOriginalUrl()).remove(userId);
        
        System.out.println("Ссылка удалена: " + shortUrl);
        return true;
    }
    
    private void startCleanupTask() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredLinks();
            }
        }, 0, 60 * 60 * 1000);
    }
    
    private void cleanupExpiredLinks() {
        int removedCount = 0;
        Iterator<Map.Entry<String, ShortLink>> iterator = shortLinks.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, ShortLink> entry = iterator.next();
            ShortLink link = entry.getValue();
            
            if (link.isExpired()) {
                notifyUser(link.getUserId(), 
                          "Ссылка " + link.getShortUrl() + " была автоматически удалена по истечении срока жизни (24 часа)");
                
                iterator.remove();
                userLinks.get(link.getUserId()).remove(link.getId());
                urlUsers.get(link.getOriginalUrl()).remove(link.getUserId());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            System.out.println("Удалено устаревших ссылок: " + removedCount);
        }
    }
    
    public UUID generateNewUserId() {
        return UUID.randomUUID();
    }
}