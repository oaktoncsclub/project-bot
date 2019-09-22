package win.oakcsclub;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import win.oakcsclub.api.Command;
import win.oakcsclub.api.CommandLoader;
import win.oakcsclub.api.Context;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static win.oakcsclub.Util.*;

public class Main {
  public static void main(String[] args) {
      // this is the main client.
      DiscordClient client = new DiscordClientBuilder(getKey()).build();
      // block means wait until it completes.
      // completes can mean three things:
      //  a) it completes with a value
      //       Mono<Void> can never complete with a value because Void is unrepresentable
      //  b) it completes without a value.
      //       this will return null if this happens
      //  c) it completes pre-maturely because of an error
      //       like an exception
      //       it will then throw said exception
      Mono.first(loadCommands(client),client.login()).block();
      // this will block till loadCommands(client) completes (it shouldn't),
      // the bot logs out, or there's an error
  }




  public static List<Command> commands = CommandLoader.loadCommands();

  static {
    commands.forEach(command -> System.out.println("loaded command " + command.getNames().get(0)));
  }

  private static PrefixFetcher prefixFetcher = new SinglePrefixFetcher(">>");

  private static Set<Snowflake> masterAdmins = setOf(Snowflake.of("293853365891235841"));

  // never completes, only errors.
  // this is a pretty horrible method
  // but it really can't be much better
  // the hope is that no one ever has to touch it again
  // lol
  private static Mono<Void> loadCommands(DiscordClient client){
      return client.getEventDispatcher()
              .on(MessageCreateEvent.class)
              .filter(message -> message.getMessage().getContent().isPresent())
              // get the prefix, in an async way
              .flatMap(message -> prefixFetcher.getPrefixForMessage(message).map(prefix -> Tuples.of(message,prefix)))
              // get the permission level of the user
              .flatMap(tuple -> getLevel(tuple.getT1()).map(level -> Tuples.of(tuple.getT1(),tuple.getT2(),level)))
              // make sure it actually starts with the prefix
              .filter(tuple -> tuple.getT1()
                      .getMessage()
                      .getContent()
                      .map(content -> content.startsWith(tuple.getT2()))
                      .orElse(false))
              // find the command we want to use.
              .flatMap(tuple -> {
                  String content = tuple.getT1().getMessage().getContent().get();
                  String commandName = content.replaceFirst(tuple.getT2(),"").split("\\s")[0];
                  Command command = first(commands,(commandTest -> commandTest.getNames().contains(commandName)));
                  if(command == null){
                      // can't find command. Ignore? no, why don't we make it even cooler and suggest the correct command
                      command = new NoCommandCommand(commandName);
                  }
                  Context context = new Context(
                          tuple.getT1().getMessage(), // the message object
                          tuple.getT3(), // the permission level
                          content,// content
                          tuple.getT2(),// the prefix
                          commandName); // the command name used
                  return command.run(context)
                          .then()
                          .onErrorResume(exception -> {
                              exception.printStackTrace();// logging
                              return context.createEmbed(emb ->
                                      emb.setTitle("Internal Error:" + exception.getClass().getSimpleName())
                                         .setDescription(exception.getMessage())
                                         .setColor(Color.RED.brighter())
                              ).then();
                          });
              })
              .last();

  }
  private static Mono<PermissionLevel> getLevel(MessageCreateEvent message){
      if(message.getMessage().getAuthor().map(author -> masterAdmins.contains(author.getId())).orElse(false)){
          return Mono.just(PermissionLevel.BOT_ADMIN);
      }
    return Mono.justOrEmpty(message.getMember())
        .flatMap(Member::getBasePermissions)
        .filter(permissions -> permissions.contains(Permission.ADMINISTRATOR))
        .map(e -> PermissionLevel.SERVER_ADMIN)
        .defaultIfEmpty(PermissionLevel.MEMBER);
  }

  private static String getKey(){
      // load key.txt
      File key = new File("key.txt");
      try {
        String result = Files.readAllLines(key.toPath()).get(0);
        return result.trim(); // if the key.txt ends with \n, it will break stuff. trim it away
      } catch(IOException e){
          throw new IllegalStateException("key.txt must be present and readable to start the bot",e);
      }
  }
}
