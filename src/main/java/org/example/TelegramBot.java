package org.example;

import com.vdurmont.emoji.EmojiParser;
import org.example.Chats.ChatsService;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private static TelegramBot instance = null;
    private final UsersService usersService = new UsersService();
    private final ChatsService chatsService = new ChatsService();

    private static final Map<Long, LocalDate> usersUpdateTime = new HashMap<>();
    private static final Map<Long, LocalDate> chatsUpdateTime = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public TelegramBot(String botToken) {
        this.botToken = botToken;
        instance = this;
    }
    @Override
    public String getBotUsername() {
        return "Викторина бот"; // Замените на имя вашего бота
    }
    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> {
            if (update.hasCallbackQuery()) {
                checkUser(update.getCallbackQuery().getFrom());
                checkChat(update.getCallbackQuery().getMessage().getChat());
                executeCallback(update);
                return;
            }

            Message message = update.getMessage();

            if (message == null) {
                logger.debug("Получено пустое сообщение (например, событие, связанное с вызовами, действиями пользователей, или callback-запросами)");
                return;
            }

            checkUser(message.getFrom());
            checkChat(message.getChat());

            if (message.getText() == null) {
                logger.debug("Полученное сообщение не содержит текста");
                return;
            }

            logger.debug("Получено сообщение из чата " + message.getChat().getId().toString() +": "+ message.getText());
            executeMessage(update);
        },executor);

        logger.info("Количество активных потоков обработки команд: " + executor.getActiveCount());
    }

    private void checkUser(User user) {
        if (!usersService.checkUser(user)) {
            usersService.addUser(user);
            usersUpdateTime.put(user.getId(), LocalDate.now());
        }
        else {
            if (!Objects.equals(usersUpdateTime.get(user.getId()), LocalDate.now())) {
                usersService.updateUser(user);
                usersUpdateTime.put(user.getId(), LocalDate.now());
            }
        }
    }
    private void checkChat(Chat chat) {
        if (!chatsService.checkChat(chat.getId())) {
            chatsService.addChat(chat);
            chatsUpdateTime.put(chat.getId(), LocalDate.now());
        }
        else {
            if (!Objects.equals(chatsUpdateTime.get(chat.getId()), LocalDate.now())) {
                chatsService.updateChat(chat);
                chatsUpdateTime.put(chat.getId(), LocalDate.now());
            }
        }
    }
    public static TelegramBot getInstance() {
        return instance;
    }
    private void botInfo(Message message) {
        Long chatID = message.getChatId();
        sendMessage(chatID, """
                 Бот для развлечений! Команды:
                 """);
    }
    public Integer sendMessage(Long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
        return null;
    }
    public Integer sendReplyMessage(Long chatId, Integer replyMessageID, String messageText) {
        SendMessage response = new SendMessage();
        response.setChatId(chatId);
        response.setText(messageText);
        response.setReplyToMessageId(replyMessageID); // Привязываем к конкретному сообщению

        try {
            return execute(response).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
            return null;
        }
    }
    public boolean sendImgMessage (Long chatId, String text, File imageFile) {

        if (imageFile.exists()) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(imageFile));
            sendPhoto.setCaption(text);

            try {
                execute(sendPhoto);
                return true;
            } catch (TelegramApiException e) {
                logger.error("Ошибка при отправке сообщения: ", e);
            }
        } else {
            logger.error("Ошибка получения изображения для отправки сообщения с картинкой: " + imageFile.getPath());
            return false;
        }
        return false;
    }
    public boolean editMessage(Long chatId, Integer messageID, String newMessageText) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageID);
        message.setText(newMessageText);
        try {
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Ошибка при попытке обновить сообщение: ", e);
            return false;
        }
    }
    public Boolean checkAccessPrivileges(Message message) {
        Long chatID = message.getChatId();
        String chatType = message.getChat().isGroupChat() ? "Group" : message.getChat().isSuperGroupChat() ? "Supergroup" : message.getChat().isChannelChat() ? "Channel" : "Private";

        switch (chatType) {
            case "Private":
                logger.debug("Получено сообщение из приватного чата");
                return true;
            case "Group", "Supergroup":
                logger.debug("Получено сообщение из группового чата");
                try {
                    GetChatMember getChatMember = new GetChatMember();
                    getChatMember.setChatId(chatID);
                    getChatMember.setUserId(this.execute(new GetMe()).getId());

                    ChatMember chatMember = execute(getChatMember);
                    return Objects.equals(chatMember.getStatus(), "administrator");

                } catch (Exception e) {
                    logger.error("Ошибка проверки прав доступа у бота: " + e);
                    return false;
                }
            case "Channel":
                logger.debug("Получено сообщение из канала");
                return false;
            default:
                logger.error("Неизвестный тип канал получения сообщений");
        }
        return false;
    }
    private void executeMessage(Update update) {
        Message message = update.getMessage();
        if (update.hasMessage()) {
            String[] parts = message.getText().split(" ", 2); // Разделяем строку по первому пробелу
            /*
            parts[0] - команда
            parts[1] - параметр (сейчас не используется)
             */

            String command = parts[0];

            switch (command) {
                case "/bot_info", "/bot_info@ChatGamePidor_Bot", "/help", "/help@ChatGamePidor_Bot" -> botInfo(message);
                default -> test();
            }
        }
    }
    private void test() {
        SendMessage message = new SendMessage();
        message.setChatId(270459384L); //указан ID пользователя, НЕ ЧАТА!!!
        message.setText("проверка отправки сообщения");
        try {
            execute(message).getMessageId();
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }

    }

    private void executeCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();

        // Обрабатываем нажатие inline кнопки
        switch (callbackData) {

            default -> logger.error("Ошибка работы inline кнопок");
        }
    }

}
