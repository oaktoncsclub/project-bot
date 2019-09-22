package win.oakcsclub;

import kotlin.NotImplementedError;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import win.oakcsclub.api.Command;
import win.oakcsclub.api.Context;

import java.awt.*;
import java.util.Collections;
import java.util.List;


// TODO: Context already has the commandNameUsed variable, there is no reason to store it in this class
public class NoCommandCommand implements Command {
    private final String commandName;

    public NoCommandCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public List<String> getNames() {
        throw new NotImplementedError();
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public String getShortHelpMenu() {
        return "no";
    }

    @Override
    public String getLongHelpMenu() {
        return "no";
    }

    @Override
    public PermissionLevel getPermissionLevelNeeded() {
        return PermissionLevel.MEMBER;
    }


    // based on http://rosettacode.org/wiki/Levenshtein_distance#Java
    private static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }


    @Override
    public Mono<Void> run(Context context) {
        String closest = null;
        int distance = 12; // this is our upper-bound. this prevents asntohusantoheunthaoeu from matching with ping.
        // or at least, it should. it hasn't been tested and may need to be adjusted
        for(Command command : Main.commands){
            for(String name : command.getNames()){
                int dis = distance(name,commandName);
                if(dis < distance){
                    closest = name;
                }
            }
        }
        String recommended = closest;// can't have a non-final variable in a lambda
        return context.createEmbed(embed ->{
            embed.setColor(Color.RED);
            embed.setTitle("Can't find command " + context.prefixUsed + commandName);
            if(recommended != null)
                embed.setDescription("Did you mean " + recommended + "?");
        }).then();
    }
}
