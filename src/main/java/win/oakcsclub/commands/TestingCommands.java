package win.oakcsclub.commands;

import reactor.core.publisher.Mono;

import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

public class TestingCommands {
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


    @CommandX(names = {"perms","perm","permission","permissions"}, shortHelp =  "query your permission level",
            longHelp = "you can call this command with zero or one arguments. " +
                    "Calling it with none will give you the main help menu, " +
                    "which shows all commands and a brief description of each. " +
                    "Or, you can put the command name you want to learn more about as an argument " +
                    "to get the longer description of that command")
    public static Mono<Void> permsCommand(Context context){
        return context.createMessage("Your permission level: " + context.highestPermission.toString().toLowerCase())
                .then();
    }


}
