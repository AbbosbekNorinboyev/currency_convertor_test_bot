package uz.brb.test_bot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.brb.test_bot.entity.User;
import uz.brb.test_bot.repository.UserRepository;

import java.time.LocalDateTime;
@Component
@RequiredArgsConstructor
public class AuthUserConfig {
    private final UserRepository userRepository;

    public void saveAuthUser(Update update, Long chatId) {
        String username = update.getMessage().getFrom().getUserName(); // 🧑 Username
        String firstName = update.getMessage().getFrom().getFirstName(); // 🪪 First name
        String lastName = update.getMessage().getFrom().getLastName(); // 👤 Last name

        User user = userRepository.findByChatId(chatId).orElse(null);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
            user.setUsername(username);
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastActiveAt(LocalDateTime.now());
        } else {
            user.setLastActiveAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }
}
