package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "Bot_NAME";
    public static final String TELEGRAM_BOT_TOKEN = "TOKEN_NAME";
    public static final String OPEN_AI_TOKEN = "AI_TOKEN";
    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null; // будет хранить текущий режим диалога
    private ArrayList<String> list = new ArrayList<>();
    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }
     private UserInfo me;
    private UserInfo he;
    private int questionCount;

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if(message.startsWith("/start")){
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");// text from file text.txt
            sendTextMessage(text);

            showMainMenu("главное меню бота","/start",
                    "генерация Tinder-профля\uD83D\uDE0E","/profile",
                    "сообщение для знакомства \uD83E\uDD70","/opener",
                    "переписка от вашего имени\uD83D\uDE08","/message",
                    "переписка со звездами \uD83D\uDD25 ","/date",
                    "задать вопрос чату GPT \uD83E\uDDE0","/gpt");
            return; // после остальные команды не исполняются
        }

        if(message.startsWith("/gpt")){
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if(currentMode == DialogMode.GPT && !isMessageCommand()) {
            Message msg = sendTextMessage("wait a moment...");
            String prompt = loadPrompt("gpt");
             String answer = chatGPT.sendMessage(prompt, message);
             updateTextMessage(msg,answer);
          return;
       }

        //command DATE
        if(message.startsWith("/date")){
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ariana Grande","date_grande",
                    "Margot Robbie","date_robbie",
                    "Zendaya","date_zendaya",
                    "Ryan Gosling","date_gosling",
                    "Tom Hardy", "date_hardy");
            return;
        }
        if(currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("date_")){
                sendPhotoMessage(query);
                sendTextMessage("Great choice!\nNow try to arrange a date using 5 messages!");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }
            Message msg = sendTextMessage("wait a moment...");
            String answer = chatGPT.addMessage(message); // обработка сообщений от человека
            updateTextMessage(msg,answer);
            return;
        }
        // command MESSAGE
        if(message.startsWith("/message")){
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Send your messages to the chat","next message","message_next","ask on a date","message_date");
            return;
        }
        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n",list);

                Message msg = sendTextMessage(" wait a few moments...chatGPT is generating a response");
                String answer = chatGPT.sendMessage(prompt, userChatHistory); // 10 sec
                updateTextMessage(msg,answer);
                return;
            }
            list.add(message);
            return;
        }

        //command PROFILE
        if(message.startsWith("/profile")){ // инициализация общения
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo(); // обнуляем всю инфу, которая известна
            questionCount = 1;
            sendTextMessage("How old are you?"); // задаем 1й вопрос
            return;
        }
        if(currentMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("What do you do?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("Do you have a hobby?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("What traits annoy you in people?");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("What are you looking for on here?");
                    return;
                case 5:
                    me.goals = message;

                    String aboutMyself = me.toString(); // преобразуем в строку инфу о человеке
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage(" wait a few moments...chatGPT is generating a response \uD83E\uDDE0");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg,answer);
                    return;
            }
            return;
        }
        //command OPENER
        if(message.startsWith("/opener")){
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            he = new UserInfo();
            questionCount = 1;
            sendTextMessage("Name of a guy?");
            return;
        }
        if(currentMode == DialogMode.OPENER && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    he.name =message;
                    questionCount =2;
                    sendTextMessage("How old is he?");
                    return;

                case 2:
                    he.age =message;
                    questionCount =3;
                    sendTextMessage("Does he have any hobbies?");
                    return;
                case 3:
                    he.hobby =message;
                    questionCount =4;
                    sendTextMessage("What does he do?");
                    return;
                case 4:
                he.occupation=message;
                questionCount =5;
                sendTextMessage("What is he looking for?");
                return;
                case 5:
                    he.goals =message;
                    String aboutFriend = message;
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage(" wait a few moments...chatGPT is generating a response \uD83E\uDDE0");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg,answer);
                    return;

            }
           return;
        }


     sendTextMessage("*Hello there!*");
        sendPhotoMessage("*_Привет!_*");

     sendTextMessage("_Your message is _" + message);

       sendTextButtonsMessage("Choose a bot option: ", "START", "start",
                                                                          "STOP", "stop");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
