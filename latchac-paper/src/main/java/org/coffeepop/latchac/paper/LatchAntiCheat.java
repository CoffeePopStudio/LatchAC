package org.coffeepop.latchac.paper;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchEngine;
import org.coffeepop.latchac.core.config.ConfigParser;
import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.paper.command.LatchacCommand;
import org.coffeepop.latchac.paper.fake.FakePlayerRegistry;
import org.coffeepop.latchac.paper.fake.HeuristicFakePlayerProvider;
import org.coffeepop.latchac.paper.sensor.MovementSensor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LatchAntiCheat extends JavaPlugin {

    private LatchEngine engine;
    private FakePlayerRegistry fakePlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // 首次运行落盘 config.yml

        this.engine = new LatchEngine(loadConfigOrBuiltin());
        this.fakePlayers = new FakePlayerRegistry();
        this.fakePlayers.register(new HeuristicFakePlayerProvider());

        getServer().getPluginManager().registerEvents(
                new MovementSensor(this, engine, fakePlayers), this);

        PluginCommand command = getCommand("latchac");
        if (command != null) {
            command.setExecutor(new LatchacCommand(this));
        }

        getLogger().info("LatchAC M2 原型已启用——记录模式，绝不回弹（生电即公理）");
    }

    /** 磁盘配置解析失败时回退 jar 内置默认，保证插件永远有可用配置。 */
    private LatchConfig loadConfigOrBuiltin() {
        Path file = getDataFolder().toPath().resolve("config.yml");
        try {
            return new ConfigParser().parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            getLogger().severe("config.yml 解析失败，回退内置默认配置: " + ex.getMessage());
            try (InputStream in = getResource("config.yml")) {
                if (in == null) {
                    throw new IllegalStateException("jar 内缺少默认 config.yml");
                }
                return new ConfigParser().parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("内置默认配置不可读", e);
            }
        }
    }

    /**
     * 热重载：解析成功才替换纪元；失败保留旧配置继续运行。
     * @return 是否重载成功
     */
    public boolean reloadLatchConfig() {
        Path file = getDataFolder().toPath().resolve("config.yml");
        try {
            engine.reload(new ConfigParser().parse(Files.readString(file, StandardCharsets.UTF_8)));
            getLogger().info("配置已热重载");
            return true;
        } catch (IOException | RuntimeException ex) {
            getLogger().severe("热重载失败，继续使用旧配置: " + ex.getMessage());
            return false;
        }
    }

    /** 供第三方扩展注册假人识别器（公开 API）。 */
    public FakePlayerRegistry fakePlayers() {
        return fakePlayers;
    }
}
