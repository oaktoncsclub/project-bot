package win.oakcsclub.commands;

import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import win.oakcsclub.Database;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

import java.util.Optional;
import java.util.Random;

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
