# How to use `LocalizedMessageUtils.java`:

Ensure `MessageSource` is configured: In your Spring Boot application, you typically configure `MessageSource` by having 
message property files (e.g., `messages.properties`, `messages_uz.properties`, `messages_ru.properties`) in src/main/resources.
Spring Boot autoconfigures a `MessageSource bean` if it finds these files. You can customize the basename:
```Properties
spring.messages.basename=i18n/messages

# This would look for files in src/main/resources/i18n/)
```
---
# Inject LocalizedMessageUtils:

```Java

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.olmosjt.utils.i18n.LocalizedMessageUtils; // Adjust import

@Service
@RequiredArgsConstructor
public class MyUserService {

    private final LocalizedMessageUtils localizedMessageUtils;

    public String getWelcomeMessage(String username) {
        // Assuming "welcome.user" is a key in your messages.properties like:
        // welcome.user=Welcome, {0}!
        return localizedMessageUtils.getMessage("welcome.user", username);
    }

    public String getUserNotFoundMessage(Long userId) {
        // Assuming "user.not.found.id" is like: User with ID {0} was not found.
        return localizedMessageUtils.getMessage("user.not.found.id", userId);
    }
}
```