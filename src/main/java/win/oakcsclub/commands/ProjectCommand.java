package win.oakcsclub.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple6;
import reactor.util.function.Tuples;
import win.oakcsclub.Database;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

import javax.xml.crypto.Data;
import java.awt.*;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectCommand {
    static {
        System.out.println("static init called");
    }


  @CommandX(
      names = "project",
      shortHelp = "Information and other things regarding club projects",
      longHelp = "long help will be coming soon")
  public static Mono<Void> projectCommand(Context context) {
    String projectCommand = context.getArguments().split(" ")[0];
    // TODO if projectCommand is blank, show the help menu
    switch (projectCommand) {
      case "create": return projectCreate(context);
      case "set-leader": return setLeader(context);
      case "set-channel": return setChannel(context);
        case "set-desc": return setDesc(context);
      case "add-member": return mutateMember(true,context);
      case "remove-member": return mutateMember(false,context);
    case "status":
        return status(context);
        case "description":
        case "desc":
            return desc(context);
        case "list": return list(context);
        case "set-role": return setRole(context);
    case "":
        case "help":
        return context.createMessage("" +
            "project commands for users:\n" +
            "```\n" +
            ">>project status [project-name]\n" +
            ">>project description [project-name]\n" +
            ">>project list\n" +
            ">>project help\n" +
            "```\n" +
            "project commands for managing projects:\n" +
            "```\n" +
            ">>project create\n" +
            ">>project set-leader [project-name] [leader mention]\n" +
            ">>project set-channel [project-name] [channel mention]\n" +
            ">>project set-desc [project-name] [desc]\n" +
            ">>project add-member [project-name] [user(s) mentions]\n" +
            ">>project remove-member [project-name] [user(s) mentions]\n" +
            ">>project set-role [project-name] [role-ID]\n" +
            "```").then();
    }
    return context.userError("Can't find project command " + projectCommand).then();
  }

    private static Mono<Void> list(Context context) {

        class Pair<A,B> {
            final A a;
            final B b;
            private Pair(A a, B b) {
                this.a = a;
                this.b = b;
            }
        }
        return context.createEmbed(e -> {
            e.setTitle("Projects:");

            Database.get().withHandle(h ->
                h.createQuery("SELECT name, description FROM project")
                    .map((row,ctx) -> new Pair<>(row.getString("name"), row.getString("description"))))
                .forEach(project -> {
                    String desc = project.b;
                    if(desc.length() > 500) {
                        desc = desc.substring(0,500) + "...";
                    }
                    e.addField(project.a,desc,false);
                });
        }).then();
    }

    private static Mono<Void> desc(Context context) {
        String projectName = context.getArguments()
            .replaceFirst("desc","")
            .replaceFirst("description","")
            .trim().split(" ")[0];

        boolean exists = Database.get().withHandle(h ->
            h.createQuery("SELECT name FROM project WHERE name = ?")
                .bind(0,projectName)
                .mapTo(String.class).findFirst()).isPresent();

        if(!exists){
            return context.userError("Can't find project \"" + projectName + "\"").then();
        }
         String desc =  Database.get().withHandle(h -> h.createQuery("SELECT description FROM project WHERE name = :name")
            .bind("name", projectName).mapTo(String.class)).first();
        return context.createMessage(desc).then();

    }

    private static Random badRandom = new Random(); // not random enough for secure things
                                                  //     random enough to generate random embed colors

    private static Mono<Void> status(Context context) {
        String projectName = context.getArguments().replaceFirst("status","").trim().split(" ")[0];

        boolean exists = Database.get().withHandle(h ->
            h.createQuery("SELECT name FROM project WHERE name = ?")
                .bind(0,projectName)
                .mapTo(String.class).findFirst()).isPresent();

        if(!exists){
            return context.userError("Can't find project \"" + projectName + "\"").then();
        }
        return Database.get().withHandle(h -> h.createQuery("SELECT * FROM project WHERE name = :name")
            .bind("name", projectName)
            .map((row,ctx) -> {
                System.out.println("building status board");
                Snowflake projectLeader = Snowflake.of(row.getString("projectLeader"));
                Snowflake projectChannel = Snowflake.of(row.getString("channel"));
                Snowflake roleId = Snowflake.of(row.getString("role"));
                String desc = row.getString("description");
                boolean isAcceptingNewMembers = row.getInt("isAcceptingNewMembers") == 1;
                boolean isRunning = row.getInt("isStillRunning") == 1;
                Mono<User> username = context.message.getClient().getUserById(projectLeader);//.cache();
                // because we call username twice, it's we should cache the value.
                // turns out cache does other things. Whatever

                String channelMention = "<#" + projectChannel.asString() + ">";

                Mono<String> roleNameMono = context.message.getGuild()
                    .flatMap(i ->  i.getRoleById(roleId))
                    .map(Role::getName)
                    .switchIfEmpty(Mono.just("ohno can't find role"));

                Mono<Member> member = context.message.getGuild().flatMap(guild -> username.flatMap(it -> it.asMember(guild.getId())));


                Mono<String> titleMono = member.flatMap(i ->
                    Mono.justOrEmpty(i.getNickname().map(nick -> nick + " (" + i.getUsername() + "#" + i.getDiscriminator() + ")")))
                    .switchIfEmpty(member.map(i -> i.getUsername() + "#" + i.getDiscriminator()));

                return username
                    .zipWith(roleNameMono)
                    .zipWith(titleMono)
                    .flatMap(tuple -> context.createEmbed(e -> {
                        System.out.println("creating embed");
                        User user = tuple.getT1().getT1();

                        String roleName = tuple.getT1().getT2();
                        String title = tuple.getT2();
                        e.setColor(new Color(badRandom.nextFloat(),badRandom.nextFloat(),badRandom.nextFloat()).brighter());
                        e.setTitle(projectName);
                        e.setAuthor("Lead: " + title,null,user.getAvatarUrl());
                        e.setDescription(
                            isRunning ? isAcceptingNewMembers ? "Still accepting new members" : "Not accepting new members" : "Project has concluded");
                        e.addField("Channel:",channelMention,false);
                        e.addField("Role:","@" + roleName,false);
                        e.addField("Description:",desc,false);
                    }));
            })).first().then();
    }

    private static final Pattern channelMatcher = Pattern.compile(".*?<#(\\d+)>.*?");
    private static final Pattern manyChannelMatcher = Pattern.compile(".*?<#\\d+>.*?<#\\d+>.*?");

    private static Mono<Void> projectCreate(Context context) {

        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
            );

    Snowflake id = context.message.getAuthor().get().getId();
    Snowflake channelId = context.message.getChannelId();
    Mono<String> nameMessage =
        promptFor(
                "What's the name of the project?",
                id,
                channelId,
                context,
                m -> {
                  String str = m.getContent().get();
                  if (!str.startsWith("project-")) {
                    return Optional.of("Project name needs to start with \"project-\"");
                  }
                  if (!str.chars().allMatch(c -> c >= 0x20 && c < 0x7F)) {
                    return Optional.of("Project name must be entirely printable ascii characters");
                  }
                  return Optional.empty();
                },Duration.ofMinutes(1))
            .map(m -> m.getContent().get());

    Mono<String> projectChannel =
        promptFor(
                "Please mention the channel of this project",
                id,
                channelId,
                context,
                m -> {
                  String content = m.getContent().get();
                  if (!channelMatcher.matcher(content).matches())
                      return Optional.of("You need to mention a channel!");
                  if (manyChannelMatcher.matcher(content).matches())
                    return Optional.of("You've mentioned more then one channel.");
                  // we'll just assume it's correct.
                  return Optional.empty();
                },Duration.ofMinutes(1))
            .map(m -> {
                Matcher f = channelMatcher.matcher(m.getContent().get());
                f.find(); // this does matter don't delete it
                return f.group(1);
            });

        Mono<String> projectRole =
            promptFor("Please mention the role that project members will be given", id,channelId,context, m -> {
                  if (m.mentionsEveryone()) return Optional.of("bad");
                  if (m.getRoleMentionIds().size() == 0)
                    return Optional.of("You need to mention a user!");
                  if (m.getRoleMentionIds().size() > 1)
                    return Optional.of("You've mentioned more then one user.");
                  // we'll just assume it's correct.
                  return Optional.empty();
                },Duration.ofMinutes(1)).map(m -> m.getRoleMentionIds().iterator().next().asString());

    Mono<String> projectLeader =
        promptFor(
                "Please mention the leader of this project",
                id,
                channelId,
                context,
                m -> {
                  if (m.mentionsEveryone()) return Optional.of("bad");
                  if (m.getUserMentionIds().size() == 0)
                    return Optional.of("You need to mention a user!");
                  if (m.getUserMentionIds().size() > 1)
                    return Optional.of("You've mentioned more then one user.");
                  // we'll just assume it's correct.
                  return Optional.empty();
                },Duration.ofMinutes(1))
            .map(m -> m.getUserMentionIds().iterator().next().asString());

    Mono<Tuple2<Snowflake, String>> projectLeaderAndDescription =
        projectLeader
            .map(Snowflake::of)
            .zipWhen(
                author ->
                    promptFor(
                            "<@"
                                + author.asString()
                                + ">, please enter a short description of your project",
                            author,
                            channelId,
                            context,
                            m -> Optional.empty(),
                        Duration.ofMinutes(15))
                        .map(m -> m.getContent().get()));



    // assume that the project is still running

    Mono<Boolean> isProjectAcceptingNewMembers =
        promptFor(
                "<@" + id.asString() + ">, Is the project still accepting new members? (y/n)",
                id,
                channelId,
                context,
                m -> {
                  String c = m.getContent().get();
                  if (c.equalsIgnoreCase("y") || c.equalsIgnoreCase("n")) return Optional.empty();
                  return Optional.of("response needs to either be \"y\" or \"n\"");
                },Duration.ofMinutes(1))
            .map(m -> m.getContent().get().equalsIgnoreCase("y"));

    //           name,  project channel, project leader, desc,  isAcceptingNewMembers, project role
    Mono<Tuple6<String, String,          String,         String, Boolean,             String>> everything =
        nameMessage.flatMap(name ->
            projectChannel.flatMap(projectChannelName ->
                projectLeaderAndDescription.flatMap(projectLeaderAndDesc ->
                    isProjectAcceptingNewMembers.flatMap(isAccepting ->
                        projectRole.map(role ->
                            Tuples.of(
                                name,
                                projectChannelName,
                                projectLeaderAndDesc.getT1().asString(),
                                projectLeaderAndDesc.getT2(),
                                isAccepting,
                                role))))));


    Mono<Void> endAll =  everything.flatMap( tuple -> {
        String name              = tuple.getT1();
        String projectChannelStr = tuple.getT2();
        String projectLeaderStr  = tuple.getT3();
        String description       = tuple.getT4();
        Boolean isAccepting      = tuple.getT5();
        String role              = tuple.getT6();

        Database.get().useHandle(h ->
            h.execute("INSERT INTO project (name, projectLeader, channel, role, description, isAcceptingNewMembers, isStillRunning) VALUES (?,?,?,?,?,?,1)",
            name, projectLeaderStr, projectChannelStr,role, description, isAccepting ? 1 : 0));

        return context.createMessage("Done!");
    }).switchIfEmpty(context.userError("Timed out")).then();


        return allowed.flatMap(b -> {
            if(b){
                return endAll;
            } else {
                return context.userError("you don't have permission").then();
            }
        });

  }

    private static Mono<Void> mutateMember(boolean add, Context context){
        String[] split = context.getArguments().replaceFirst(add ? "add-member" : "remove-member","").trim().split(" ");
        String projectName = split[0];

        boolean exists = Database.get().withHandle(h ->
            h.createQuery("SELECT name FROM project WHERE name = ?")
                .bind(0,projectName)
                .mapTo(String.class).findFirst()).isPresent();

        if(!exists){
            return context.userError("Can't find project \"" + projectName + "\"").then();
        }

        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
                || Database.get().withHandle(h ->
                h.createQuery("SELECT projectLeader FROM project WHERE name = :name")
                    .bind("name",projectName)
                    .mapTo(String.class)
                    .first()
                    .equals(author.getId().asString())
            )
        );

        Set<Snowflake> membersToAdd = context.message.getUserMentionIds();

        return allowed.flatMap(b -> {
            if(!b){
                return context.userError("you don't have permission");
            } else {
                String roleID = Database.get().withHandle(h -> {
                    for(Snowflake snowflake : membersToAdd){
                        boolean in = h.createQuery("SELECT (project) FROM projectMember WHERE id = :id AND project = :project")
                            .bind("id",snowflake.asString())
                            .bind("project",projectName)
                            .mapTo(String.class)
                            .findFirst()
                            .isPresent();
                        if(add && !in){
                            h.execute("INSERT INTO projectMember (id, project) VALUES (?,?)",snowflake.asString(),projectName);
                        } else if(!in){
                            h.execute("DELETE FROM projectMember WHERE project = ? AND id = ?",projectName,snowflake.asString());
                        }
                    }
                    return h.createQuery("SELECT role FROM project WHERE name = ?")
                        .bind(0,projectName)
                        .mapTo(String.class)
                        .first();
                });
                System.out.println("roleID: " + roleID);

                Mono<String> changeRoles = context.message
                    .getUserMentions()
                    .flatMap(member ->
                        context.message.getGuild().flatMap(it -> member.asMember(it.getId())))
                    .flatMap(it -> add ? it.addRole(Snowflake.of(roleID),"Person has been added to this project").thenReturn("added")
                                     :   it.removeRole(Snowflake.of(roleID),"Person has been removed from this project").thenReturn("removed"))
                    .last();

                return context.createMessage("added to database")
                    .then(changeRoles)
                    .then(context.createMessage("Done!"));
            }
        }).then();


    }

    private static Mono<Void> setLeader(Context context) {
        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
        );
        String projectName = context.getArguments().replaceFirst("set-leader","").trim().split(" ")[0];
        if(context.message.getUserMentionIds().size() != 1) return context.userError("mention exactly 1 user").then();
        String leaderId = context.message.getUserMentionIds().iterator().next().asString();

        return allowed.flatMap(b -> {
            if(b){
                Database.get().useHandle(h -> h.execute("UPDATE project SET projectLeader = ? WHERE name = ?",leaderId,projectName));
                return context.createMessage("Success!");
            } else {
                return context.userError("permissione error");
            }
        }).then();
    }
    private static Mono<Void> setChannel(Context context) {
        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
        );
        String[] split = context.getArguments().replaceFirst("set-channel","").trim().split(" ");
        String projectName = split[0];
        String arguments = split[1];
        Matcher m = channelMatcher.matcher(arguments);
        if(!m.find()){
            return context.userError("Can't find channel \"" + arguments + "\"").then();
        }
        String channelId = m.group();

        return allowed.flatMap(b -> {
            if(b){
                Database.get().useHandle(h -> h.execute("UPDATE project SET channel = ? WHERE name = ?",channelId,projectName));
                return context.createMessage("Success!");
            } else {
                return context.userError("permission error");
            }
        }).then();
    }

    private static Mono<Void> setDesc(Context context) {

        String[] split = context.getArguments().replaceFirst("set-desc","").trim().split(" ",2);
        if(split.length == 1) return context.userError("Nope, you need to have a description").then();
        String projectName = split[0];
        String desc = split[1];
        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
                || Database.get().withHandle(h ->
                h.createQuery("SELECT projectLeader FROM project WHERE name = :name")
                    .bind("name",projectName)
                    .mapTo(String.class)
                    .first()
                    .equals(author.getId().asString())
            )
        );
        return allowed.flatMap(b -> {
            if(b){
                Database.get().useHandle(h -> h.execute("UPDATE project SET description = ? WHERE name = ?",desc,projectName));
                return context.createMessage("Success!");
            } else {
                return context.userError("permission error");
            }
        }).then();
    } private static Mono<Void> setRole(Context context) {

        String[] split = context.getArguments().replaceFirst("set-role","").trim().split(" ",2);
        if(split.length == 1) return context.userError("you need to have a role description").then();
        String projectName = split[0];
        String roleID = split[1];
        Mono<Boolean> allowed = context.message.getAuthorAsMember().map(author ->
            author.getRoleIds().contains(Snowflake.of("365228273044553728")) // the [admin] role
                || Database.get().withHandle(h ->
                h.createQuery("SELECT projectLeader FROM project WHERE name = :name")
                    .bind("name",projectName)
                    .mapTo(String.class)
                    .first()
                    .equals(author.getId().asString())
            )
        );
        return allowed.flatMap(b -> {
            if(b){
                Database.get().useHandle(h -> h.execute("UPDATE project SET role = ? WHERE name = ?",roleID,projectName));
                return context.createMessage("Success!");
            } else {
                return context.userError("permission error");
            }
        }).then();
    }


    private static Mono<Message> promptFor(
      String ask,
      Snowflake authorId,
      Snowflake channelId,
      Context context,
      Function<Message, Optional<String>> conditional,
      Duration timeout) {
    return context
        .createMessage(ask)
        .flatMapMany(
            message ->
                message
                    .getClient()
                    .getEventDispatcher()
                    .on(MessageCreateEvent.class)
                    .filter(mess -> mess.getMessage().getAuthor().get().getId().equals(authorId))
                    .filter(mess -> mess.getMessage().getChannelId().equals(channelId)))
        .take(timeout)
        .map(MessageCreateEvent::getMessage)
        .next()
        .flatMap(
            message -> {
              if (!message.getContent().isPresent()) {
                return context
                    .userError("I'm going to need some sort of content in the message")
                    .then(promptFor(ask, authorId, channelId, context, conditional,timeout));
              } else return Mono.just(message);
            })
        .flatMap(
            content -> {
              Optional<String> yes = conditional.apply(content);
              if (yes.isPresent()) {
                return context
                    .userError(yes.get())
                    .then(promptFor(ask, authorId, channelId, context, conditional,timeout));
              } else return Mono.just(content);
            });
  }
}
