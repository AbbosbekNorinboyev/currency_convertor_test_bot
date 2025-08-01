package uz.brb.test_bot.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.brb.test_bot.entity.CurrencyRate;
import uz.brb.test_bot.entity.User;
import uz.brb.test_bot.repository.UserRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component(value = "testBot")
@RequiredArgsConstructor
public class TestBot extends TelegramLongPollingBot {
    private final UserRepository userRepository;

    private final Map<Long, String> selectedCurrency = new HashMap<>();
    private final Map<Long, String> enteredAmount = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String data = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            saveAuthUser(update, chatId);

            if (selectedCurrency.containsKey(chatId) && !enteredAmount.containsKey(chatId)) {
                enteredAmount.put(chatId, data); // summani saqlaymiz
                sendConversionTargetButtons(chatId); // valyutaga o‘girish buttonlarini yuboramiz
                return;
            }

            if (data.equals("/start")) {
                sendStartMenu(chatId);
            }

        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (data.equals("GET_RATES")) {
                sendCurrencyButtons(chatId);
                String fromCurrency = data.substring(5);
                selectedCurrency.put(chatId, fromCurrency);
            } else if (data.equals("BACK_TO_START")) {
                sendStartMenu(chatId); // asosiy menyuga qaytadi
            } else if (data.startsWith("CURR_")) {
                String fromCurrency = data.substring(5);
                selectedCurrency.put(chatId, fromCurrency);
                sendText(chatId, "Iltimos, summani kiriting:");
            } else if (data.startsWith("TO_")) {
                try {
                    Gson gson = new Gson();
                    URL url = new URL("https://cbu.uz/oz/arkhiv-kursov-valyut/json/");
                    URLConnection urlConnection = url.openConnection();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    List<CurrencyRate> currencyRates = gson.fromJson(bufferedReader, new TypeToken<List<CurrencyRate>>() {
                    }.getType());

                    if (!currencyRates.isEmpty()) {
                        // Valyutalar
                        String fromCcy = selectedCurrency.remove(chatId); // Masalan: "USD", "UZS"
                        String toCcy = data.substring(3);                 // "TO_USD" -> "USD"
                        BigDecimal amount = new BigDecimal(enteredAmount.remove(chatId)); // kiritilgan summa

                        CurrencyRate fromRate = currencyRates.stream()
                                .filter(r -> r.getCcy().equals(fromCcy))
                                .findFirst()
                                .orElse(null);

                        CurrencyRate toRate = currencyRates.stream()
                                .filter(r -> r.getCcy().equals(toCcy))
                                .findFirst()
                                .orElse(null);

                        BigDecimal result;

                        if (fromCcy.equals("UZS") && toRate != null) {
                            // UZS -> boshqa valyuta
                            BigDecimal rate = new BigDecimal(toRate.getRate());
                            BigDecimal nominal = new BigDecimal(toRate.getNominal());
                            result = amount.divide(rate.divide(nominal, 8, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);
                        } else if (toCcy.equals("UZS") && fromRate != null) {
                            // boshqa valyuta -> UZS
                            BigDecimal rate = new BigDecimal(fromRate.getRate());
                            BigDecimal nominal = new BigDecimal(fromRate.getNominal());
                            result = amount.multiply(rate).divide(nominal, 2, RoundingMode.HALF_UP);
                        } else if (fromRate != null && toRate != null) {
                            // boshqa valyuta -> boshqa valyuta
                            BigDecimal fromRateVal = new BigDecimal(fromRate.getRate());
                            BigDecimal fromNominal = new BigDecimal(fromRate.getNominal());
                            BigDecimal toRateVal = new BigDecimal(toRate.getRate());
                            BigDecimal toNominal = new BigDecimal(toRate.getNominal());

                            BigDecimal uzsAmount = amount.multiply(fromRateVal).divide(fromNominal, 8, RoundingMode.HALF_UP);
                            result = uzsAmount.divide(toRateVal.divide(toNominal, 8, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);
                        } else {
                            sendText(chatId, "❌ Valyuta ma'lumotlari topilmadi.");
                            return;
                        }

                        sendText(chatId, "💱 Valyuta konvertatsiyasi:\n" +
                                "💰 Kiritilgan miqdor: " + amount.stripTrailingZeros().toPlainString() + " " + fromCcy + "\n" +
                                "🔄 Natija: " + result.stripTrailingZeros().toPlainString() + " " + toCcy);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendText(chatId, "❌ Ma'lumotlarni yuklashda xatolik yuz berdi.");
                }
            }
        }
    }

    private void sendStartMenu(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "Assalomu alaykum! Quyidagilardan birini tanlang:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton("💱 Valyuta kursi");
        button1.setCallbackData("GET_RATES");

        InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Ortga");
        backButton.setCallbackData("BACK_TO_START");

        rows.add(List.of(button1));
        rows.add(List.of(backButton));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeSafely(message);
    }

    private void saveAuthUser(Update update, Long chatId) {
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

    private void sendCurrencyButtons(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "Valyutani tanlang (FROM):");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<String> currencies = List.of("UZS", "USD", "EUR", "JPY", "RUB", "KGS"); // Qo‘shimcha valyutalar ham kiritishingiz mumkin
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        for (int i = 0; i < currencies.size(); i++) {
            String currency = currencies.get(i);
            InlineKeyboardButton button = new InlineKeyboardButton(currency);
            button.setText(currency);
            button.setCallbackData("CURR_" + currency);
            row1.add(button);

            // Har 3 tugmadan keyin yangi qator
            if ((i + 1) % 3 == 0 || i == currencies.size() - 1) {
                rows.add(row1);
                row1 = new ArrayList<>();
            }
        }

        rows.addAll(getMainInlineMenu());

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeSafely(message);
    }


    private void sendConversionTargetButtons(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "Qaysi valyutaga o‘girishni xohlaysiz (TO):");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<String> currencies = List.of("UZS", "USD", "EUR", "JPY", "RUB", "KGS");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        for (int i = 0; i < currencies.size(); i++) {
            String currency = currencies.get(i);
            InlineKeyboardButton button = new InlineKeyboardButton(currency);
            button.setCallbackData("TO_" + currency);
            row1.add(button);

            // 3 tadan tugma bo‘lsa, yangi qator boshlaymiz
            if ((i + 1) % 3 == 0 || i == currencies.size() - 1) {
                rows.add(row1);
                row1 = new ArrayList<>();
            }
        }

        rows.addAll(getMainInlineMenu());

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        executeSafely(message);
    }

    private List<List<InlineKeyboardButton>> getMainInlineMenu() {
        InlineKeyboardButton row2 = new InlineKeyboardButton("⬅\uFE0F Ortga");
        row2.setCallbackData("BACK_TO_START");
        return List.of(List.of(row2));
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        executeSafely(message);
    }

    private void executeSafely(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotToken() {
        return "8420697459:AAGH7y8Kp-heaxQn8NEhsAH4H4mVvKLCUv8";
    }

    @Override
    public String getBotUsername() {
        return "t.me/new_test_quiz_bot";
    }
}
