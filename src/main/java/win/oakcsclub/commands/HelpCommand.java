package win.oakcsclub.commands;

import reactor.core.publisher.Mono;
import win.oakcsclub.Main;
import win.oakcsclub.PermissionLevel;
import win.oakcsclub.api.Command;
import win.oakcsclub.api.Context;

import java.awt.*;
import java.time.Instant;
import java.util.List;

import static win.oakcsclub.Util.first;
import static win.oakcsclub.Util.listOf;

public class HelpCommand implements Command {

    @Override
    public List<String> getNames() {
        return listOf("help");
    }

    @Override
    public boolean isDisabled() { return false; }

    @Override
    public String getShortHelpMenu() {
        return "tells you how commands work";
    }

    @Override
    public String getLongHelpMenu() {
        return "you can call this command with zero or one arguments. " +
                "Calling it with none will give you the main help menu, " +
                "which shows all commands and a brief description of each. " +
                "Or, you can put the command name you want to learn more about as an argument " +
                "to get the longer description of that command";
    }

    @Override
    public PermissionLevel getPermissionLevelNeeded() {
        return PermissionLevel.MEMBER;
    }

    @Override
    public Mono<Void> run(Context context) {
        if(context.getArguments().isEmpty()){
            // main help menu
            return context.createEmbed(emb ->{
                emb.setTitle("Help Menu");
                emb.setTimestamp(Instant.now());
                for(Command command : Main.commands){
                    emb.addField(command.getNames().get(0),command.getShortHelpMenu(),false);
                }
                emb.setColor(Color.CYAN);
                // IMPROVABLE: this could be made to look better
            }).then();
        }else {
            Command command = first(Main.commands,(commandTest -> commandTest.getNames().contains(context.getArguments())));
            if(command == null){
                return context.userError("Can't find command with a name of " + context.getArguments()).then();
            }
            return context.createEmbed(emb -> {
                emb.setTitle("Help: " + context.getArguments());
                emb.setDescription(command.getLongHelpMenu());
                emb.setColor(Color.CYAN);
                // IMPROVABLE: this could be made to look better
            }).then();
        }
    }
}
