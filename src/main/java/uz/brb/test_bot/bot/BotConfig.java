package uz.brb.test_bot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import uz.brb.test_bot.repository.UserRepository;

@Configuration
@RequiredArgsConstructor
public class BotConfig {
    private final UserRepository userRepository;

    @Bean
    public TestBot testBot() {
        TestBot testBot = new TestBot(userRepository);
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(testBot);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return testBot;
    }
}