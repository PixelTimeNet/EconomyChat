/*
 * Copyright (C) 2017 The MoonLake Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.minecraft.moonlake.economychat;

import com.minecraft.moonlake.MoonLakeAPI;
import com.minecraft.moonlake.MoonLakePlugin;
import com.minecraft.moonlake.api.annotation.plugin.command.Command;
import com.minecraft.moonlake.api.annotation.plugin.command.CommandArgumentOptional;
import com.minecraft.moonlake.api.annotation.plugin.command.MoonLakeCommand;
import com.minecraft.moonlake.api.annotation.plugin.command.exception.CommandPermissionException;
import com.minecraft.moonlake.api.event.MoonLakeListener;
import com.minecraft.moonlake.api.player.MoonLakePlayer;
import com.minecraft.moonlake.manager.PlayerManager;
import com.minecraft.moonlake.util.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class EconomyChatPlugin extends JavaPlugin implements MoonLakeListener, MoonLakeCommand {

    private String prefix;
    private int limitEconomy;
    private boolean consume;

    public EconomyChatPlugin() {
    }

    @Override
    public void onEnable() {
        if(!setupMoonLake()) {
            this.getLogger().log(Level.SEVERE, "前置月色之湖核心API插件加载失败.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.initFolder();
        this.initUserOptions();
        this.getLogger().info("经济聊天 EconomyChat 插件 v" + getDescription().getVersion() + " 成功加载.");
    }

    @Override
    public void onDisable() {
    }

    private void initFolder() {
        if(!getDataFolder().exists())
            getDataFolder().mkdirs();
        File config = new File(getDataFolder(), "config.yml");
        if(!config.exists())
            saveDefaultConfig();
        reload();
    }

    private void reload() {
        this.prefix = StringUtil.toColor(getConfig().getString("Prefix", "&f[&2经济聊天&f] "));
        this.limitEconomy = getConfig().getInt("LimitEconomy", 0);
        this.consume = getConfig().getBoolean("Consume", false);
    }

    private void initUserOptions() {
        if(limitEconomy > 0)
            // 大于 0 则加载事件
            MoonLakeAPI.registerEvent(this, this);
        // 注册命令
        MoonLakeAPI.getCommandAnnotation().registerCommand(this, this);
    }

    public String getMessage(String key, Object... args) {
        return StringUtil.toColor(prefix + String.format(getConfig().getString("Messages." + key, ""), args));
    }

    private boolean setupMoonLake() {
        Plugin plugin = this.getServer().getPluginManager().getPlugin("MoonLake");
        return plugin != null && plugin instanceof MoonLakePlugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        // 处理玩家聊天
        MoonLakePlayer player = PlayerManager.adapter(event.getPlayer());
        double economy = player.getEconomyVaultBalance();

        if(economy < limitEconomy) {
            // 玩家的当前经济小于限制的经济则阻止
            event.setCancelled(true);
            player.send(getMessage("EconomyChatCancelled", limitEconomy));
            return;
        }
        // 否则足够则判断是否需要消费
        if(consume) {
            // 需要消费则减少经济
            player.withdrawEconomyVaultBalance(limitEconomy);
            player.send(getMessage("EconomyChatConsume", limitEconomy));
        }
    }

    @Command(name = "economychat", usage = "help", max = 1)
    public void onCommand(CommandSender sender, @CommandArgumentOptional String arg) {
        // 处理命令
        if(arg == null)
            arg = "help";

        if(arg.equalsIgnoreCase("help")) {
            // 帮助
            sender.sendMessage(StringUtil.toColor(new String[] {
                    "&b&l&m          &d EconomyChat &7By &6Month_Light &b&l&m          ",
                    "&6/economychat help &7- 查看插件命令帮助.",
                    "&6/economychat reload &7- 重新载入插件配置文件.",
            }));
        } else if(arg.equalsIgnoreCase("reload")) {
            // 重新载入配置文件数据
            if(!sender.hasPermission("moonlake.economychat"))
                throw new CommandPermissionException("moonlake.economychat");
            // 拥有权限则重载
            this.reloadConfig();
            this.reload();
            this.initUserOptions();
            sender.sendMessage(getMessage("EconomyChatReload"));
        } else {
            sender.sendMessage(getMessage("ErrorCommandArgs", "/economychat help - 查看命令帮助."));
        }
    }
}
