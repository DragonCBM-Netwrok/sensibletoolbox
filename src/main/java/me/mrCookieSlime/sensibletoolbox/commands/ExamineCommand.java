package me.mrCookieSlime.sensibletoolbox.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.commands.AbstractCommand;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;

public class ExamineCommand extends AbstractCommand {
	
    public ExamineCommand() {
        super("stb examine", 0, 0);
        setUsage("/stb examine");
        setPermissionNode("stb.commands.examine");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        notFromConsole(sender);
        Player player = (Player) sender;
        MessagePager pager = MessagePager.getPager(sender).clear();
        pager.add(STBUtil.dumpItemStack(player.getItemInHand()));
        pager.showPage();
        return true;
    }
}
