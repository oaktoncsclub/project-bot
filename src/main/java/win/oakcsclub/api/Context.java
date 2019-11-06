package win.oakcsclub.api;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;
import win.oakcsclub.Database;
import win.oakcsclub.PermissionLevel;

import java.awt.*;
import java.util.function.Consumer;

public class Context {
    public final Message message;
    public final PermissionLevel highestPermission;
    public final String content;
    public final String prefixUsed;
    public final String commandNameUsed;

    public Context(Message message,
                   PermissionLevel highestPermission,
                   String content,
                   String prefixUsed,
                   String commandNameUsed) {
        this.message = message;
        this.highestPermission = highestPermission;
        this.content = content;
        this.prefixUsed = prefixUsed;
        this.commandNameUsed = commandNameUsed;
    }

    // helper methods
    public Mono<Message> createMessage(String content){
        return message.getChannel().flatMap(channel -> channel.createMessage(content));
    }
    public Mono<Message> createMessage(Consumer<MessageCreateSpec> spec){
        return message.getChannel().flatMap(channel -> channel.createMessage(spec));
    }
    public Mono<Message> createEmbed(Consumer<EmbedCreateSpec> spec){
        return message.getChannel().flatMap(channel -> channel.createMessage(sp -> sp.setEmbed(spec)));
    }

    public Mono<Message> userError(String message){
        return createEmbed(spec -> spec.setTitle("Error:").setDescription(message).setColor(Color.RED.brighter()));
    }

    public String getArguments(){
        return content
                .replaceFirst(prefixUsed,"")
                .replaceFirst(commandNameUsed,"")
                .trim();
    }



}
