package uz.brb.test_bot.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class Button {
    public List<List<InlineKeyboardButton>> getMainInlineMenu() {
        InlineKeyboardButton row2 = new InlineKeyboardButton("⬅\uFE0F Ortga");
        row2.setCallbackData("BACK_TO_START");
        return List.of(List.of(row2));
    }
}
