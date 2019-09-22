package win.oakcsclub.commands;

import reactor.core.publisher.Mono;
import win.oakcsclub.PermissionLevel;
import win.oakcsclub.api.Command;
import win.oakcsclub.api.Context;

import java.util.List;

import static win.oakcsclub.Util.listOf;

public class PermsCommand implements Command  {
    @Override
    public List<String> getNames() {
        return listOf("permission","permissions","perms","perm");
    }

    @Override
    public String getShortHelpMenu() {
        return "query your permission level";
    }

    @Override
    public String getLongHelpMenu() {
        return "Commands can be limited to people with certain permissions." +
                "Use this command to query what level you are.";
    }

    @Override
    public PermissionLevel getPermissionLevelNeeded() {
        return PermissionLevel.MEMBER;
    }

    @Override
    public Mono<Void> run(Context context) {
        return context.createMessage("Your permission level: " + context.highestPermission.toString().toLowerCase())
                .then();
    }
}
