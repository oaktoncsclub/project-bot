package win.oakcsclub.commands;

import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import win.oakcsclub.PermissionLevel;
import win.oakcsclub.api.Command;
import win.oakcsclub.api.Context;

import java.util.List;

import static win.oakcsclub.Util.*;

public class PingCommand implements Command {
    @Override
    public List<String> getNames() {
        return listOf("ping");
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public String getShortHelpMenu() {
        return "Time the connection to Discord";
    }

    @Override
    public String getLongHelpMenu() {
        return "This command times the time it takes to make a full command request from Discord\n" +
                "It measures the time from the creation of the message to the response by the bot\n" +
                "This is not technicly \"ping\", but it's even better then ping. " +
                "It takes into account the command processing stack.";
    }

    @Override
    public PermissionLevel getPermissionLevelNeeded() {
        return PermissionLevel.MEMBER;
    }

    @Override
    public Mono<Void> run(Context context) {
        return context.message.getChannel().flatMap(channel ->
                channel.createMessage("Pinging....").flatMap(message -> {
                    long msPing = message.getTimestamp().toEpochMilli() - context.message.getTimestamp().toEpochMilli();
                    return message.edit(spec -> spec.setContent("Ping:" + msPing + "ms"));
                }))
                .then(); // ignores the outputted result, because it will be different between commands
    }
}
