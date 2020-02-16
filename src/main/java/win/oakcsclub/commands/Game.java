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

    @CommandX(names={"coin-flip","coin","coinflip"}, shortHelp = "flip a coin", longHelp = "nope")
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
            return context.userError("youre broke").then();
        }
        boolean flippedHeads = new Random().nextBoolean();
        String message = "idk it doesnt matter we will overwrite";
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

    @CommandX(names = {"leaderboard", "lb"}, shortHelp = "how many :money: do you have?", longHelp = "moneys")
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
