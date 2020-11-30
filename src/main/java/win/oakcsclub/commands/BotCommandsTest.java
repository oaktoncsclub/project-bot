package win.oakcsclub.commands;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.MessageEditSpec;
import reactor.core.publisher.Mono;

import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

import java.util.function.Consumer;

public class BotCommandsTest {
    @CommandX(names = {"ping"}, shortHelp = "Time the connection to Discord",
            longHelp = "This command times the time it takes to make a full command request from Discord\n" +
                    "It measures the time from the creation of the message to the response by the bot\n" +
                    "This is not technically \"ping\", but it's even better then ping. " +
                    "It takes into account the command processing stack.")
    public static Mono<Void> pingCommand(Context context){
        return context.message.getChannel().flatMap(channel ->
                channel.createMessage("Pinging....").flatMap(message -> {
                    long msPing = message.getTimestamp().toEpochMilli() - context.message.getTimestamp().toEpochMilli();
                    return message.edit(spec -> spec.setContent("Ping:" + msPing + "ms"));
                }))
                .then(); // ignores the outputted result, because it will be different between commands
    }








    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    @CommandX(
            names = "ping-blocking",
            shortHelp = "ping but use blocking code",
            longHelp = "this ping implementation blocks a thread until it completes.")
    public static Mono<Void> pingCommandBlocking(Context context){
        Message m1 = context.createMessage("Pinging....").block();
        if(m1 == null){
            throw new IllegalStateException("message1 can not be null");
        }
        // compare the timestamps
        long msPing = m1.getTimestamp().toEpochMilli() - context.message.getTimestamp().toEpochMilli();
        m1.edit(spec -> spec.setContent("Ping:" + msPing + "ms")).block();
        return Mono.empty();
    }









    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    @CommandX(
            names = "roll",
            shortHelp = "roll a dice",
            longHelp = "Roll a 6 sided dice.")
    public static Mono<Void> roll(Context context){
        int result = (int)(Math.random()*6) + 1;
        context.createMessage("Rolled a " + result).block();
        return Mono.empty();
    }


























    @CommandX(names = {"contribute","c"}, shortHelp = "start contributing!",
            longHelp = "call this command to get information about contributing.")
    public static Mono<Void> contributeCommand(Context context){
        return context.createMessage("You can start contributing by reading the wiki," +
                " which you find at https://github.com/oaktoncsclub/project-bot/wiki. " +
                "Feel free to ping the maintainer of this project, <@293853365891235841> if you have any questions" +
                " whatsoever. Happy coding! :tada:").then();
    }


    @CommandX(names = {"perms","perm","permission","permissions"}, shortHelp =  "query your permission level",
            longHelp = "call this command to learn what your permission level is")
    public static Mono<Void> permsCommand(Context context){
        return context.createMessage("Your permission level: " + context.highestPermission.toString().toLowerCase())
                .then();
    }

}
