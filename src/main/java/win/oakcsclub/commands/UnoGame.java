package win.oakcsclub.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UnoGame {
    private static boolean isUnoPlaying = false;

    @CommandX(names = {"Uno","uno"}, shortHelp = "play uno", longHelp = "it's uno dude")
    public static Mono<Void> uno(Context context) {
        if (isUnoPlaying) return context.userError("There's already a game").then();
        Mono.fromRunnable(() -> {
            isUnoPlaying = true;
            try{
                playUno(context);
            } catch (Exception e){
                e.printStackTrace();
                context.createEmbed(spec -> {
                    spec.setColor(Color.red);
                    spec.setTitle("Internal Error");
                    spec.setDescription("" + e.getMessage());
                }).block();
            }
            isUnoPlaying = false;
        }).publishOn(Schedulers.elastic()).subscribe();
        return Mono.empty();
    }

    private static void playUno(Context context){
        List<Snowflake> players = new ArrayList<>();
        players.add(context.authorID);
        context.createMessage("type join to join game").block();
        context.message.getClient().getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(m -> m.getChannelId().equals(context.message.getChannelId()))
                .filter(m -> m.getAuthor().isPresent())
                .takeUntil(m -> m.getContent().isPresent()&&m.getContent().get().equals("start"))
                .filter(m -> m.getContent().isPresent() && m.getContent().get().equals("join"))
                .take(Duration.ofSeconds(30))
                .flatMap(m -> {
                    if (players.contains(m.getAuthor().get().getId())) {
                        return context.userError("don't join twice pls");
                    } else {
                        players.add(m.getAuthor().get().getId());
                        return context.createMessage("you've been added to the game");
                    }
                }).blockLast();
        if(players.size()==1){
            context.createMessage("not enough people to start :(").block();
            return;
        }
        context.createMessage("picking done! " + players.size() + " players").block();

        List<Card> deck = new ArrayList<>();
        List<Card> discards = new ArrayList<>();
        for (int x = 0; x < 10 ; x++) {
            for (CardColor color : CardColor.values()) {
                for (int i = 0; i < 10; i++) {
                    deck.add(new NormalCard(color, i));
                    if (i != 0) {
                        deck.add(new NormalCard(color, i));
                    }
                }
                for (FuckedUpCardType type : FuckedUpCardType.values()) {
                    deck.add(new FuckedUpCard(color, type));
                    deck.add(new FuckedUpCard(color, type));
                }
            }
        }

        Collections.shuffle(deck);
        List<Player> hands = new ArrayList<>();
        for(Snowflake snowflake : players){
            String displayName = context.message.getGuild()
                    .flatMap(guild -> guild.getMemberById(snowflake))
                    .map(Member::getDisplayName)
                    .block();
            Player currPlayer = new Player(snowflake, displayName, context.message.getClient().getUserById(snowflake).block().getPrivateChannel().block());
            for(int i=0; i<7; i++){
                currPlayer.addToHand(deck.remove(0));
            }
            hands.add(currPlayer);
        }
        for(Player player : hands){
            player.sendPrivateMessage(player.playerName + "'s hand is "+ player.toString()).block();
        }
        int index = 0;
        Card topCard = deck.remove(0);
        while (true) {
            Player player = hands.get(index);
            player.orderCards();
            player.sendPrivateMessage("Your hand is: " + player + "\n top card is: " + topCard).block();
            final Card topCardFinal = topCard;
            List<Card> playableCards = player.cards.stream()
                    .filter(card -> card.willGoOnTopOf(topCardFinal)).collect(Collectors.toList());
            if (playableCards.isEmpty()) {
                Card drawn = deck.remove(0);
                player.addToHand(drawn);
                player.sendPrivateMessage("you had to draw. you drew a: " + drawn).block();
                for (Player subPlayer : hands) {
                    if (player != subPlayer) subPlayer.sendPrivateMessage(player.playerName + " had to draw").block();
                }
                index = (index + 1) % hands.size();
                continue;
            }
            player.sendPrivateMessage("Here are the cards you can play: \n" + playableCards.toString() + "\n Select one by index (0 base)").block();
            Integer number = context.message.getClient().getEventDispatcher().on(MessageCreateEvent.class)
                    .map(MessageCreateEvent::getMessage)
                    .filter(message -> message.getChannelId().equals(player.privateChannel.getId()))
                    .filter(message -> message.getAuthor().get().getId().equals(player.player))
                    .filter(message -> message.getContent().isPresent())
                    .map(message -> Integer.parseInt(message.getContent().get()))
                    .onErrorContinue(NumberFormatException.class, (a, b) -> player.sendPrivateMessage("no").block())
                    .take(Duration.ofSeconds(60))
                    .next()
                    .block();
            if (number == null || number >= playableCards.size()) {
                hands.remove(player);
                player.sendPrivateMessage("you've been timed out or typed the wrong number").block();
                // TODO make nicer
            } else {
                Card toRemove = playableCards.get(number);
                player.removeFromHand(toRemove);
                discards.add(topCard);
                if(discards.size()>20){
                    Collections.shuffle(discards);
                    deck.addAll(discards);
                    discards.clear();
                }
                topCard = toRemove;
                if (topCard instanceof FuckedUpCard) {
                    switch (((FuckedUpCard) topCard).type) {
                        case SKIP:
                            Player skippedPlayer = hands.get((index + 1) % hands.size());
                            for (Player subPerson : hands) {
                                subPerson.sendPrivateMessage(skippedPlayer.playerName + " was skipped by " + player.playerName).block();
                            }
                            index++;
                            break;
                        case REVERSE:
                            Collections.reverse(hands);
                            index = hands.size() - index - 1;
                            for (Player subPerson : hands) {
                                subPerson.sendPrivateMessage("order was reversed by " + player.playerName).block();
                            }
                            break;
                        case PLUS_TWO:
                            Player sadPlayer = hands.get((index + 1) % hands.size());
                            sadPlayer.addToHand(deck.remove(0));
                            sadPlayer.addToHand(deck.remove(0));
                            for (Player subPerson : hands) {
                                subPerson.sendPrivateMessage(sadPlayer.playerName + " drew 2 because of " + player.playerName).block();
                            }
                            index=(index+1)%hands.size();
                    }//switch
                }//if FuckedUpCard
                player.sendPrivateMessage("card played").block();
            }// if the user plays a card
            for (Player subPlayer: hands){
                if(subPlayer.cards.size()==0){
                    for (Player subSubPlayer: hands){
                        subSubPlayer.sendPrivateMessage(subPlayer.playerName + " won!").block();
                    }
                    context.createMessage(player.playerName + " won! :tada:").block();
                    return;
                }
            }
            for(Player subPlayer: hands){
                if(subPlayer.cards.size()==1){
                    for (Player subSubPlayer: hands){
                        subSubPlayer.sendPrivateMessage("**" + subPlayer.playerName + " has uno**").block();
                    }
                }
            }
            if(hands.size()==1){
                hands.get(0).sendPrivateMessage("congrats! you won by not losing").block();
                context.createMessage(/*player.playerName*/ hands.get(0).playerName + " was the last one standing").block();
                return;
            }
            index=(index+1)%hands.size();
        }

    }
}

enum CardColor{
    RED(0), YELLOW(1), GREEN(2), BLUE(3);
    final int index;

    CardColor(int index) {
        this.index = index;
    }
}
interface Card extends Comparable<Card> {
    boolean willGoOnTopOf(Card lastCard);
}
/*class WildCard implements Card{
    final boolean isPlusFour;

    WildCard(boolean isPlusFour) {
        this.isPlusFour = isPlusFour;
    }

    @Override
    public boolean willGoOnTopOf(Card lastCard) {
        return true;
    }
}*/
abstract class ColorCard implements Card{
    final CardColor color;

    protected ColorCard(CardColor color) {
        this.color = color;
    }

    //public int compareTo(ColorCard o) {
    //    return this.color.index
    //}

    @Override
    public String toString() {
        return " :" + color.name().toLowerCase() +"_circle:";
    }
}
enum FuckedUpCardType {
    SKIP, REVERSE, PLUS_TWO
}

class FuckedUpCard extends ColorCard{
    final FuckedUpCardType type;

    protected FuckedUpCard(CardColor color, FuckedUpCardType type) {
        super(color);
        this.type = type;
    }

    @Override
    public boolean willGoOnTopOf(Card lastCard) {
        if(lastCard instanceof ColorCard){
            if(this.color==((ColorCard) lastCard).color) return true;
        }
        if(lastCard instanceof FuckedUpCard){
            return this.type == ((FuckedUpCard) lastCard).type;
        }
        return false;
    }

    @Override
    public String toString() {
        switch (type){
            case SKIP:return "S"+super.toString();
            case PLUS_TWO:return "+2"+super.toString();
            case REVERSE:return "R"+super.toString();
            default:return "?? you're bad";
        }
    }

    @Override
    public int compareTo(Card o) {
        if(o instanceof ColorCard){
            if(color.index!=((ColorCard) o).color.index){
                return color.index-((ColorCard) o).color.index;
            }
        }
        if(o instanceof NormalCard){
            return 1;
        }
        return type.ordinal() - ((FuckedUpCard) o).type.ordinal();
    }
}
class NormalCard extends ColorCard{
    final int number;

    public NormalCard(CardColor color, int number) {
        super(color);
        this.number = number;
    }

    @Override
    public String toString() {
        return number + super.toString();
    }
    public boolean willGoOnTopOf(Card lastCard){
        if(lastCard instanceof ColorCard){
            if(this.color==((ColorCard) lastCard).color) return true;
        }
        if(lastCard instanceof NormalCard){
            return this.number == ((NormalCard) lastCard).number;
        }
        return false;
    }

    @Override
    public int compareTo(Card o) {
        if(o instanceof ColorCard){
            if(color.index!=((ColorCard) o).color.index){
                return color.index-((ColorCard) o).color.index;
            }
        }
        if(o instanceof FuckedUpCard){
            return -1;
        }
        return number - ((NormalCard) o).number;
    }
}
class Player {
    List<Card> cards = new ArrayList<>();
    Snowflake player;
    final String playerName;
    final MessageChannel privateChannel;

    public Player(Snowflake player, String playerName, MessageChannel privateChannel) {
        this.player = player;
        this.playerName = playerName;
        this.privateChannel = privateChannel;
    }

    public void orderCards(){
        Collections.sort(cards);
    }


    public Mono<Message> sendPrivateMessage(String context){
        return privateChannel.createMessage(context);
    }
    public void addToHand(Card card){
        cards.add(card);
    }
    public void removeFromHand(Card card){
        if(!(cards.contains(card))) return;
        cards.remove(card);
    }
    @Override
    public String toString(){
        String s = "[";
        for(Card card:cards){
            s+= card+", ";
        }
        s=s.substring(0, s.length()-1);
        s+="]";
        return s;
    }
}