package net.uku3lig.oldinput;

import com.google.common.util.concurrent.AtomicDouble;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = OldInput.MOD_ID, name = OldInput.MOD_NAME, version = OldInput.VERSION)
public class OldInput extends MouseHelper {
    public static final String MOD_ID = "oldinput";
    public static final String MOD_NAME = "OldInput";
    public static final String VERSION = "1.0.0";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

    private final AtomicDouble dx = new AtomicDouble();
    private final AtomicDouble dy = new AtomicDouble();

    private ControllerEnvironment env;

    @Override
    public void mouseXYChange() {
        this.deltaX = (int) dx.getAndSet(0);
        this.deltaY = (int) -dy.getAndSet(0);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new RescanCommand());

        env = ControllerEnvironment.getDefaultEnvironment();
        Minecraft.getMinecraft().mouseHelper = this;

        executor.scheduleAtFixedRate(() -> {
            if (Minecraft.getMinecraft().currentScreen != null) return;
            Arrays.stream(env.getControllers())
                    .filter(Mouse.class::isInstance)
                    .map(Mouse.class::cast)
                    .forEach(mouse -> {
                        mouse.poll();
                        dx.addAndGet(mouse.getX().getPollData());
                        dy.addAndGet(mouse.getY().getPollData());
                    });
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    public class RescanCommand extends CommandBase {
        @Override @Nonnull
        public String getName() {
            return "rescan";
        }

        @Override @Nonnull
        public String getUsage(@Nonnull ICommandSender sender) {
            return "/rescan";
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, ICommandSender sender, @Nonnull String[] args) {
            sender.sendMessage(getText("rescanning devices (will cause a small lagspike)"));
            Optional<ControllerEnvironment> newEnv = getNewEnv();
            if (newEnv.isPresent()) {
                env = newEnv.get();
            } else {
                sender.sendMessage(getText("could not rescan devices, please retry"));
            }
        }

        private ITextComponent getText(String msg) {
            return new TextComponentString("[OldInput] " + msg);
        }

        @SuppressWarnings("unchecked")
        private Optional<ControllerEnvironment> getNewEnv() {
            try {
                // Find constructor (class is package private, so we can't access it directly)
                Constructor<ControllerEnvironment> constructor = (Constructor<ControllerEnvironment>)
                        Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                return Optional.of(constructor.newInstance());
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }
}
