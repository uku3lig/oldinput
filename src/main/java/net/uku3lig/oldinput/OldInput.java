package net.uku3lig.oldinput;

import com.google.common.util.concurrent.AtomicDouble;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Mod(modid = OldInput.MOD_ID, name = OldInput.MOD_NAME, version = OldInput.VERSION)
public class OldInput extends MouseHelper {
    public static final String MOD_ID = "oldinput";
    public static final String MOD_NAME = "OldInput";
    public static final String VERSION = "1.1.2";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

    private final AtomicDouble dx = new AtomicDouble();
    private final AtomicDouble dy = new AtomicDouble();

    private static final Set<Mouse> mice = new HashSet<>();

    @Override
    public void mouseXYChange() {
        this.deltaX = (int) dx.getAndSet(0);
        this.deltaY = (int) -dy.getAndSet(0);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft.getMinecraft().mouseHelper = this;

        mice.addAll(this.getMice(ControllerEnvironment.getDefaultEnvironment()));

        executor.scheduleAtFixedRate(() -> {
            if (Minecraft.getMinecraft().currentScreen == null) {
                mice.forEach(mouse -> {
                    mouse.poll();
                    dx.addAndGet(mouse.getX().getPollData());
                    dy.addAndGet(mouse.getY().getPollData());
                });
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        executor.scheduleAtFixedRate(() -> {
            if (Minecraft.getMinecraft().currentScreen == null) return;
            this.getNewEnv().ifPresent(env -> {
                Set<Mouse> newMice = this.getMice(env);
                mice.clear();
                mice.addAll(newMice);
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private Set<Mouse> getMice(ControllerEnvironment env) {
        return Arrays.stream(env.getControllers())
                .filter(Mouse.class::isInstance)
                .map(Mouse.class::cast)
                .collect(Collectors.toSet());
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
