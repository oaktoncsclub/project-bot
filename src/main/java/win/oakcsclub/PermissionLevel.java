package win.oakcsclub;


/**
 * This class is made to have a highierarical structure to commands
 *
 * [bot admin] -> is a -> [server admin] -> is a -> [member]
 *
 * additional permission types that do not fit into this format should use something like a
 * {link Properties} class
 */
public enum PermissionLevel {

    MEMBER("MEMBER", "Every person will have access to commands that need this role",null),
    SERVER_ADMIN("SERVER_ADMIN","People will admin permissions on servers can run these commands",MEMBER),
    BOT_ADMIN("BOT_ADMIN","People who have full control over every part of the bot",SERVER_ADMIN);

    // final variables do not need to be private
    final String name;
    final String desc;
    final PermissionLevel higherThen;

    PermissionLevel(String name, String desc, PermissionLevel higherThen) {
        this.name = name;
        this.desc = desc;
        this.higherThen = higherThen;
    }
}
