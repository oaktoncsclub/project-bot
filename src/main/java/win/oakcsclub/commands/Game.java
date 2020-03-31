package win.oakcsclub.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Snowflake;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import win.oakcsclub.Database;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;
import org.antlr.v4.runtime.misc.Pair;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class Game {

    @CommandX(names = {"balance", "checkBalance"}, shortHelp = "check your bank account", longHelp = "check the balance of your account")
    public static Mono<Void> checkBalance(Context context){
        int balance = getBalance(context.authorID);
        return context.createMessage("you have " + balance + " :dollar:").then();
    }

    @CommandX(names={"coin-flip","coin","coinflip", "bet"}, shortHelp = "flip a coin", longHelp = "nope")
    public static Mono<Void> coinFlip(Context context){
        int betAmount;
        try {
            betAmount = Integer.parseInt(context.getArguments());
        }catch(NumberFormatException e){
            return context.userError("please supply a valid bet").then();
        }
        if (betAmount <= 0)
            return context.userError("lol nice try").then();
        int balance = getBalance(context.authorID);
        if (betAmount > balance) {
            return context.userError("you're broke").then();
        }
        boolean flippedHeads = new Random().nextBoolean();
        String message;
        if (flippedHeads) {
            balance += betAmount;
            message = "you flipped heads. noice. +" + betAmount + " :dollar:\n" +
                    "your new balance is " + balance + " :dollar:";
        } else {
            balance -= betAmount;
            message = "you flipped tails. -" + betAmount + " :dollar:\n" +
                    "your new balance is " + balance + " :dollar:";
        }
        setBalance(context.authorID, balance);
        return context.createMessage(message).then();

    }

    @CommandX(names = {"leaderboard", "lb"}, shortHelp = "how much money do you have?", longHelp = "check balance")
    public static Mono<Void> leaderboard(Context context){
        List<Pair<Snowflake, Integer>> people = Database.get().withHandle(h ->
                h.createQuery("SELECT * FROM user")
                .map((rs, ctx) -> new Pair<>(Snowflake.of(rs.getString(1)), rs.getInt(2)))
                ).list();
        people.sort(Comparator.comparing(o -> Integer.MAX_VALUE-o.b));
        Mono<Guild> guild = context.message.getGuild().cache();
        Mono<List<String>> names = Flux.fromIterable(people)
            .map(pair -> pair.a)
            .flatMap(snowflake -> guild.flatMap(g -> g.getMemberById(snowflake)))
            .map(Member::getDisplayName)
            .collectList();
        return names.flatMap(namesReal ->
            context.createEmbed(spec -> {
                spec.setTitle("Leaderboard");
                spec.setTimestamp(Instant.now());
                spec.setColor(Color.getHSBColor((float) Math.random(),(float) (Math.random()/2+0.5),(float) (Math.random()/2+0.5)));
                for(int i = 0; i < people.size(); i++){
                    spec.addField((i+1) + ": " + namesReal.get(i), people.get(i).b + " :dollar:", false);
                }
            })
        ).then();
    }

    @CommandX(names = {"give", "pay"}, shortHelp = "give someone money", longHelp = "you give your $ to someone else")
    public static Mono<Void> pay(Context context){
        int paymentAmount;
        Snowflake personPaying = context.authorID; //person who will give money
        Snowflake beingPaid; //person who was mentioned will get money
        try{ //make sure there's a person mentioned
            beingPaid = context.message.getUserMentionIds().iterator().next();
        }catch (Exception e){
            return context.userError("pls mention someone in your request").then();
        }
        if (beingPaid.equals(personPaying))
            return context.userError("don't give yourself money").then();

        try { //make sure there's a positive number
            String messageArgs = context.getArguments();
            paymentAmount = Integer.parseInt(messageArgs.substring(messageArgs.indexOf('>') + 1).trim());
        }catch (NumberFormatException e){
            return context.userError("please just put a normal number").then();
        }
        if (paymentAmount <= 0)
            return context.userError("no").then();
        if (paymentAmount > getBalance(personPaying))
            return context.userError("you don't have that much money").then();

        setBalance(personPaying, getBalance(personPaying) - paymentAmount);
        setBalance(beingPaid, getBalance(beingPaid) + paymentAmount);

        //get their display names for the output message
        String payingName = getNickname(personPaying, context);
        String paidName = getNickname(beingPaid, context);
        String message =  payingName + " gave " + paymentAmount + " to " + paidName + ". Now "
                + payingName + " has " + getBalance(personPaying) + " dollars and " + paidName
                + " has " + getBalance(beingPaid) + " dollars.";
        return context.createMessage(message).then();
    }

    //gets nickname from person in the guild a message was sent in. returns normal username if there's no nickname
    private static String getNickname(Snowflake id, Context context){
        Guild guild = context.message.getGuild().block();

        assert guild != null;
        Member user = guild.getMembers().filter(member -> member.getId().equals(id)).blockFirst();
        assert user != null;
        String name = user.getUsername();
        if(user.getNickname().isPresent()){
            name = user.getNickname().get();
        }
        return name;
    }

    private static int getBalance(Snowflake id){
        Optional<Integer> balance = Database.get().withHandle(h ->
            h.createQuery("SELECT (money) FROM user WHERE id =:id")
                    .bind("id", id.asString())
                    .mapTo(Integer.class).findFirst()
        );
        return balance.orElseGet(() -> {
            Database.get().useHandle(handle ->
                    handle.execute("INSERT INTO user (id, money) VALUES (?, 100)", id.asString())
            );
            return 100;
        });
    }

    private static void setBalance(Snowflake id, int newBalance) {
        Database.get().useHandle(handle ->
            handle.createUpdate("update user set money=:money where id=:id")
                .bind("money", newBalance)
                .bind("id", id.asString())
                .execute()
        );
    }
}
