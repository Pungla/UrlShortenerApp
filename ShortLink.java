package URL;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Random;

class ShortLink {
    private String id;
    private String originalUrl;
    private String shortUrl;
    private UUID userId;
    private int clickLimit;
    private int currentClicks;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
    
    public ShortLink(String originalUrl, UUID userId, int clickLimit, int validityHours) {
        this.id = generateId();
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.clickLimit = clickLimit;
        this.currentClicks = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusHours(validityHours);
        this.isActive = true;
        this.shortUrl = "clck.ru/" + this.id;
    }
    
    private String generateId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    public String getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getShortUrl() { return shortUrl; }
    public UUID getUserId() { return userId; }
    public int getClickLimit() { return clickLimit; }
    public int getCurrentClicks() { return currentClicks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isActive() { return isActive; }
    
    public boolean canBeAccessed() {
        return isActive && 
               currentClicks < clickLimit && 
               LocalDateTime.now().isBefore(expiresAt);
    }
    
    public void incrementClicks() {
        if (canBeAccessed()) {
            currentClicks++;
        }
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    @Override
    public String toString() {
        return String.format(
            "Short URL: %s -> %s\nClicks: %d/%d, Expires: %s, Active: %s",
            shortUrl, originalUrl, currentClicks, clickLimit, 
            expiresAt.toString(), isActive
        );
    }
}